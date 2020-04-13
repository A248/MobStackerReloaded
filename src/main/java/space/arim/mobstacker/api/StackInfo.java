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

/**
 * Information relating to a stacked entity.
 * 
 * @author A248
 *
 */
public interface StackInfo {

	/**
	 * The size of the stack, or how many mobs are contained within it. <br>
	 * <br>
	 * <b>If the entity cannot be stacked</b>, <code>0</code> is returned.
	 * 
	 * @return the size
	 */
	int getSize();
	
	/**
	 * The health of the entire stack, which is changed by AOE damage to the stack. <br>
	 * <br>
	 * <b>If the entity cannot be stacked</b>, <code>0</code> is returned.
	 * 
	 * @return the health
	 */
	double getHealth();
	
}
