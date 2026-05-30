package io.github.vilguax.reforged.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Projectile de l'enchantement Blink.
 *
 * Lance comme une ender pearl mais purement cosmetique : l'epee reste en main du
 * joueur (rien n'est retire de l'inventaire), le stack porte ici ne sert qu'au
 * rendu en vol. A l'impact (bloc ou entite), on teleporte le lanceur au point de
 * chute, sans aucun cout (pas de degats, pas d'XP).
 */
public class BlinkSwordEntity extends ThrownItemEntity {

	public BlinkSwordEntity(EntityType<? extends BlinkSwordEntity> entityType, World world) {
		super(entityType, world);
	}

	/** Lancer : {@code stack} est l'epee copiee, utilisee uniquement pour le rendu en vol. */
	public BlinkSwordEntity(World world, LivingEntity owner, ItemStack stack) {
		super(ModEntities.BLINK_SWORD, owner, world, stack);
	}

	/** Item de repli pour le rendu si l'entite est reconstruite sans stack (sauvegarde/spawn). */
	@Override
	protected Item getDefaultItem() {
		return Items.IRON_SWORD;
	}

	@Override
	protected void onCollision(HitResult hitResult) {
		super.onCollision(hitResult);
		if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
			return;
		}

		if (this.getOwner() instanceof ServerPlayerEntity player
				&& player.isAlive() && player.getWorld() == serverWorld) {
			Vec3d target = this.getPos();
			serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			player.networkHandler.requestTeleport(target.x, target.y, target.z,
					player.getYaw(), player.getPitch());
			player.onLanding();
			serverWorld.playSound(null, target.x, target.y, target.z,
					SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
		}

		this.discard();
	}
}
