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

import space.arim.universal.util.AutoClosable;

/**
 * The officially supported API for interacting with MobStackerReloaded.
 * 
 * @author A248
 *
 */
public interface MobStackerAPI extends AutoClosable {
	
	/**
	 * Returns the stack information of an entity.
	 * 
	 * @param entity the entity
	 * @return the stack info
	 */
	StackInfo getStackInfo(LivingEntity entity);
	
	/**
	 * Updates the stack size of an entity. <br>
	 * If the entity cannot be stacked according to {@link #isStackable(LivingEntity)}, nothing happens.
	 * 
	 * @param entity the entity to change
	 * @param size the updated size
	 */
	void setSize(LivingEntity entity, int size);
	
	/**
	 * Updates the stack health of an entity. <br>
	 * If the entity cannot be stacked according to {@link #isStackable(LivingEntity)}, nothing happens.
	 * 
	 * @param entity the entity to change
	 * @param health the updated health
	 */
	void setHealth(LivingEntity entity, double health);
	
	/**
	 * Attempts to merge an entity into surrounding entities.
	 * 
	 * @param entity the entity to merge
	 * @param cause the StackCause, use StackCause.PLUGIN
	 */
	void attemptMerges(LivingEntity entity, StackCause cause);
	
	/**
	 * Checks if the entity is stacked
	 * 
	 * @param entity the entity to check
	 * @return true if and only if the entity is a stacked entity
	 */
	boolean isStacked(LivingEntity entity);
	
	/**
	 * Checks whether an entity may ever be stacked.
	 * Nonliving entities, players, and entity types filtered in the config.yml cannot be stacked.
	 * 
	 * @param entity the entity to check
	 * @return true if and only if the entity is included in possible future stacking
	 */
	boolean isStackable(LivingEntity entity);
	
	/**
	 * Reloads plugin configuration (same as /mobstacker reload)
	 * 
	 */
	void reload();
	
}
