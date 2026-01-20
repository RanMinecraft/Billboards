package de.blablubbabc.billboards.entry;

import de.blablubbabc.billboards.util.Utils;
import org.apache.commons.lang.util.Validate;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class SignEdit {

	public final BillboardSign billboard;
	public final Location location;

	public SignEdit(BillboardSign billboard, Location location) {
		Validate.notNull(billboard, "Billboard is null!");
		Validate.notNull(location, "Source location is null!");
		this.billboard = billboard;
		this.location = location;
	}

	@SuppressWarnings({"deprecation"})
	public void sendBlockChange(Player player) {
		Block block = location.getBlock();
		if (Utils.hasBlockData()) {
			player.sendBlockChange(location, block.getBlockData());
		} else {
			player.sendBlockChange(location, block.getType(), block.getData());
		}
	}

	@SuppressWarnings({"deprecation"})
	public static void sendBlockChange(Player player, Location location, Material material) {
		if (Utils.hasBlockData()) {
			player.sendBlockChange(location, material.createBlockData());
		} else {
			player.sendBlockChange(location, material, (byte) 0);
		}
	}

	@SuppressWarnings({"deprecation"})
	public static void sendBlockChange(Player player, Location location, Block block) {
		if (Utils.hasBlockData()) {
			player.sendBlockChange(location, block.getBlockData());
		} else {
			player.sendBlockChange(location, block.getType(), block.getData());
		}
	}
}
