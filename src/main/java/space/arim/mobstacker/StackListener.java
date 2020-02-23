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
		if (core.config.getBoolean("stacking.mob-spawners-only") && evt.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.SPAWNER)) {
			core.validMobs.get().add(evt.getEntity().getUniqueId());
		}
		if (core.config.getBoolean("triggers.events.spawn")) {
			core.attemptMerges(evt.getEntity(), StackCause.SPAWN);
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onChunkLoad(ChunkLoadEvent evt) {
		if (core.config.getBoolean("triggers.events.chunk-load")) {
			for (Entity entity : evt.getChunk().getEntities()) {
				core.attemptMerges(entity, StackCause.CHUNK_LOAD);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onDeath(EntityDeathEvent evt) {
		LivingEntity entity = evt.getEntity();
		if (core.isStacked(entity)) {
			int amount = core.getAmount(entity);
			if (amount > 1) {
				Entity progeny = entity.getWorld().spawnEntity(entity.getLocation(), entity.getType());
				core.copyAttributes(entity, progeny);
		        core.updateAmount(progeny, --amount);
			}
			core.untrack(entity);
			core.plugin.getServer().getPluginManager().callEvent(new StackDeathEvent(entity));
		}
	}
	
}
