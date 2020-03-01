/* 
 * MobStackerReloaded
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * MobStackerReloaded is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MobStackerReloaded is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MobStackerReloaded. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.mobstacker;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.plugin.java.JavaPlugin;

import space.arim.universal.util.function.ErringConsumer;

import space.arim.api.util.FilesUtil;
import space.arim.api.util.LazySingleton;
import space.arim.api.uuid.UUIDUtil;

import space.arim.mobstacker.api.MobStackerAPI;
import space.arim.mobstacker.api.StackCause;
import space.arim.mobstacker.api.StackChangeEvent;

public class MobStacker implements MobStackerAPI {

	final JavaPlugin plugin;
	private final File dataFile;
	
	final StackConfig config;
	private final StackListener listener;
	
	private final HashMap<UUID, Integer> amounts;
	
	private StackPeriodic periodic;
	private final LazySingleton<File> validFile;
	final LazySingleton<Set<UUID>> validMobs;
	
	MobStacker(JavaPlugin plugin) {
		this.plugin = plugin;
		dataFile = new File(plugin.getDataFolder(), "data/stacks.txt");
		config = new StackConfig(plugin.getDataFolder());
		listener = new StackListener(this);
		amounts = new HashMap<UUID, Integer>();
		validFile = new LazySingleton<File>(() -> new File(plugin.getDataFolder(), "data/valid.txt"));
		validMobs = new LazySingleton<Set<UUID>>(() -> new HashSet<UUID>());
	}
	
	void load() {
		config.reload();
		if (config.getBoolean("enable-plugin")) {
			plugin.getServer().getPluginManager().registerEvents(listener, plugin);
			plugin.getLogger().info("Registered events!");
			if (loadDataFile()) {
				plugin.getLogger().info("Loaded existing stacks!");
			}
			if (config.mobSpawnersOnly() && loadValidFile()) {
				plugin.getLogger().info("Loaded valid entities!");
			}
			if (config.getBoolean("triggers.periodic.enable")) {
				periodic = new StackPeriodic(this);
				periodic.start();
			}
		}
	}
	
	private boolean processFile(File file, Consumer<String> processor) {
		return FilesUtil.readLines(file, processor, (ex) -> plugin.getLogger().log(Level.WARNING, "Failed to read file " + file.getPath() + "!", ex));
	}
	
	private boolean loadDataFile() {
		return processFile(dataFile, (line) -> {
			if (line.contains(":")) {
				String[] data = line.split(":");
				amounts.put(UUIDUtil.expandAndParse(data[0]), Integer.parseInt(data[1]));
			}
		});
	}
	
	private boolean loadValidFile() {
		return processFile(validFile.get(), (line) -> {
			if (!line.isEmpty()) {
				validMobs.get().add(UUIDUtil.expandAndParse(line));
			}
		});
	}
	
	@Override
	public void attemptMerges(Entity entity, StackCause cause) {
		if (isCorrectLocation(entity.getLocation()) && isStackable(entity)) {
			for (Entity nearby : entity.getNearbyEntities(config.radiusX(), config.radiusY(), config.radiusZ())) {
				if (isStackable(nearby)) {
					attemptMerge((LivingEntity) entity, (LivingEntity) nearby, cause);
				}
			}
		}
	}
	
	void copyAttributes(Entity entity, Entity progeny) {
		progeny.setTicksLived(entity.getTicksLived());
	}
	
	private boolean mergeable(LivingEntity entity, LivingEntity other) {
		if (entity.getType() != other.getType()) {
			return false;
		}
		if (config.getBoolean("stacking.separation.age.enable") && entity instanceof Ageable) {
			Ageable age1 = (Ageable) entity;
			Ageable age2 = (Ageable) other;
			if (config.getBoolean("stacking.separation.age.strict")) {
				if (age1.getAge() != age2.getAge()) {
					return false;
				}
			} else if (age1.isAdult() && !age2.isAdult() || !age1.isAdult() && age2.isAdult()) {
				return false;
			}
		}
		return true;
	}
	
	private void attemptMerge(LivingEntity entity, LivingEntity nearby, StackCause cause) {
		if (mergeable(entity, nearby)) {
			int finalAmount = getAmount(entity) + getAmount(nearby);
			if (finalAmount <= config.getInt("stacking.max-stack-size") && fireStackEvent(entity, nearby, cause)) {
				updateAmount(entity, finalAmount);
				nearby.remove();
			}
		}
	}
	
	private boolean fireStackEvent(Entity stack, Entity stacked, StackCause cause) {
		StackChangeEvent evt = new StackChangeEvent(stack, stacked, cause);
		plugin.getServer().getPluginManager().callEvent(evt);
		return !evt.isCancelled();
	}
	
	boolean isCorrectLocation(Location location) {
		return isCorrectWorld(location.getWorld());
	}
	
	private boolean isCorrectWorld(World world) {
		List<String> worlds = config.getStrings("triggers.per-world.worlds");
		return config.getBoolean("triggers.per-world.use-as-whitelist") ? worlds.contains(world.getName()) : !worlds.contains(world.getName());
	}
	
	private boolean isEntityTypeAllowed(EntityType type) {
		List<String> typeList = config.getStrings("stacking.exempt.types");
		return config.getBoolean("stacking.filter.use-as-whitelist") ? typeList.contains(type.name()) : !typeList.contains(type.name());
	}
	
	private boolean isEntityValid(Entity entity) {
		return !config.mobSpawnersOnly() || validMobs.get().contains(entity.getUniqueId());
	}
	
	void updateAmount(Entity entity, int amount) {
		if (config.getBoolean("stacking.names.enable")) {
			entity.setCustomNameVisible(true);
			entity.setCustomName(config.getString("stacking.names.name").replace("%COUNT%", Integer.toString(amount + 1)));
		}
		amounts.put(entity.getUniqueId(), amount);
	}
	
	void untrack(Entity entity) {
		amounts.remove(entity.getUniqueId());
	}
	
	@Override
	public int getAmount(Entity entity) {
		return amounts.containsKey(entity.getUniqueId()) ? amounts.get(entity.getUniqueId()) : isEverStackable(entity) ? 1 : 0;
	}
	
	@Override
	public void setAmount(Entity entity, int amount) {
		if (isStackable(entity)) {
			updateAmount(entity, amount);
		}
	}
	
	@Override
	public boolean isStacked(Entity entity) {
		return getAmount(entity) > 1;
	}
	
	@Override
	public boolean isStackable(Entity entity) {
		return getAmount(entity) > 0;
	}
	
	private boolean isEverStackable(Entity entity) {
		return entity instanceof LivingEntity && !(entity instanceof Player) && !(entity instanceof Slime) && isEntityValid(entity) && isEntityTypeAllowed(entity.getType());
	}
	
	@Override
	public void reload() {
		config.reload();
		if (config.getBoolean("triggers.periodic.enable")) {
			periodic = new StackPeriodic(this);
			periodic.start();
		} else if (periodic != null) {
			periodic.stop();
			periodic = null;
		}
	}
	
	private void printToFile(File file, ErringConsumer<Writer, IOException> printer) {
		FilesUtil.writeTo(file, printer, (ex) -> plugin.getLogger().log(Level.WARNING, "Could not print data to file " + file.getPath() + "!", ex));
	}
	
	@Override
	public void close() {
		printToFile(dataFile, (writer) -> {
			for (HashMap.Entry<UUID, Integer> entry : amounts.entrySet()) {
				writer.append(entry.getKey().toString().replace("-", "") + ":" + entry.getValue() + "\n");
			}
		});
		if (config.mobSpawnersOnly()) {
			printToFile(validFile.get(), (writer) -> {
				for (UUID uuid : validMobs.get()) {
					writer.append(uuid.toString().replace("-", "") + "\n");
				}
			});
			validMobs.get().clear();
		}
		if (periodic != null) {
			periodic.stop();
			periodic = null;
		}
	}
	
}
