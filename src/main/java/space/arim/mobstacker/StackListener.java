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
		if (evt.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.SPAWNER) && core.config.isTypeStackable(evt.getEntity())) {
			core.amounts.put(evt.getEntity().getUniqueId(), 1);
			if (core.config.getBoolean("triggers.events.spawn")) {
				core.attemptMerges(evt.getEntity(), StackCause.SPAWN);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onChunkLoad(ChunkLoadEvent evt) {
		if (core.config.getBoolean("triggers.events.chunk-load")) {
			Bukkit.getServer().getScheduler().runTaskLater(core.plugin, () -> {
				for (Entity entity : evt.getChunk().getEntities()) {
					if (entity instanceof LivingEntity) {
						core.attemptMerges((LivingEntity) entity, StackCause.CHUNK_LOAD);
					}
				}
			}, 1L);
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onDeath(EntityDeathEvent evt) {
		LivingEntity entity = evt.getEntity();
		if (core.isStacked(entity)) {
			int amount = core.getAmount(entity);
			if (amount > 1) {
				LivingEntity progeny = (LivingEntity) entity.getWorld().spawnEntity(entity.getLocation(), entity.getType());
				progeny.setTicksLived(entity.getTicksLived());
		        core.updateAmount(progeny, --amount);
			}
			core.amounts.remove(entity.getUniqueId());
			core.plugin.getServer().getPluginManager().callEvent(new StackDeathEvent(entity));
		}
	}
	
}
