package io.github.vilguax.reforged.entity;

import io.github.vilguax.reforged.Reforged;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/** Types d'entites custom du mod. */
public final class ModEntities {
	/** Projectile cosmetique de l'enchantement Blink (rendu = l'epee lancee). */
	public static final EntityType<BlinkSwordEntity> BLINK_SWORD = register("blink_sword",
			EntityType.Builder.<BlinkSwordEntity>create(BlinkSwordEntity::new, SpawnGroup.MISC)
					.dimensions(0.25f, 0.25f)
					.maxTrackingRange(4)
					.trackingTickInterval(10));

	private ModEntities() {
	}

	/** Force le chargement de la classe : les EntityType sont enregistres dans l'init statique. */
	public static void register() {
	}

	private static EntityType<BlinkSwordEntity> register(String name, EntityType.Builder<BlinkSwordEntity> builder) {
		RegistryKey<EntityType<?>> key = RegistryKey.of(RegistryKeys.ENTITY_TYPE,
				Identifier.of(Reforged.MOD_ID, name));
		return Registry.register(Registries.ENTITY_TYPE, key, builder.build(key));
	}
}
