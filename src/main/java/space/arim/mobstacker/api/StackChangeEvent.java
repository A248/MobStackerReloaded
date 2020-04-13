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

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

public class StackChangeEvent extends StackEvent implements Cancellable {

	private static final HandlerList HANDLERS = new HandlerList();
	
	private final LivingEntity stacked;
	private final StackInfo stackedInfo;
	private final StackCause cause;
	private volatile boolean cancelled = false;
	
	public StackChangeEvent(LivingEntity stack, LivingEntity stacked, StackInfo stackInfo, StackInfo stackedInfo, StackCause cause) {
		super(stack, stackInfo);
		this.stacked = stacked;
		this.stackedInfo = stackedInfo;
		this.cause = cause;
	}
	
	/**
	 * The entity which will be added to the stack.
	 * 
	 * @return the stacked entity
	 */
	public LivingEntity getStackedEntity() {
		return stacked;
	}
	
	/**
	 * The info of the entity which will be added to the stack,
	 * before the event occurs
	 * 
	 * @return the stacked entity info
	 */
	public StackInfo getStackedEntityInfo() {
		return stackedInfo;
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
	
	@Override
	public boolean isCancelled() {
		return cancelled;
	}
	
	@Override
	public void setCancelled(boolean cancel) {
		cancelled = cancel;
	}
	
}
