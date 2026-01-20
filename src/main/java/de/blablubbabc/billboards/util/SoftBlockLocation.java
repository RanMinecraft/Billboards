package de.blablubbabc.billboards.util;

import org.apache.commons.lang.util.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public class SoftBlockLocation {

	public final String worldName; // not empty
	public final int x;
	public final int y;
	public final int z;

	public SoftBlockLocation(Block block) {
		this(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}

	public SoftBlockLocation(String worldName, int x, int y, int z) {
		Validate.isTrue(!Utils.isEmpty(worldName), "World name is empty!");
		this.worldName = worldName;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Location getBukkitLocation() {
		World world = Bukkit.getWorld(worldName);
		if (world == null) return null;
		return new Location(world, x, y, z);
	}

	@Override
	public String toString() {
		return worldName + ";" + x + ";" + y + ";" + z;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + worldName.hashCode();
		result = prime * result + x;
		result = prime * result + y;
		result = prime * result + z;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof SoftBlockLocation)) return false;
		SoftBlockLocation other = (SoftBlockLocation) obj;
		return worldName.equals(other.worldName)
				&& x == other.x
				&& y == other.y
				&& z == other.z;
	}

	public static SoftBlockLocation getFromString(String string) {
		if (string == null) return null;
		String[] split = string.split(";");
		if (split.length != 4) return null;
		String worldName = split[0];
		if (worldName == null) return null;
		Integer x = Utils.parseInteger(split[1]);
		if (x == null) return null;
		Integer y = Utils.parseInteger(split[2]);
		if (y == null) return null;
		Integer z = Utils.parseInteger(split[3]);
		if (z == null) return null;
		return new SoftBlockLocation(worldName, x, y, z);
	}

	public static Location getBukkit(String string) {
		if (string == null) return null;
		String[] split = string.split(";");
		if (split.length != 4) return null;
		String worldName = split[0];
		if (worldName == null) return null;
		Double x = Utils.parseDouble(split[1]);
		if (x == null) return null;
		Double y = Utils.parseDouble(split[2]);
		if (y == null) return null;
		Double z = Utils.parseDouble(split[3]);
		if (z == null) return null;
		return new Location(Bukkit.getWorld(worldName), x, y, z);
	}
}
