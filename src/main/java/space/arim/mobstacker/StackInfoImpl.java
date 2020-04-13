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

import space.arim.mobstacker.api.StackInfo;

class StackInfoImpl implements StackInfo {

	private final int amount;
	private final double health;

	StackInfoImpl(int amount, double health) {
		this.amount = amount;
		this.health = health;
	}

	@Override
	public int getSize() {
		return amount;
	}

	@Override
	public double getHealth() {
		return health;
	}

	StackInfoImpl combine(StackInfoImpl other) {
		// use weighted average of health based on respective stack sizes
		return new StackInfoImpl(amount+other.amount, (health*amount + other.health*other.amount)/(amount + other.amount));
	}

}
