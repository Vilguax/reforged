package io.github.vilguax.reforged.effect;

import io.github.vilguax.reforged.ModEnchantments;
import io.github.vilguax.reforged.entity.BlinkSwordEntity;
import io.github.vilguax.reforged.util.EnchantUtil;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * Logique custom de l'enchantement Blink, porte sur une epee.
 *
 * Clic droit : projette une epee fantome (cosmetique) comme une ender pearl.
 * L'epee reste en main et reste utilisable pendant le vol. Au point d'impact, le
 * joueur est teleporte (cf. {@link BlinkSwordEntity}). Un court cooldown evite le
 * spam ; mettre COOLDOWN_TICKS a 0 pour le desactiver.
 */
public final class BlinkHandler {
	/** Cooldown anti-spam en ticks (30 = 1,5 s). Mettre a 0 pour spam libre. */
	private static final int COOLDOWN_TICKS = 30;

	/** Vitesse de lancer (1.5 = comme une ender pearl). */
	private static final float THROW_SPEED = 1.5f;

	/** Cout en durabilite par lancer, comme un coup d'epee (Solidite/Unbreaking respecte par damage()). */
	private static final int DURABILITY_COST = 1;

	private BlinkHandler() {
	}

	public static void register() {
		UseItemCallback.EVENT.register(BlinkHandler::onUse);
	}

	private static ActionResult onUse(PlayerEntity player, World world, Hand hand) {
		if (hand != Hand.MAIN_HAND) {
			return ActionResult.PASS;
		}

		ItemStack stack = player.getStackInHand(hand);
		if (EnchantUtil.getLevel(stack, ModEnchantments.BLINK) <= 0) {
			return ActionResult.PASS;
		}
		if (player.getItemCooldownManager().isCoolingDown(stack)) {
			return ActionResult.PASS;
		}

		if (!world.isClient) {
			// copie : l'epee reste en main, ce stack ne sert qu'au rendu en vol
			BlinkSwordEntity blink = new BlinkSwordEntity(world, player, stack.copy());
			blink.setVelocity(player, player.getPitch(), player.getYaw(), 0.0f, THROW_SPEED, 1.0f);
			world.spawnEntity(blink);
			world.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.ENTITY_ENDER_PEARL_THROW, SoundCategory.PLAYERS, 0.5f, 1.0f);

			// Cout durabilite comme un coup d'epee. damage() applique Solidite et gere la casse ;
			// ignore en creatif. On entame le stack en main (pas la copie portee par l'entite).
			if (!player.isCreative()) {
				stack.damage(DURABILITY_COST, player, EquipmentSlot.MAINHAND);
			}
		}

		if (COOLDOWN_TICKS > 0) {
			player.getItemCooldownManager().set(stack, COOLDOWN_TICKS);
		}
		player.swingHand(hand);
		return ActionResult.SUCCESS;
	}
}
