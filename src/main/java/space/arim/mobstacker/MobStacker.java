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
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;

import space.arim.api.util.FilesUtil;
import space.arim.api.uuid.UUIDUtil;

import space.arim.mobstacker.api.MobStackerAPI;
import space.arim.mobstacker.api.StackCause;
import space.arim.mobstacker.api.StackChangeEvent;

public class MobStacker implements MobStackerAPI {

	final JavaPlugin plugin;
	private final File dataFile;
	
	final StackConfig config;
	private final StackListener listener;
	
	/*
	 * 1 not stacked but stackable
	 * 1+ stacked
	 */
	final HashMap<UUID, Integer> amounts = new HashMap<UUID, Integer>();
	
	private StackPeriodic periodic;
	
	MobStacker(JavaPlugin plugin) {
		this.plugin = plugin;
		dataFile = new File(plugin.getDataFolder(), "stacks.txt");
		config = new StackConfig(plugin.getDataFolder());
		listener = new StackListener(this);
	}
	
	void load() {
		config.reload();
		if (config.getBoolean("enable-plugin")) {
			plugin.getServer().getPluginManager().registerEvents(listener, plugin);
			plugin.getLogger().info("Registered events!");
			if (loadDataFile()) {
				plugin.getLogger().info("Loaded existing stacks!");
			}
			if (config.getBoolean("triggers.periodic.enable")) {
				periodic = new StackPeriodic(this);
				periodic.start();
			}
		} else {
			plugin.getLogger().info("Turn on enable-plugin in the config.yml to enable MobStackerReloaded.");
		}
	}
	
	@Override
	public int getAmount(LivingEntity entity) {
		return amounts.getOrDefault(entity, 0);
	}
	
	@Override
	public boolean isStackable(LivingEntity entity) {
		return getAmount(entity) > 0;
	}
	
	private boolean fireStackEvent(LivingEntity stack, LivingEntity stacked, StackCause cause) {
		StackChangeEvent evt = new StackChangeEvent(stack, stacked, cause);
		plugin.getServer().getPluginManager().callEvent(evt);
		return !evt.isCancelled();
	}
	
	@Override
	public void attemptMerges(LivingEntity entity, StackCause cause) {
		if (config.isCorrectLocation(entity.getLocation()) && isStackable(entity)) {
			for (Entity nearby : entity.getNearbyEntities(config.radiusX(), config.radiusY(), config.radiusZ())) {
				if (nearby instanceof LivingEntity) {
					attemptMerge(entity, (LivingEntity) nearby, cause);
				}
			}
		}
	}
	
	private void attemptMerge(LivingEntity entity, LivingEntity nearby, StackCause cause) {
		if (isStackable(nearby) && config.mergeable(entity, nearby)) {
			int finalAmount = getAmount(entity) + getAmount(nearby);
			if (finalAmount <= config.getInt("stacking.max-stack-size") && fireStackEvent(entity, nearby, cause)) {
				updateAmount(entity, finalAmount);
				amounts.remove(nearby.getUniqueId());
				nearby.remove();
			}
		}
	}
	
	void updateAmount(LivingEntity entity, int amount) {
		if (config.getBoolean("stacking.names.enable")) {
			entity.setCustomNameVisible(true);
			entity.setCustomName(config.getString("stacking.names.name").replace("%COUNT%", Integer.toString(amount + 1)));
		}
		amounts.put(entity.getUniqueId(), amount);
	}
	
	@Override
	public void setAmount(LivingEntity entity, int amount) {
		if (isStackable(entity)) {
			updateAmount(entity, amount);
		}
	}
	
	@Override
	public boolean isStacked(LivingEntity entity) {
		return getAmount(entity) > 1;
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
	
	private boolean loadDataFile() {
		return FilesUtil.readLines(dataFile, (line) -> {
			if (line.contains(":")) {
				String[] data = line.split(":");
				amounts.put(UUIDUtil.expandAndParse(data[0]), Integer.parseInt(data[1]));
			}
		}, (ex) -> plugin.getLogger().log(Level.WARNING, "Failed to read file " + dataFile.getPath() + "!", ex));
	}
	
	@Override
	public void close() {
		FilesUtil.writeTo(dataFile, (writer) -> {
			for (HashMap.Entry<UUID, Integer> entry : amounts.entrySet()) {
				writer.append(entry.getKey().toString().replace("-", "") + ":" + entry.getValue() + "\n");
			}
		}, (ex) -> plugin.getLogger().log(Level.WARNING, "Could not print data to file " + dataFile.getPath() + "!", ex));
		if (periodic != null) {
			periodic.stop();
			periodic = null;
		}
	}
	
}
