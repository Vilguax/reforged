package io.github.vilguax.reforged.util;

import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;

/** Helpers pour lire les enchantements du mod sur un ItemStack. */
public final class EnchantUtil {
	private EnchantUtil() {
	}

	/** Niveau de l'enchantement {@code key} sur {@code stack}, ou 0 si absent. */
	public static int getLevel(ItemStack stack, RegistryKey<Enchantment> key) {
		if (stack.isEmpty()) {
			return 0;
		}
		ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(stack);
		for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
			if (entry.matchesKey(key)) {
				return enchantments.getLevel(entry);
			}
		}
		return 0;
	}
}
