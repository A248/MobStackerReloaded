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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import org.slf4j.Logger;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;

import space.arim.api.util.log.LoggerConverter;
import space.arim.api.uuid.UUIDUtil;

import space.arim.mobstacker.api.MobStackerAPI;
import space.arim.mobstacker.api.StackCause;
import space.arim.mobstacker.api.StackChangeEvent;
import space.arim.mobstacker.api.StackInfo;

public class MobStacker implements MobStackerAPI {

	final JavaPlugin plugin;
	private final Logger logger;

	final StackConfig config;
	private final StackListener listener;

	/**
	 * <i>Concurrent</i> so that we can load files asynchronously
	 * We don't need to worry about thread safety once the server has finished starting
	 * 
	 */
	final ConcurrentHashMap<UUID, StackInfoImpl> stacks = new ConcurrentHashMap<>();

	private static final StackInfoImpl BLANK_STACK = new StackInfoImpl(0, 0D);

	private static final BiFunction<StackInfoImpl, StackInfoImpl, StackInfoImpl> MERGE_FUNCTION = StackInfoImpl::combine;

	private StackPeriodic periodic;

	/**
	 * Used to help us load files asynchronously at startup,
	 * <code>null</code> if we don't pass the parallelism threshold
	 * 
	 * To ensure that we've finished loading once server startup has completed,
	 * we call <code>CompletableFuture.join</code> in {@link #finishLoad()}
	 */
	private volatile HashSet<CompletableFuture<?>> futures;
	
	private static final int ASYNC_IO_PARALLELISM_THRESHOLD = 10;

	/**
	 * If a stack's health is less than this number, it is considered dead
	 * 
	 */
	static final double HEALTH_DEATH_THRESHOLD = 0.5D;
	
	private boolean registered = false;
	
	MobStacker(JavaPlugin plugin) {
		this.plugin = plugin;
		logger = LoggerConverter.get().convert(plugin.getLogger());
		config = new StackConfig(plugin.getDataFolder());
		listener = new StackListener(this);
	}
	
	private Runnable getFileLoadAction(File dataFile) {
		return () -> {
			UUID uuid = UUIDUtil.expandAndParse(dataFile.getName());
			try (Scanner scanner = new Scanner(dataFile, "UTF-8")) {
				if (scanner.hasNext()) {

					String[] data = scanner.next().split("\\|");
					StackInfoImpl stack = new StackInfoImpl(Integer.parseInt(data[0]), Double.parseDouble(data[1]));
					stacks.merge(uuid, stack, MERGE_FUNCTION);
				}
			} catch (IOException | NoSuchElementException ex) {
				logger.warn("Error reading file " + dataFile.getPath(), ex);
			}
		};
	}
	
	private void registerEventsIfNotAlready() {
		if (!registered) {
			registered = true;
			plugin.getServer().getPluginManager().registerEvents(listener, plugin);
			logger.info("Registered events!");
		}
	}
	
	void load() {
		config.reload();
		if (config.getBoolean("enable-plugin")) {
			File[] mobFiles = (new File(plugin.getDataFolder(), "mob-data")).listFiles();
			// if the mob-data directory doesn't exist or isn't a dir, mobFiles must be null
			if (mobFiles != null) {

				if (mobFiles.length >= ASYNC_IO_PARALLELISM_THRESHOLD) {
					futures = new HashSet<>();
				}
				for (File dataFile : mobFiles) {

					Runnable cmd = getFileLoadAction(dataFile);
					if (futures != null) {
						futures.add(CompletableFuture.runAsync(cmd));

					} else {
						cmd.run();
					}
				}
			}

			registerEventsIfNotAlready();

			if (config.getBoolean("triggers.periodic.enable")) {
				periodic = new StackPeriodic(this);
				periodic.start();
			}

			if (futures != null) {
				futures.forEach((f) -> f.join()); // await termination
				futures = null;
			}
			logger.info("Loaded mob data files!");
		} else {
			logger.info("Turn on enable-plugin in the config.yml to enable MobStackerReloaded.");
		}
	}
	
	@Override
	public StackInfoImpl getStackInfo(LivingEntity entity) {
		return stacks.getOrDefault(entity.getUniqueId(), BLANK_STACK);
	}
	
	int getSize(LivingEntity entity) {
		return getStackInfo(entity).getSize();
	}

	@Override
	public void setSize(LivingEntity entity, int amount) {
		stacks.computeIfPresent(entity.getUniqueId(), (uuid, stack) -> new StackInfoImpl(amount, stack.getHealth()));
	}

