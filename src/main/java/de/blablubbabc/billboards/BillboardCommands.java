package de.blablubbabc.billboards;

import de.blablubbabc.billboards.entry.BillboardSign;
import de.blablubbabc.billboards.entry.HologramHolder;
import de.blablubbabc.billboards.message.Message;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.blablubbabc.billboards.util.SoftBlockLocation;
import de.blablubbabc.billboards.util.Utils;
import org.jetbrains.annotations.NotNull;

public class BillboardCommands implements CommandExecutor {

	private final BillboardsPlugin plugin;

	BillboardCommands(BillboardsPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(Message.ONLY_AS_PLAYER.get());
			return true;
		}
		Player player = (Player) sender;

		// check permission:
		boolean hasAdminPermission = player.hasPermission(BillboardsPlugin.ADMIN_PERMISSION);
		if (!hasAdminPermission && !player.hasPermission(BillboardsPlugin.CREATE_PERMISSION)) {
			player.sendMessage(Message.NO_PERMISSION.get());
			return true;
		}
		if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
			plugin.reloadMessages();
			plugin.reloadConfig();
			player.sendMessage(Message.RELOADED.get());
			return true;
		}
		if (args.length > 3) {
			return false;
		}

		// get targeted sign:
		Block targetBlock = player.getTargetBlock(null, 10);
		if (Utils.isNotSign(targetBlock.getType())) {
			player.sendMessage(Message.NO_TARGETED_SIGN.get());
			return true;
		}
		SoftBlockLocation blockLocation = new SoftBlockLocation(targetBlock);

		// already a billboard sign?
		if (plugin.getBillboard(blockLocation) != null) {
			player.sendMessage(Message.ALREADY_BILLBOARD_SIGN.get());
			return true;
		}

		// create new billboard:
		int duration = plugin.defaultDurationInDays;
		int price = plugin.defaultPrice;

		// /billboard [<price> <duration>] [creator]
		if (args.length >= 2) {
			Integer priceArgument = Utils.parseInteger(args[0]);
			if (priceArgument == null) {
				player.sendMessage(Message.INVALID_NUMBER.get(args[0]));
				return true;
			}
			Integer durationArgument = Utils.parseInteger(args[1]);
			if (durationArgument == null) {
				player.sendMessage(Message.INVALID_NUMBER.get(args[1]));
				return true;
			}
			price = priceArgument;
			duration = durationArgument;
		}

		Player creator = hasAdminPermission ? null : player;
		if (args.length == 1 || args.length == 3) {
			if (!hasAdminPermission) {
				player.sendMessage(Message.NO_PERMISSION.get());
				return true;
			}
			String creatorName = args[args.length == 1 ? 0 : 2];
			// TODO support offline players
			creator = Bukkit.getPlayer(creatorName);
			if (creator == null) {
				player.sendMessage(Message.PLAYER_NOT_FOUND.get(creatorName));
				return true;
			}
		}

		HologramHolder hologram = null; // TODO: 创建悬浮字广告牌

		// add and setup billboard sign:
		BillboardSign billboard = new BillboardSign(hologram, hologram == null ? blockLocation : null, creator, duration, price);
		plugin.addBillboard(billboard);
		plugin.refreshSign(billboard);
		plugin.saveBillboards();

		String[] msgArgs = billboard.getMessageArgs();
		player.sendMessage(Message.ADDED_SIGN.get(msgArgs));
		return true;
	}
}
