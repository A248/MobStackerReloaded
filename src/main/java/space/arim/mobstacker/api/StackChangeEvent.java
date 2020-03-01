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
import org.bukkit.event.HandlerList;

import space.arim.api.platform.spigot.CancellableEvent;

public class StackChangeEvent extends CancellableEvent implements StackEvent {

	private static final HandlerList HANDLERS = new HandlerList();
	
	private final Entity stack;
	private final Entity stacked;
	private final StackCause cause;
	
	public StackChangeEvent(Entity stack, Entity stacked, StackCause cause) {
		this.stack = stack;
		this.stacked = stacked;
		this.cause = cause;
	}
	
	@Override
	public Entity getStackEntity() {
		return stack;
	}
	
	/**
	 * The entity which will be added to the stack.
	 * 
	 * @return the stacked entity
	 */
	public Entity getStackedEntity() {
		return stacked;
	}
	
	/**
	 * The reason this event is taking place.
	 * 
	 * @return the {@link StackCause}
	 */
	public StackCause getCause() {
		return cause;
	}
	
	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}
	
	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

}
