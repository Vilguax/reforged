package io.github.vilguax.reforged.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Cassage groupe partage par les enchantements de zone (Vein Miner, Bucheron,
 * Tunnelier, Faucheur...).
 *
 * Tous les drops sont regroupes a la position d'origine : ainsi ils tombent
 * dans la boite scannee en fin de tick par Auto-Smelt / Magnet et composent
 * avec eux, quelle que soit la distance. Fortune est appliquee via l'outil et
 * l'XP des blocs (minerais...) est laissee sur place.
 *
 * Cout en durabilite : 1 point par bloc supplementaire casse, comme si on
 * l'avait mine a la main (le bloc d'origine est lui deja use par vanilla).
 * Casser une grosse zone use donc l'outil a proportion ; Solidite reste un vrai
 * levier puisque damage() roule la reduction par point.
 */
public final class BulkBreaker {
	/** Garde anti-reentrance : nos cassages ne doivent pas se re-declencher. */
	private static boolean active = false;

	private BulkBreaker() {
	}

	/** Vrai pendant qu'un cassage groupe est en cours (a tester en debut de handler). */
	public static boolean isActive() {
		return active;
	}

	/**
	 * Casse {@code positions} (le bloc d'origine est suppose deja casse par
	 * vanilla), regroupe leurs drops a {@code origin}, laisse l'XP sur place,
	 * puis use l'outil de facon degressive. Les positions deja vides sont
	 * ignorees.
	 */
	public static void breakAll(ServerWorld world, PlayerEntity player, BlockPos origin,
			ItemStack tool, List<BlockPos> positions) {
		if (positions.isEmpty()) {
			return;
		}
		int broken = 0;
		active = true;
		try {
			for (BlockPos pos : positions) {
				BlockState state = world.getBlockState(pos);
				if (state.isAir()) {
					continue;
				}
				BlockEntity be = world.getBlockEntity(pos);
				for (ItemStack drop : Block.getDroppedStacks(state, world, pos, be, player, tool)) {
					Block.dropStack(world, origin, drop);
				}
				state.onStacksDropped(world, pos, tool, true);
				world.breakBlock(pos, false, player);
				broken++;
			}
		} finally {
			active = false;
		}
		applyDurability(tool, player, broken);
	}

	/**
	 * 1 point de durabilite par bloc supplementaire casse. Applique en un seul
	 * appel : damage() roule la reduction de Solidite par point, comme en vanilla.
	 */
	private static void applyDurability(ItemStack tool, PlayerEntity player, int blocks) {
		if (player.isCreative() || blocks <= 0 || !tool.isDamageable()) {
			return;
		}
		tool.damage(blocks, player, EquipmentSlot.MAINHAND);
	}
}
