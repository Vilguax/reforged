package io.github.vilguax.reforged.effect;

import io.github.vilguax.reforged.ModEnchantments;
import io.github.vilguax.reforged.util.EnchantUtil;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;

/**
 * Logique custom de l'enchantement Lifesteal (vol de vie).
 *
 * Quand le porteur inflige des degats avec une arme enchantee, il est soigne
 * d'un pourcentage des degats reellement infliges, proportionnel au niveau.
 */
public final class LifestealHandler {
	/** Part des degats rendue en soin sur un coup, par niveau (10% / niveau). */
	private static final float HEAL_PER_LEVEL = 0.10f;

	/** Bonus de soin forfaitaire sur un kill, par niveau (2 PV = 1 coeur / niveau). */
	private static final float KILL_HEAL_PER_LEVEL = 2.0f;

	private LifestealHandler() {
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register(LifestealHandler::onAfterDamage);
		ServerLivingEntityEvents.AFTER_DEATH.register(LifestealHandler::onAfterDeath);
	}

	private static void onAfterDamage(LivingEntity victim, DamageSource source,
			float baseDamageTaken, float damageTaken, boolean blocked) {
		if (blocked || damageTaken <= 0.0f) {
			return;
		}
		if (!(source.getAttacker() instanceof LivingEntity attacker)) {
			return;
		}
		if (attacker.getWorld().isClient()) {
			return;
		}

		ItemStack weapon = attacker.getMainHandStack();
		if (weapon.isEmpty()) {
			return;
		}

		int level = EnchantUtil.getLevel(weapon, ModEnchantments.LIFESTEAL);
		if (level <= 0) {
			return;
		}

		float healAmount = damageTaken * HEAL_PER_LEVEL * level;
		if (healAmount > 0.0f) {
			attacker.heal(healAmount);
		}
	}

	/** Bonus de soin forfaitaire quand l'arme lifesteal acheve une cible. */
	private static void onAfterDeath(LivingEntity victim, DamageSource source) {
		if (!(source.getAttacker() instanceof LivingEntity attacker)) {
			return;
		}
		if (attacker.getWorld().isClient()) {
			return;
		}

		int level = EnchantUtil.getLevel(attacker.getMainHandStack(), ModEnchantments.LIFESTEAL);
		if (level <= 0) {
			return;
		}

		attacker.heal(KILL_HEAL_PER_LEVEL * level);
	}
}
