package io.github.vilguax.reforged;

import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/** Tags du mod. */
public final class ModTags {
	private ModTags() {
	}

	/** Blocs que le Vein Miner est autorise a casser en chaine (minerais uniquement). */
	public static final TagKey<Block> VEIN_MINEABLE =
			TagKey.of(RegistryKeys.BLOCK, Identifier.of(Reforged.MOD_ID, "vein_mineable"));
}
