package io.github.vilguax.reforged.effect;

import io.github.vilguax.reforged.ModEnchantments;
import io.github.vilguax.reforged.util.BulkBreaker;
import io.github.vilguax.reforged.util.EnchantUtil;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

/**
 * Logique custom de l'enchantement Faucheur (Reaper), porte sur une houe.
 *
 * Recolte les cultures mures sur un plan horizontal autour du bloc casse
 * (lvl1 = 3x3, lvl2 = 4x4, lvl3 = 5x5, profondeur 1). Le cassage est delegue a
 * {@link BulkBreaker} (drops regroupes a l'origine), donc le Faucheur compose
 * avec Auto-Smelt / Magnet.
 *
 * Composition avec Auto-Replant : si la houe le porte aussi, chaque culture
 * fauchee est replantee et une graine est reservee sur la recolte AVANT que
 * l'Auto-Smelt ne la cuise (meme priorite que Auto-Replant -> enregistre avant
 * lui). Sans Auto-Replant, c'est une simple faux de zone, sans replantation.
 */
public final class ReaperHandler {
	private ReaperHandler() {
	}

	public static void register() {
		PlayerBlockBreakEvents.AFTER.register(ReaperHandler::onBlockBroken);
	}

	private static void onBlockBroken(net.minecraft.world.World world, PlayerEntity player,
			BlockPos origin, BlockState state, BlockEntity blockEntity) {
		if (BulkBreaker.isActive() || !(world instanceof ServerWorld serverWorld)) {
			return;
		}
		// Ne se declenche que sur une culture (l'origine est geree par Auto-Replant).
		if (!(state.getBlock() instanceof CropBlock)) {
			return;
		}

		ItemStack hoe = player.getMainHandStack();
		int level = EnchantUtil.getLevel(hoe, ModEnchantments.REAPER);
		if (level <= 0) {
			return;
		}

		int size = level + 2; // lvl1=3, lvl2=4, lvl3=5
		int lo = -(size - 1) / 2;
		int hi = size / 2;
		boolean replant = EnchantUtil.getLevel(hoe, ModEnchantments.AUTO_REPLANT) > 0;

		List<BlockPos> crops = new ArrayList<>();
		List<CropBlock> cropBlocks = new ArrayList<>();
		List<Item> seeds = new ArrayList<>();

		for (int dx = lo; dx <= hi; dx++) {
			for (int dz = lo; dz <= hi; dz++) {
				if (dx == 0 && dz == 0) {
					continue; // origine deja recoltee/replantee par Auto-Replant
				}
				BlockPos pos = origin.add(dx, 0, dz);
				BlockState cropState = serverWorld.getBlockState(pos);
				if (cropState.getBlock() instanceof CropBlock crop && crop.isMature(cropState)) {
					crops.add(pos);
					if (replant) {
						cropBlocks.add(crop);
						seeds.add(cropState.getPickStack(serverWorld, pos, false).getItem());
					}
				}
			}
		}

		if (crops.isEmpty()) {
			return;
		}

		// Casse + regroupe les drops a l'origine + use la houe.
		BulkBreaker.breakAll(serverWorld, player, origin, hoe, crops);

		if (!replant) {
			return;
		}

		// Replante chaque culture fauchee a sa position.
		for (int i = 0; i < crops.size(); i++) {
			serverWorld.setBlockState(crops.get(i), cropBlocks.get(i).getDefaultState());
		}

		if (player.isCreative()) {
			return; // pas de cout en creatif
		}

		// Reserve une graine par replant sur la recolte regroupee a l'origine,
		// avant qu'Auto-Smelt ne la cuise (tache differee, fin de tick).
		Box box = new Box(origin).expand(1.5);
		List<Item> toReserve = new ArrayList<>(seeds);
		serverWorld.getServer().execute(() -> reserveSeeds(serverWorld, box, toReserve));
	}

	private static void reserveSeeds(ServerWorld world, Box box, List<Item> seeds) {
		List<ItemEntity> drops = world.getEntitiesByClass(ItemEntity.class, box,
				io.github.vilguax.reforged.util.DropScan::isFresh);
		for (Item seedItem : seeds) {
			for (ItemEntity drop : drops) {
				ItemStack stack = drop.getStack();
				if (!stack.isEmpty() && stack.isOf(seedItem)) {
					stack.decrement(1);
					if (stack.isEmpty()) {
						drop.discard();
					} else {
						drop.setStack(stack);
					}
					break; // une graine reservee pour cette culture
				}
			}
		}
	}
}
