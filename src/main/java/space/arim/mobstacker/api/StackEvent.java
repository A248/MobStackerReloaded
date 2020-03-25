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
package space.arim.mobstacker.api;

import org.bukkit.entity.Entity;
import org.bukkit.event.Event;

/**
 * A parent class for all stacking related events.
 * 
 * @author A248
 *
 */
public abstract class StackEvent extends Event {
	
	private final Entity entity;
	
	StackEvent(Entity entity) {
		this.entity = entity;
	}
	
	/**
	 * The entity which represents the entire stack.
	 * 
	 * @return the stack entity
	 */
	public Entity getStackEntity() {
		return entity;
	}
	
}
