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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Logique custom de l'enchantement Bucheron (Lumberjack), porte sur une hache.
 *
 * Casser une buche (tag {@code #minecraft:logs}) avec une hache enchantee
 * abat aussi les buches connectees (voisinage 26), jusqu'a un plafond qui
 * grandit avec le niveau ; au niveau max le plafond couvre un arbre entier
 * quelle que soit sa taille. Le cassage est delegue a {@link BulkBreaker}, donc
 * les buches composent avec Auto-Smelt (buche -> charbon) et Magnet.
 */
public final class LumberjackHandler {
	/** Plafond de buches supplementaires par niveau ; lvl5 = arbre entier. */
	private static final int[] MAX_EXTRA = {6, 12, 24, 48, 4096};

	private LumberjackHandler() {
	}

	public static void register() {
		PlayerBlockBreakEvents.AFTER.register(LumberjackHandler::onBlockBroken);
	}

	private static void onBlockBroken(net.minecraft.world.World world, PlayerEntity player,
			BlockPos origin, BlockState state, BlockEntity blockEntity) {
		if (BulkBreaker.isActive() || !(world instanceof ServerWorld serverWorld)) {
			return;
		}
		if (!state.isIn(BlockTags.LOGS)) {
			return;
		}

		ItemStack tool = player.getMainHandStack();
		int level = EnchantUtil.getLevel(tool, ModEnchantments.LUMBERJACK);
		if (level <= 0) {
			return;
		}

		int maxExtra = MAX_EXTRA[Math.min(level, MAX_EXTRA.length) - 1];
		List<BlockPos> logs = collectLogs(serverWorld, origin, maxExtra);
		BulkBreaker.breakAll(serverWorld, player, origin, tool, logs);
	}

	/** Flood-fill voisinage 26 des blocs du tag LOGS, plafonne a {@code maxExtra}. */
	private static List<BlockPos> collectLogs(ServerWorld world, BlockPos origin, int maxExtra) {
		List<BlockPos> found = new ArrayList<>();
		Set<BlockPos> visited = new HashSet<>();
		visited.add(origin);
		Deque<BlockPos> queue = new ArrayDeque<>();
		queue.add(origin);

		while (!queue.isEmpty() && found.size() < maxExtra) {
			BlockPos current = queue.poll();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) {
							continue;
						}
						if (found.size() >= maxExtra) {
							break;
						}
						BlockPos next = current.add(dx, dy, dz);
						if (!visited.add(next)) {
							continue;
						}
						if (world.getBlockState(next).isIn(BlockTags.LOGS)) {
							found.add(next);
							queue.add(next);
						}
					}
				}
			}
		}
		return found;
	}
}
