package io.github.vilguax.reforged;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/**
 * Cles de registre des enchantements du mod.
 *
 * En 1.21.x les enchantements sont data-driven : la definition (cout, table,
 * items supportes...) vit dans data/reforged/enchantment/*.json. Cote Java on
 * garde juste une RegistryKey pour pouvoir tester la presence/le niveau sur un
 * ItemStack dans la logique custom.
 */
public final class ModEnchantments {
	private ModEnchantments() {
	}

	public static final RegistryKey<Enchantment> LIFESTEAL = of("lifesteal");
	public static final RegistryKey<Enchantment> AUTO_SMELT = of("auto_smelt");
	public static final RegistryKey<Enchantment> VEIN_MINER = of("vein_miner");
	public static final RegistryKey<Enchantment> MAGNET = of("magnet");
	public static final RegistryKey<Enchantment> AUTO_REPLANT = of("auto_replant");
	public static final RegistryKey<Enchantment> BLINK = of("blink");

	private static RegistryKey<Enchantment> of(String name) {
		return RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Reforged.MOD_ID, name));
	}
}
