package io.github.vilguax.reforged.effect;

import io.github.vilguax.reforged.ModEnchantments;
import io.github.vilguax.reforged.ModTags;
import io.github.vilguax.reforged.util.EnchantUtil;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Logique custom de l'enchantement Vein Miner.
 *
 * Quand on casse un minerai (tag {@link ModTags#VEIN_MINEABLE}) avec un outil
 * enchante, on casse aussi jusqu'a {@code niveau} blocs supplementaires du
 * meme minerai connectes (voisinage 26). La durabilite consommee par les blocs
 * en plus est degressive (1/2, 1/4, 1/8...), donc plus la veine est grosse,
 * moins ca coute par bloc. Les drops respectent Fortune via Block.dropStacks.
 */
public final class VeinMinerHandler {
	/** Garde anti-reentrance (nos cassages ne doivent pas se re-declencher). */
	private static boolean active = false;

	private VeinMinerHandler() {
	}

	public static void register() {
		PlayerBlockBreakEvents.AFTER.register(VeinMinerHandler::onBlockBroken);
	}

	private static void onBlockBroken(net.minecraft.world.World world, PlayerEntity player,
			BlockPos origin, BlockState state, BlockEntity blockEntity) {
		if (active || !(world instanceof ServerWorld serverWorld)) {
			return;
		}
		if (!state.isIn(ModTags.VEIN_MINEABLE)) {
			return;
		}

		ItemStack tool = player.getMainHandStack();
		int level = EnchantUtil.getLevel(tool, ModEnchantments.VEIN_MINER);
		if (level <= 0) {
			return;
		}

		// Quantite totale cassee = 2^niveau (lvl1=2, lvl2=4, ... lvl5=32).
		// Le bloc d'origine est deja casse par vanilla -> on en casse 2^niveau - 1 de plus.
		int maxExtras = (1 << level) - 1;
		List<BlockPos> vein = collectVein(serverWorld, origin, state.getBlock(), maxExtras);
		if (vein.isEmpty()) {
			return;
		}

		active = true;
		try {
			for (BlockPos pos : vein) {
				BlockState targetState = serverWorld.getBlockState(pos);
				if (!targetState.isOf(state.getBlock())) {
					continue;
				}
				BlockEntity targetBe = serverWorld.getBlockEntity(pos);
				// Drops regroupes a la position d'origine : ainsi ils tombent dans la
				// zone scannee par Auto-Smelt / Magnet et composent avec eux, quelle
				// que soit la distance dans la veine. Fortune appliquee via l'outil.
				for (ItemStack drop : Block.getDroppedStacks(targetState, serverWorld, pos, targetBe, player, tool)) {
					Block.dropStack(serverWorld, origin, drop);
				}
				// XP du minerai laissee sur place (les orbes se collectent seuls).
				targetState.onStacksDropped(serverWorld, pos, tool, true);
				serverWorld.breakBlock(pos, false, player);
			}
		} finally {
			active = false;
		}

		applyDiminishingDurability(tool, player, vein.size());
	}

	/** Flood-fill voisinage 26, meme bloc, dans le tag, plafonne a {@code maxExtras} blocs. */
	private static List<BlockPos> collectVein(ServerWorld world, BlockPos origin, Block oreBlock, int maxExtras) {
		List<BlockPos> found = new ArrayList<>();
		Set<BlockPos> visited = new HashSet<>();
		visited.add(origin);
		Deque<BlockPos> queue = new ArrayDeque<>();
		queue.add(origin);

		while (!queue.isEmpty() && found.size() < maxExtras) {
			BlockPos current = queue.poll();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) {
							continue;
						}
						if (found.size() >= maxExtras) {
							break;
						}
						BlockPos next = current.add(dx, dy, dz);
						if (!visited.add(next)) {
							continue;
						}
						BlockState ns = world.getBlockState(next);
						if (ns.isOf(oreBlock) && ns.isIn(ModTags.VEIN_MINEABLE)) {
							found.add(next);
							queue.add(next);
						}
					}
				}
			}
		}
		return found;
	}

	/**
	 * Durabilite degressive pour les blocs supplementaires : le n-ieme extra
	 * coute 1/2^n point de durabilite. La somme converge vers ~1, donc une
	 * grosse veine use tres peu l'outil.
	 */
	private static void applyDiminishingDurability(ItemStack tool, PlayerEntity player, int extraBlocks) {
		if (player.isCreative() || extraBlocks <= 0 || !tool.isDamageable()) {
			return;
		}
		double cost = 0.0;
		for (int i = 1; i <= extraBlocks; i++) {
			cost += Math.pow(0.5, i);
		}
		int damage = (int) Math.round(cost);
		if (damage > 0) {
			tool.damage(damage, player, EquipmentSlot.MAINHAND);
		}
	}
}
