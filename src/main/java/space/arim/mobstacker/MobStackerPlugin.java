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

import org.bstats.bukkit.Metrics;

import org.bukkit.plugin.java.JavaPlugin;

public class MobStackerPlugin extends JavaPlugin {

	private MobStacker core;
	
	@Override
	public void onEnable() {
		core = new MobStacker(this);
		core.load();
		Metrics metrics = new Metrics(this, 6532);
		if (metrics.isEnabled()) {
			getLogger().info("Metrics enabled!");
		}
	}
	
	@Override
	public void onDisable() {
		core.close();
		core = null;
	}
	
}
