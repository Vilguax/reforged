package io.github.vilguax.reforged.effect;

import io.github.vilguax.reforged.ModEnchantments;
import io.github.vilguax.reforged.util.DropScan;
import io.github.vilguax.reforged.util.EnchantUtil;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.Optional;

/**
 * Logique custom de l'enchantement Auto-Smelt.
 *
 * Apres qu'un bloc est casse avec un outil enchante, on transmute les drops
 * fraichement apparus en leur version fondue (raw iron -> lingot, sable ->
 * verre, ...) et on lache l'XP de fonte correspondante.
 */
public final class AutoSmeltHandler {
	private AutoSmeltHandler() {
	}

	public static void register() {
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (!(world instanceof ServerWorld serverWorld)) {
				return;
			}
			ItemStack tool = player.getMainHandStack();
			if (EnchantUtil.getLevel(tool, ModEnchantments.AUTO_SMELT) <= 0) {
				return;
			}

			// Les drops du bloc ne sont pas encore spawnes au moment de AFTER :
			// on differe a la fin du tick courant, ou ils existent (age == 0).
			Box box = new Box(pos).expand(1.5);
			serverWorld.getServer().execute(() -> {
				List<ItemEntity> drops = serverWorld.getEntitiesByClass(ItemEntity.class, box,
						DropScan::isFresh);
				for (ItemEntity drop : drops) {
					smeltDrop(serverWorld, drop);
				}
			});
		});
	}

	private static void smeltDrop(ServerWorld world, ItemEntity drop) {
		ItemStack input = drop.getStack();
		if (input.isEmpty()) {
			return;
		}

		SingleStackRecipeInput recipeInput = new SingleStackRecipeInput(input);
		Optional<RecipeEntry<SmeltingRecipe>> match =
				world.getServer().getRecipeManager().getFirstMatch(RecipeType.SMELTING, recipeInput, world);
		if (match.isEmpty()) {
			return;
		}

		SmeltingRecipe recipe = match.get().value();
		ItemStack result = recipe.craft(recipeInput, world.getRegistryManager());
		if (result.isEmpty()) {
			return;
		}

		int count = input.getCount();
		drop.setStack(result.copyWithCount(result.getCount() * count));

		float xpPerItem = recipe.getExperience();
		if (xpPerItem > 0.0f) {
			int xp = Math.round(xpPerItem * count);
			if (xp > 0) {
				ExperienceOrbEntity.spawn(world, drop.getPos(), xp);
			}
		}
	}
}
