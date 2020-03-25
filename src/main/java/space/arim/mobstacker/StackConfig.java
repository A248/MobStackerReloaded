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

import java.io.File;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;

import space.arim.api.config.SimpleConfig;

public class StackConfig extends SimpleConfig {

	private double stacking_radiusX;
	private double stacking_radiusY;
	private double stacking_radiusZ;
	
	public StackConfig(File folder) {
		super(folder, "config.yml", "do-not-touch-version");
	}
	
	@Override
	public void reload() {
		super.reload();
		stacking_radiusX = getInt("stacking.radius.x").doubleValue();
		stacking_radiusY = getInt("stacking.radius.y").doubleValue();
		stacking_radiusZ = getInt("stacking.radius.z").doubleValue();
	}
	
	double radiusX() {
		return stacking_radiusX;
	}
	
	double radiusY() {
		return stacking_radiusY;
	}
	
	double radiusZ() {
		return stacking_radiusZ;
	}
	
	private boolean isEntityTypeAllowed(EntityType type) {
		List<String> typeList = getStrings("stacking.exempt.types");
		return getBoolean("stacking.exempt.use-as-whitelist") ? typeList.contains(type.name()) : !typeList.contains(type.name());
	}
	
	boolean isTypeStackable(LivingEntity entity) {
		return !(entity instanceof Player) && !(entity instanceof Slime) && isEntityTypeAllowed(entity.getType());
	}
	
	private boolean isCorrectWorld(World world) {
		List<String> worlds = getStrings("triggers.per-world.worlds");
		return getBoolean("triggers.per-world.use-as-whitelist") ? worlds.contains(world.getName()) : !worlds.contains(world.getName());
	}
	
	boolean isCorrectLocation(Location location) {
		return isCorrectWorld(location.getWorld());
	}
	
	boolean mergeable(LivingEntity entity, LivingEntity other) {
		if (entity.getType() != other.getType()) {
			return false;
		}
		if (getBoolean("stacking.separation.age.enable") && entity instanceof Ageable) {
			Ageable age1 = (Ageable) entity;
			Ageable age2 = (Ageable) other;
			return (getBoolean("stacking.separation.age.strict")) ? age1.getAge() == age2.getAge()
					: (age1.isAdult() && age2.isAdult() || !age1.isAdult() && !age2.isAdult());
		}
		return true;
	}
	
	String toStringEntity(EntityType type) {
		char[] result = type.toString().toLowerCase().toCharArray();
		result[0] = Character.toUpperCase(result[0]);
		return String.valueOf(result);
	}

}
