package io.github.vilguax.reforged.effect;

import io.github.vilguax.reforged.ModEnchantments;
import io.github.vilguax.reforged.util.DropScan;
import io.github.vilguax.reforged.util.EnchantUtil;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

/**
 * Logique custom de l'enchantement Magnet (aimant), porte sur une armure.
 *
 * Ce qu'on casse n'est pas laisse au sol : les drops frais sont envoyes
 * directement dans l'inventaire. Le loot vanilla (Fortune, etc.) est deja
 * calcule a ce stade ; on se contente de rediriger les ItemEntity. Si
 * l'inventaire est plein, le surplus reste au sol.
 *
 * Comme Auto-Smelt, on differe a la fin du tick (les drops n'existent pas
 * encore pendant AFTER) et on cible les drops frais (age == 0). L'enregistrement
 * apres Auto-Smelt garantit que les items sont d'abord fondus, puis ramasses.
 */
public final class MagnetHandler {
	private static final EquipmentSlot[] ARMOR_SLOTS = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	private MagnetHandler() {
	}

	public static void register() {
		// Minage : aspire les drops du bloc casse (compose avec Auto-Smelt / Vein).
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (!(world instanceof ServerWorld serverWorld) || !hasMagnet(player)) {
				return;
			}
			vacuum(serverWorld, player, new Box(pos).expand(1.5));
		});

		// Kills : aspire le loot d'un mob tue par un joueur equipe de Magnet.
		ServerLivingEntityEvents.AFTER_DEATH.register((victim, source) -> {
			if (!(victim.getWorld() instanceof ServerWorld serverWorld)) {
				return;
			}
			if (!(source.getAttacker() instanceof PlayerEntity player) || !hasMagnet(player)) {
				return;
			}
			vacuum(serverWorld, player, victim.getBoundingBox().expand(2.0));
		});
	}

	/** Differe a la fin du tick (les drops n'existent pas encore) puis ramasse les drops frais. */
	private static void vacuum(ServerWorld world, PlayerEntity player, Box box) {
		world.getServer().execute(() -> {
			for (ItemEntity drop : world.getEntitiesByClass(ItemEntity.class, box, DropScan::isFresh)) {
				collect(player, drop);
			}
		});
	}

	private static boolean hasMagnet(PlayerEntity player) {
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			if (EnchantUtil.getLevel(player.getEquippedStack(slot), ModEnchantments.MAGNET) > 0) {
				return true;
			}
		}
		return false;
	}

	private static void collect(PlayerEntity player, ItemEntity drop) {
		ItemStack stack = drop.getStack();
		if (stack.isEmpty()) {
			return;
		}

		int before = stack.getCount();
		player.getInventory().insertStack(stack); // reduit le count de la pile en place
		int taken = before - stack.getCount();
		if (taken > 0) {
			// Animation/son de ramassage vanilla.
			player.sendPickup(drop, taken);
		}

		if (stack.isEmpty()) {
			drop.discard();
		} else {
			drop.setStack(stack); // surplus laisse au sol
		}
	}
}
