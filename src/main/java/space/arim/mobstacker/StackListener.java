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

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;

import space.arim.mobstacker.api.StackCause;
import space.arim.mobstacker.api.StackDeathEvent;

public class StackListener implements Listener {

	private final MobStacker core;
	
	StackListener(MobStacker core) {
		this.core = core;
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onSpawn(CreatureSpawnEvent evt) {
		if (evt.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.SPAWNER)
				&& core.config.isTypeStackable(evt.getEntity())
				&& core.config.isCorrectWorld(evt.getLocation().getWorld())) {

			core.stacks.put(evt.getEntity().getUniqueId(), new StackInfoImpl(1, evt.getEntity().getMaxHealth()));

			if (core.config.getBoolean("triggers.events.spawn")) {
				core.directAttemptMerges(evt.getEntity(), StackCause.SPAWN);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onChunkLoad(ChunkLoadEvent evt) {
		if (core.config.getBoolean("triggers.events.chunk-load") && core.config.isCorrectWorld(evt.getWorld())) {
			Bukkit.getServer().getScheduler().runTaskLater(core.plugin, () -> {
				for (Entity entity : evt.getChunk().getEntities()) {
					if (entity instanceof LivingEntity) {
						core.directAttemptMerges((LivingEntity) entity, StackCause.CHUNK_LOAD);
					}
				}
			}, 1L);
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onDamage(EntityDamageEvent evt) {
		if (evt.getEntity() instanceof LivingEntity) {
			LivingEntity entity = (LivingEntity) evt.getEntity();
			if (core.config.isAoe(evt.getCause())) {

				double damage = evt.getFinalDamage();
				core.stacks.computeIfPresent(entity.getUniqueId(),
						(uuid, stack) -> new StackInfoImpl(stack.getSize(), stack.getHealth() - damage));
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onDeath(EntityDeathEvent evt) {
		LivingEntity entity = evt.getEntity();
		if (core.isStacked(entity) && core.config.isCorrectWorld(evt.getEntity().getWorld())) {
			// find existing amount of entity
			StackInfoImpl stackInfo = core.getStackInfo(entity);
			int stackSize = stackInfo.getSize();
			double stackHealth = stackInfo.getHealth();

			if (stackSize > 1) {
				if (stackHealth < MobStacker.HEALTH_DEATH_THRESHOLD) {
					// entire stack must die, multiply drops and xp
					evt.setDroppedExp(evt.getDroppedExp() * stackSize);
					evt.getDrops().forEach((item) -> item.setAmount(item.getAmount() * stackSize));
				} else {
					// typical scenario, only 1 entity from the stack dies, so we spawn another
					LivingEntity progeny = (LivingEntity) entity.getWorld().spawnEntity(entity.getLocation(), entity.getType());

					// copy attributes
					progeny.setCanPickupItems(entity.getCanPickupItems());
					progeny.setCustomName(entity.getCustomName());
					progeny.setFireTicks(entity.getFireTicks());
					progeny.setLeashHolder(entity.getLeashHolder());
					progeny.setMaximumAir(entity.getMaximumAir());
					progeny.setMaximumNoDamageTicks(entity.getMaximumNoDamageTicks());
					progeny.setNoDamageTicks(entity.getNoDamageTicks());
					progeny.setRemainingAir(entity.getRemainingAir());
					progeny.setRemoveWhenFarAway(entity.getRemoveWhenFarAway());
					progeny.setTicksLived(entity.getTicksLived());
					progeny.setVelocity(entity.getVelocity());

					// update stack size
			        StackInfoImpl result = new StackInfoImpl((stackSize - 1), stackInfo.getHealth());
			        core.stacks.put(progeny.getUniqueId(), result);

			        core.updateName(progeny, result);
				}
			}
			// cleanup
			core.stacks.remove(entity.getUniqueId());
			core.plugin.getServer().getPluginManager().callEvent(new StackDeathEvent(entity, stackInfo));
		}
	}
	
}
