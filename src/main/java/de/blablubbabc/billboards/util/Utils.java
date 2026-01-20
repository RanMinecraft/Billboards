package de.blablubbabc.billboards.util;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class Utils {
	private static Boolean hasPAPI = null;
	private static boolean hasBlockData;
	public static void init() {
		try {
			Class.forName("me.clip.placeholderapi.PlaceholderAPI");
			hasPAPI = true;
		} catch (ClassNotFoundException e) {
			hasPAPI = false;
		}
		hasPAPI = getClass("me.clip.placeholderapi.PlaceholderAPI") != null;
		hasBlockData = getClass("org.bukkit.block.data.BlockData") != null;
	}

	public static boolean hasBlockData() {
		return hasBlockData;
	}

	public static void runCommand(Player player, String command, Object... args) {
		if (command.startsWith("player:")) {
			String s = String.format(command.substring(7), args);
			Bukkit.dispatchCommand(player, papi(player, s));
		}
		if (command.startsWith("console:")) {
			String s = String.format(command.substring(8), args);
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), papi(player, s));
		}
	}
	public static String papi(OfflinePlayer p, String s) {
		String name = p.getName();
		if (!hasPAPI) return s.replace("%player_name%", name == null ? "" : name);
		return PlaceholderAPI.setPlaceholders(p, s);
	}
	public static String papi(Player p, String s) {
		if (!hasPAPI) return s.replace("%player_name%", p.getName());
		return PlaceholderAPI.setPlaceholders(p, s);
	}

	public static List<String> papi(OfflinePlayer p, List<String> s) {
		String name = p.getName();
		if (!hasPAPI) return Lists.newArrayList(String.join("\n", s).replace("%player_name%", name == null ? "" : name).split("\n"));
		return PlaceholderAPI.setPlaceholders(p, s);
	}

	public static Optional<Material> parseMat(String s) {
		if (s != null && !s.isEmpty()) {
			for (Material m : Material.values()) {
				if (m.name().equalsIgnoreCase(s)) {
					return Optional.of(m);
				}
			}
		}
		return Optional.empty();
	}

	public static boolean isEmpty(String string) {
		return (string == null || string.isEmpty());
	}

	private static final Set<String> legacySigns = Sets.newHashSet("SIGN", "SIGN_POST", "WALL_SIGN");
	public static boolean isNotSign(Material material) {
		if (material == null) return true;
		if (hasBlockData) {
            Class<?> data = material.data;
            if (data.isAssignableFrom(org.bukkit.block.data.type.Sign.class)) return false;
            if (data.isAssignableFrom(org.bukkit.block.data.type.WallSign.class)) return false;
			if (data.isAssignableFrom(org.bukkit.block.data.type.HangingSign.class)) return false;
            try {
                if (data.isAssignableFrom(org.bukkit.block.data.type.HangingSign.class)) return false;
                if (data.isAssignableFrom(org.bukkit.block.data.type.WallHangingSign.class)) return false;
				if (data.isAssignableFrom(org.bukkit.block.data.type.HangingSign.class)) return false;
            } catch (Throwable ignored) {
            }
            return true;
		}
		return !legacySigns.contains(material.name().toUpperCase());
	}

	public static Integer parseInteger(String string) {
		try {
			return Integer.parseInt(string);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static Double parseDouble(String string) {
		try {
			return Double.parseDouble(string);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static UUID parseUUID(String string) {
		if (string == null) return null;
		try {
			return UUID.fromString(string);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public static long addSaturated(long x, long y) {
		try {
			return Math.addExact(x, y);
		} catch (ArithmeticException e) {
			if (y > 0) {
				return Long.MAX_VALUE;
			} else {
				return Long.MIN_VALUE;
			}
		}
	}

	@Nullable
	public static Class<?> getClass(String name) {
		try {
			return Class.forName(name);
		} catch (Throwable t) {
			return null;
		}
	}
}
