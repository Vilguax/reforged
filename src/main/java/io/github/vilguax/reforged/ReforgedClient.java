package io.github.vilguax.reforged;

import io.github.vilguax.reforged.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;

/** Init cote client : rendu des entites custom. */
@Environment(EnvType.CLIENT)
public final class ReforgedClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// L'epee Blink en vol est rendue comme un item flottant (le stack porte par l'entite).
		EntityRendererRegistry.register(ModEntities.BLINK_SWORD, FlyingItemEntityRenderer::new);
	}
}
