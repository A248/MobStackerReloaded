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
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitTask;

import space.arim.mobstacker.api.StackCause;

public class StackPeriodic {

	private final MobStacker core;
	
	private BukkitTask task;
	
	StackPeriodic(MobStacker core) {
		this.core = core;
	}
	
	void start() {
		task = Bukkit.getServer().getScheduler().runTaskTimer(core.plugin, () -> {
			for (World world : core.plugin.getServer().getWorlds()) {
				if (core.config.isCorrectWorld(world)) {
					for (Entity entity : world.getEntities()) {
						if (entity instanceof LivingEntity) {
							core.directAttemptMerges((LivingEntity) entity, StackCause.PERIODIC);
						}
					}
				}
			}
		}, 0L, 20L * core.config.getInt("triggers.periodic.period-in-seconds").longValue());
	}
	
	void stop() {
		if (task != null) {
			task.cancel();
		}
	}

}