	@Override
	public void setHealth(LivingEntity entity, double health) {
		stacks.computeIfPresent(entity.getUniqueId(), (uuid, stack) -> new StackInfoImpl(stack.getSize(), health));
		if (health < entity.getHealth()) {
			entity.setHealth((health > HEALTH_DEATH_THRESHOLD) ? health : 0);
		}
	}

	@Override
	public boolean isStackable(LivingEntity entity) {
		return getSize(entity) > 0;
	}
	
	@Override
	public boolean isStacked(LivingEntity entity) {
		return getSize(entity) > 1;
	}
	
	private boolean fireStackEvent(LivingEntity stackEntity, StackInfo stackInfo, LivingEntity stackedEntity, StackInfo stackedInfo, StackCause cause) {
		StackChangeEvent evt = new StackChangeEvent(stackEntity, stackInfo, stackedEntity, stackedInfo, cause);
		plugin.getServer().getPluginManager().callEvent(evt);
		return !evt.isCancelled();
	}
	
	void directAttemptMerges(LivingEntity entity, StackCause cause) {
		if (isStackable(entity)) {
			for (Entity nearby : entity.getNearbyEntities(config.radiusX(), config.radiusY(), config.radiusZ())) {
				if (nearby instanceof LivingEntity) {
					attemptMerge(entity, (LivingEntity) nearby, cause);
				}
			}
		}
	}
	
	@Override
	public void attemptMerges(LivingEntity entity, StackCause cause) {
		if (config.isCorrectWorld(entity.getWorld())) {
			directAttemptMerges(entity, cause);
		}
	}
	
	private void attemptMerge(LivingEntity entity, LivingEntity nearby, StackCause cause) {
		if (isStackable(nearby) && config.mergeable(entity, nearby)) {
			StackInfoImpl stackEntity = getStackInfo(entity);
			StackInfoImpl stackNearby = getStackInfo(nearby);

			int finalAmount = stackEntity.getSize() + stackNearby.getSize();
			if (finalAmount <= config.getInt("stacking.max-stack-size")) {
				// find which entity is bigger and prioritise it
				LivingEntity stack;
				StackInfoImpl stackInfo;
				LivingEntity stacked;
				StackInfoImpl stackedInfo;

				if (stackEntity.getSize() > stackNearby.getSize()) {
					stack = entity;
					stackInfo = stackEntity;

					stacked = nearby;
					stackedInfo = stackNearby;
				} else {
					stack = nearby;
					stackInfo = stackNearby;

					stacked = entity;
					stackedInfo = stackEntity;
				}
				if (fireStackEvent(stack, stackInfo, stacked, stackedInfo, cause)) {

					StackInfoImpl result = stackInfo.combine(stackedInfo);
					stacks.put(stack.getUniqueId(), result);

					stacks.remove(stacked.getUniqueId());
					stacked.remove();

					updateName(stack, result);
				}
			}
		}
	}
	
	void updateName(LivingEntity entity, StackInfo info) {
		if (config.useDisplayName() && (info.getSize() != 1 || !config.getBoolean("stacking.names.disable-for-unstacked"))) {
			entity.setCustomNameVisible(true);
			entity.setCustomName(config.getDisplayName().replace("%SIZE%", Integer.toString(info.getSize()))
					.replace("%HEALTH%", Double.toString(info.getHealth()))
					.replace("%TYPE%", config.toStringEntity(entity.getType())));
		}
	}
	
	@Override
	public void reload() {
		config.reload();
		registerEventsIfNotAlready();

		if (periodic != null) {
			periodic.stop();
			periodic = null;
		}
		if (config.getBoolean("triggers.periodic.enable")) {
			periodic = new StackPeriodic(this);
			periodic.start();
		}
	}
	
	@Override
	public void close() {
		if (periodic != null) {
			periodic.stop();
			periodic = null;
		}

		File mobDataFolder = new File(plugin.getDataFolder(), "mob-data");
		if (mobDataFolder.isDirectory() || mobDataFolder.mkdirs()) {

			stacks.forEach(ASYNC_IO_PARALLELISM_THRESHOLD, (uuid, stack) -> {
				File dataFile = new File(mobDataFolder, uuid.toString().replace("-", ""));
				if (dataFile.exists() && !dataFile.delete()) {
					logger.warn("Could not override data file " + dataFile.getPath());

				} else {
					try (OutputStream output = new FileOutputStream(dataFile); OutputStreamWriter writer = new OutputStreamWriter(output, "UTF-8")) {
						writer.append(Integer.toString(stack.getSize())).append('|').append(Double.toString(stack.getHealth()));
					} catch (IOException ex) {
						logger.warn("Could not print data to file " + dataFile.getPath() + "!", ex);
					}
				}
			});
		} else {
			logger.warn("Could not create mob data directory!");
		}
	}
	
}
