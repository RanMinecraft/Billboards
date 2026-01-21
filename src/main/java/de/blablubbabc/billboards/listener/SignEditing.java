package de.blablubbabc.billboards.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLib;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.google.common.collect.Lists;
import de.blablubbabc.billboards.BillboardsPlugin;
import de.blablubbabc.billboards.entry.BillboardSign;
import de.blablubbabc.billboards.entry.HologramHolder;
import de.blablubbabc.billboards.entry.SignEdit;
import de.blablubbabc.billboards.message.Message;
import de.blablubbabc.billboards.util.SoftBlockLocation;
import de.blablubbabc.billboards.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static de.blablubbabc.billboards.entry.SignEdit.sendBlockChange;

public class SignEditing implements Listener {

	private final BillboardsPlugin plugin;
	// player name -> editing information
	private final Map<String, SignEdit> editing = new HashMap<>();
	private ProtocolManager protocolManager;
	private int bedrock;
	private String verPL, verMC;
	public SignEditing(BillboardsPlugin plugin) {
		this.plugin = plugin;
	}

	public void onPluginEnable() {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		protocolManager = ProtocolLibrary.getProtocolManager();
		bedrock = protocolManager.getMinecraftVersion().isAtLeast(new MinecraftVersion("1.18")) ? -64 : 0;
		verPL = ProtocolLib.getPlugin(ProtocolLib.class).getDescription().getVersion();
		verMC = protocolManager.getMinecraftVersion().getVersion();
		protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.UPDATE_SIGN) {
			@Override
			public void onPacketReceiving(PacketEvent event) {
				BillboardsPlugin plugin = SignEditing.this.plugin;
				Player player = event.getPlayer();
				SignEdit signEdit = endSignEdit(player);
				if (signEdit == null) {
					return; // player wasn't editing
				}
				event.setCancelled(true);

				String[] input;
				PacketContainer packet = event.getPacket();
				StructureModifier<String[]> stringArrays = packet.getStringArrays();
				StructureModifier<String> strings = packet.getStrings();
				if (stringArrays.size() > 0) {
					input = stringArrays.read(0);
				} else if (strings.size() >= 4) {
					input = new String[] {
							strings.read(0),
							strings.read(1),
							strings.read(2),
							strings.read(3)
					};
				} else {
					plugin.getLogger().warning("收到来自玩家 " + player.getName() + " 的无效 UPDATE_SIGN 包，这代表插件可能不支持当前版本");
					return;
				}
				String[] lines = encodeColor(input, player);
				if (plugin.refreshSign(signEdit.billboard)) {
					// still owner and has still the permission?
					if (signEdit.billboard.canEdit(player) && player.hasPermission(BillboardsPlugin.RENT_PERMISSION)) {
						// update billboard sign content:
						SoftBlockLocation signLoc = signEdit.billboard.getLocation();
						HologramHolder hologram = signEdit.billboard.getHologram();
						if (hologram != null) {
							plugin.getScheduler().runNextTick((t) -> { // 更新悬浮字
								List<String> list = Lists.newArrayList(lines);
								hologram.setLines(list);
							});
						} else if (signLoc != null) {
							Location location = signLoc.getBukkitLocation();
							plugin.getScheduler().runAtLocation(location, (t) -> { // 更新木牌
								Sign target = (Sign) location.getBlock().getState();
								for (int i = 0; i < lines.length && i < 4; i++) {
									target.setLine(i, lines[i]);
								}
								target.update();
							});
						}
					} else {
						player.sendMessage(Message.NO_PERMISSION.get());
					}
				} else {
					player.sendMessage(Message.INVALID_BILLBOARD.get());
				}

				plugin.getScheduler().runLater((t) -> {
					if (player.isOnline()) {
						signEdit.sendBlockChange(player);
					}
				}, 2L);
			}
		});
	}

	public void onPluginDisable() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			this.endSignEdit(player);
		}
		if (protocolManager != null) protocolManager.removePacketListeners(plugin);
	}
	public String[] encodeColor(String[] lines, Player player) {
		boolean color = player == null || player.hasPermission(BillboardsPlugin.SIGN_COLOR_PERMISSION);
		boolean format = player == null || player.hasPermission(BillboardsPlugin.SIGN_FORMAT_PERMISSION);
		boolean magic = player == null || player.hasPermission(BillboardsPlugin.SIGN_FORMAT_MAGIC_PERMISSION);
		if (color || format || magic) {
			for (int i = 0; i < lines.length; i++) {
				String line = lines[i];
				if (color) line = line.replaceAll("&([0-9A-Fa-f])", "§$1");
				if (format) line = line.replaceAll("&([LMNORlmnor])", "§$1");
				if (magic) line = line.replace("&k", "§k");
				lines[i] = line;
			}
		}
		return lines;
	}
	public String[] decodeColor(String[] lines) {
		for (int i = 0; i < lines.length; i++) {
			lines[i] = lines[i].replace("§", "&");
		}
		return lines;
	}
	public String[] decodeColor(List<String> lines) {
		String[] array = new String[lines.size()];
		for (int i = 0; i < lines.size(); i++) {
			array[i] = lines.get(i).replace("§", "&");
		}
		return array;
	}
	public static Material getSignMaterial() {
		return Utils.parseMat("OAK_SIGN")
				.orElseGet(() -> Utils.parseMat("SIGN_POST").orElse(null));
	}
	public void openSignEdit(Player player, BillboardSign billboard) {
		if (!player.isOnline() || editing.containsKey(player.getName())) {
			return;
		}
		Location location = player.getLocation().clone();
		int blockY = location.getBlockY();
		if (blockY - 4 < bedrock) {
			location.setY(blockY + 4);
		} else {
			location.setY(blockY - 4);
		}

		HologramHolder hologram = billboard.getHologram();

		String[] content;
		if (hologram != null) {
			content = decodeColor(hologram.getLines());
			// create a fake sign
			sendBlockChange(player, location, getSignMaterial());
		} else {
			Block block = billboard.getLocation().getBukkitLocation().getBlock();
			BlockState state = block.getState();
			if (!(state instanceof Sign)) return;
			content = decodeColor(((Sign) state).getLines());
			// create a fake sign
			sendBlockChange(player, location, block);
		}

		// send fake sign lines
		player.sendSignChange(location, content);

		String packetClass = "unknown";
		try {
			// open sign edit gui for player
			PacketType packetType = PacketType.Play.Server.OPEN_SIGN_EDITOR;
			PacketContainer openSign = new PacketContainer(packetType);
			packetClass = openSign.getType().name() + " " + openSign.getHandle().getClass().getName();
			openSignEditor(location, openSign);
			ProtocolLibrary.getProtocolManager().sendServerPacket(player, openSign);
			editing.put(player.getName(), new SignEdit(billboard, location));
		} catch (Throwable t) {
			plugin.getLogger().log(Level.SEVERE, "为玩家 " + player.getName() + " 打开木牌编辑界面时出现问题 (ProtocolLib " + verPL + ", Minecraft " + verMC + ", PacketClass=" + packetClass + ") 如果 PacketClass 不是 OpenSignEditor，请考虑升级 ProtocolLib", t);
		}
	}

	private void openSignEditor(Location loc, PacketContainer packet) {
		BlockPosition position = new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		packet.getBlockPositionModifier().write(0, position); // index out of bounds
		StructureModifier<Boolean> booleans = packet.getBooleans();
		if (booleans.size() > 0) { // 1.20.4+ isFrontText
			booleans.write(0, true);
		}
	}

	// returns null if the player was editing
	public SignEdit endSignEdit(Player player) {
        return editing.remove(player.getName());
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		this.endSignEdit(event.getPlayer());
	}
}
