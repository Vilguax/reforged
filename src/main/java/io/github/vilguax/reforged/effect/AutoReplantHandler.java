package io.github.vilguax.reforged.effect;

import io.github.vilguax.reforged.ModEnchantments;
import io.github.vilguax.reforged.util.EnchantUtil;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/**
 * Logique custom de l'enchantement Auto-Replant, porte sur une houe.
 *
 * Quand on casse une culture (ble, carottes, pommes de terre, betteraves) avec
 * une houe enchantee, on replante immediatement au meme endroit. Le cout d'une
 * graine est preleve directement sur la recolte, dans une tache differee
 * enregistree AVANT celle de l'Auto-Smelt : ainsi la graine est reservee avant
 * que l'Auto-Smelt ne cuise la recolte (priorite replant).
 *
 * On replante meme une pousse immature : sinon, combinee a l'Auto-Smelt, casser
 * une pousse en croissance cuirait l'unique graine droppee et detruirait la
 * culture. La replanter (et reserver la graine) neutralise cet exploit.
 */
public final class AutoReplantHandler {
	private AutoReplantHandler() {
	}

	public static void register() {
		PlayerBlockBreakEvents.AFTER.register(AutoReplantHandler::onBlockBroken);
	}

	private static void onBlockBroken(net.minecraft.world.World world, PlayerEntity player,
			BlockPos pos, BlockState state, net.minecraft.block.entity.BlockEntity blockEntity) {
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}
		if (!(state.getBlock() instanceof CropBlock crop)) {
			return;
		}

		ItemStack hoe = player.getMainHandStack();
		if (EnchantUtil.getLevel(hoe, ModEnchantments.AUTO_REPLANT) <= 0) {
			return;
		}

		// La graine = l'item de "pick block" de la culture (ble->graines, carotte->carotte...).
		ItemStack seed = state.getPickStack(serverWorld, pos, false);
		if (seed.isEmpty()) {
			return;
		}

		// Replante tout de suite (jeune pousse).
		serverWorld.setBlockState(pos, crop.getDefaultState());

		if (player.isCreative()) {
			return; // pas de cout en creatif
		}

		// Reserve une graine sur la recolte AVANT que l'Auto-Smelt ne la cuise.
		Item seedItem = seed.getItem();
		Box box = new Box(pos).expand(1.5);
		serverWorld.getServer().execute(() -> reserveSeedFromDrops(serverWorld, box, seedItem));
	}

	private static void reserveSeedFromDrops(ServerWorld world, Box box, Item seedItem) {
		for (ItemEntity drop : world.getEntitiesByClass(ItemEntity.class, box,
				io.github.vilguax.reforged.util.DropScan::isFresh)) {
			ItemStack stack = drop.getStack();
			if (stack.isOf(seedItem)) {
				stack.decrement(1);
				if (stack.isEmpty()) {
					drop.discard();
				} else {
					drop.setStack(stack);
				}
				return; // une seule graine reservee
			}
		}
	}
}
