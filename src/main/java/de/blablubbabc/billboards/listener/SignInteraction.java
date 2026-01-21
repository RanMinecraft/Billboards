package de.blablubbabc.billboards.listener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.blablubbabc.billboards.BillboardsPlugin;
import de.blablubbabc.billboards.entry.BillboardSign;
import de.blablubbabc.billboards.message.Message;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import de.blablubbabc.billboards.util.SoftBlockLocation;
import de.blablubbabc.billboards.util.Utils;

import net.milkbowl.vault.economy.EconomyResponse;

public class SignInteraction implements Listener {

	private final BillboardsPlugin plugin;
	// player name -> currently interacting billboard sign
	public final Map<String, BillboardSign> confirmations = new HashMap<>();
	private SimpleDateFormat dateFormat;

	public SignInteraction(BillboardsPlugin plugin) {
		this.plugin = plugin;
	}

	public void onPluginEnable() {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public void onReloadMessages() {
		dateFormat = new SimpleDateFormat(Message.DATE_FORMAT.get());
	}

	public void onPluginDisable() {
		this.confirmations.clear();
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onInteract(PlayerInteractEvent event) {
		Block clickedBlock = event.getClickedBlock();
		if (clickedBlock == null || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		Player player = event.getPlayer();
		String playerName = player.getName();

		// reset confirmation status:
		BillboardSign confirmationBillboard = confirmations.remove(playerName);
		if (Utils.isNotSign(clickedBlock.getType())) return; // not a sign

		SoftBlockLocation blockLocation = new SoftBlockLocation(clickedBlock);
		BillboardSign billboard = plugin.getBillboard(blockLocation);
		if (billboard == null || !plugin.refreshSign(billboard)) return; // not a valid billboard sign

		// cancel all block-placing against a billboard sign already here:
		event.setCancelled(true);

		// can rent?
		if (!player.hasPermission(BillboardsPlugin.RENT_PERMISSION)) {
			player.sendMessage(Message.NO_PERMISSION.get());
			return;
		}
		// own sign?
		if (billboard.isCreator(player)) {
			player.sendMessage(Message.CANT_RENT_OWN_SIGN.get());
			return;
		}

		if (confirmationBillboard != null && confirmationBillboard == billboard) {
			// check if it's still available:
			if (billboard.hasOwner()) {
				// no longer available:
				player.sendMessage(Message.NO_LONGER_AVAILABLE.get());
				return;
			}

			// check if player has enough money:
			if (!BillboardsPlugin.economy.has(player, billboard.getPrice())) {
				// not enough money:
				player.sendMessage(Message.NOT_ENOUGH_MONEY.get(String.valueOf(billboard.getPrice()), String.valueOf(BillboardsPlugin.economy.getBalance(player))));
				return;
			}

			// rent:
			// take money:
			EconomyResponse withdraw = BillboardsPlugin.economy.withdrawPlayer(player, billboard.getPrice());
			// transaction successful ?
			if (!withdraw.transactionSuccess()) {
				// something went wrong
				player.sendMessage(Message.TRANSACTION_FAILURE.get(withdraw.errorMessage));
				return;
			}

			if (billboard.hasCreator()) {
				// give money to the creator:
				OfflinePlayer creatorPlayer = Bukkit.getOfflinePlayer(billboard.getCreatorUUID());
				// note: OfflinePlayer.getName() will return null if the player's last known name is unknown to the
				// server
				EconomyResponse deposit = BillboardsPlugin.economy.depositPlayer(creatorPlayer, billboard.getPrice());
				// transaction successful ?
				if (!deposit.transactionSuccess()) {
					// something went wrong :(
					player.sendMessage(Message.TRANSACTION_FAILURE.get(deposit.errorMessage));

					// try to refund the withdraw
					EconomyResponse withdrawUndo = BillboardsPlugin.economy.depositPlayer(player, withdraw.amount);
					if (!withdrawUndo.transactionSuccess()) {
						// this is really bad:
						player.sendMessage(Message.TRANSACTION_FAILURE.get(withdrawUndo.errorMessage));
					}
					player.updateInventory();
					return;
				}
			}
			player.updateInventory();

			// set new owner:
			billboard.setOwner(player);
			billboard.setStartTime(System.currentTimeMillis());
			plugin.saveBillboards();

			// initialize new sign text:
			Sign sign = (Sign) clickedBlock.getState();
			String[] msgArgs = billboard.getMessageArgs();
			sign.setLine(0, Message.RENT_SIGN_LINE_1.get(msgArgs));
			sign.setLine(1, Message.RENT_SIGN_LINE_2.get(msgArgs));
			sign.setLine(2, Message.RENT_SIGN_LINE_3.get(msgArgs));
			sign.setLine(3, Message.RENT_SIGN_LINE_4.get(msgArgs));
			sign.update();

			player.sendMessage(Message.YOU_HAVE_RENT_A_SIGN.get(msgArgs));
		} else {
			// check if available:
			if (!billboard.hasOwner()) {
				// check if the player already owns to many billboards:
				if (plugin.maxBillboardsPerPlayer >= 0 && plugin.getRentBillboards(player.getUniqueId()).size() >= plugin.maxBillboardsPerPlayer) {
					player.sendMessage(Message.MAX_RENT_LIMIT_REACHED.get(String.valueOf(plugin.maxBillboardsPerPlayer)));
					return;
				}

				// check if player has enough money:
				if (!BillboardsPlugin.economy.has(player, billboard.getPrice())) {
					// no enough money:
					player.sendMessage(Message.NOT_ENOUGH_MONEY.get(String.valueOf(billboard.getPrice()),
							String.valueOf(BillboardsPlugin.economy.getBalance(player))));
					return;
				}

				// click again to rent:
				confirmations.put(playerName, billboard);
				player.sendMessage(Message.CLICK_TO_RENT.get(billboard.getMessageArgs()));
			} else {
				// is owner -> edit
				if (billboard.canEdit(player)) {
					if (player.isSneaking()) {
						plugin.signEditGuiConfig.gui(player, billboard).open();
					} else {
						printBillboard(player, billboard);
					}
				} else if (!billboard.getCommandArg().isEmpty()) {
					if (player.isSneaking()) {
						printBillboard(player, billboard);
					} else {
						plugin.signEditGuiConfig.executeClickCommand(player, billboard.getCommandArg());
					}
				} else {
					printBillboard(player, billboard);
				}
			}
		}
	}

	void printBillboard(Player player, BillboardSign billboard) {
		// print information of sign:
		player.sendMessage(Message.INFO_HEADER.get());
		player.sendMessage(Message.INFO_CREATOR.get(billboard.getMessageCreatorName(), billboard.getMessageCreatorUUID()));
		player.sendMessage(Message.INFO_OWNER.get(billboard.getMessageOwnerName(), billboard.getMessageOwnerUUID()));
		player.sendMessage(Message.INFO_PRICE.get(String.valueOf(billboard.getPrice())));
		player.sendMessage(Message.INFO_DURATION.get(String.valueOf(billboard.getDurationInDays())));
		player.sendMessage(Message.INFO_RENT_SINCE.get(dateFormat.format(new Date(billboard.getStartTime()))));

		long endTime = billboard.getEndTime();
		player.sendMessage(Message.INFO_RENT_UNTIL.get(dateFormat.format(new Date(endTime))));

		long left = billboard.getTimeLeft();
		long days = TimeUnit.MILLISECONDS.toDays(left);
		long hours = TimeUnit.MILLISECONDS.toHours(left) - TimeUnit.DAYS.toHours(days);
		long minutes = TimeUnit.MILLISECONDS.toMinutes(left) - TimeUnit.DAYS.toMinutes(days) - TimeUnit.HOURS.toMinutes(hours);
		String timeLeft = String.format(Message.TIME_REMAINING_FORMAT.get(), days, hours, minutes);

		player.sendMessage(Message.INFO_TIME_LEFT.get(timeLeft));
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		confirmations.remove(player.getName());
	}
}
