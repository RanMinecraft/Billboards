package de.blablubbabc.billboards.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Items {

	public static void setItemDisplayName(ItemStack item, String name) {
		if (item == null)
			return;
		ItemMeta im = item.getItemMeta() == null ? getItemMeta(item.getType()) : item.getItemMeta();
		if (im == null)
			return;
		im.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
		item.setItemMeta(im);
	}

	public static void setItemLore(ItemStack item, List<String> lore) {
		if (item == null)
			return;
		ItemMeta im = item.getItemMeta() == null ? getItemMeta(item.getType()) : item.getItemMeta();
		if (im == null)
			return;
		List<String> newLore = new ArrayList<>();
		lore.forEach(s -> newLore.add(ChatColor.translateAlternateColorCodes('&', s)));
		im.setLore(newLore);
		item.setItemMeta(im);
	}

	public static ItemMeta getItemMeta(Material material) {
		return Bukkit.getItemFactory().getItemMeta(material);
	}

	@NotNull
	public static ItemMeta getItemMeta(@NotNull ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		return meta == null ? getItemMeta(item.getType()) : meta;
	}

	public static void setGlow(ItemStack item) {
		ItemMeta meta = getItemMeta(item);
		meta.addEnchant(Enchantment.UNBREAKING, 1, true);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		item.setItemMeta(meta);
	}
}
