package io.github.vilguax.reforged.effect;

import io.github.vilguax.reforged.ModEnchantments;
import io.github.vilguax.reforged.util.BulkBreaker;
import io.github.vilguax.reforged.util.EnchantUtil;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Logique custom de l'enchantement Tunnelier (Tunneler), porte sur une pioche.
 *
 * Casser un bloc minable a la pioche elargit le creusage a un plan NxN
 * perpendiculaire a la face regardee, sur une seule profondeur :
 * lvl1 = 3x3, lvl2 = 4x4, lvl3 = 5x5. Le cassage est delegue a
 * {@link BulkBreaker}, donc le plan compose avec Auto-Smelt / Magnet / Vein Miner.
 */
public final class TunnelerHandler {
	private TunnelerHandler() {
	}

	public static void register() {
		PlayerBlockBreakEvents.AFTER.register(TunnelerHandler::onBlockBroken);
	}

	private static void onBlockBroken(net.minecraft.world.World world, PlayerEntity player,
			BlockPos origin, BlockState state, BlockEntity blockEntity) {
		if (BulkBreaker.isActive() || !(world instanceof ServerWorld serverWorld)) {
			return;
		}

		ItemStack tool = player.getMainHandStack();
		int level = EnchantUtil.getLevel(tool, ModEnchantments.TUNNELER);
		if (level <= 0) {
			return;
		}

		// Le plan est perpendiculaire a l'axe que le joueur regarde.
		Vec3d look = player.getRotationVector();
		Direction.Axis axis = Direction.getFacing(look.x, look.y, look.z).getAxis();

		int size = level + 2; // lvl1=3, lvl2=4, lvl3=5
		int lo = -(size - 1) / 2;
		int hi = size / 2;

		List<BlockPos> blocks = new ArrayList<>();
		for (int a = lo; a <= hi; a++) {
			for (int b = lo; b <= hi; b++) {
				if (a == 0 && b == 0) {
					continue; // origine deja cassee par vanilla
				}
				BlockPos pos = switch (axis) {
					case X -> origin.add(0, a, b);
					case Y -> origin.add(a, 0, b);
					case Z -> origin.add(a, b, 0);
				};
				if (isBreakable(serverWorld, pos)) {
					blocks.add(pos);
				}
			}
		}

		BulkBreaker.breakAll(serverWorld, player, origin, tool, blocks);
	}

	/** Minable a la pioche, present, et non indestructible (exclut bedrock & co). */
	private static boolean isBreakable(ServerWorld world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		return !state.isAir()
				&& state.isIn(BlockTags.PICKAXE_MINEABLE)
				&& state.getHardness(world, pos) >= 0;
	}
}
