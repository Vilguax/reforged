package io.github.vilguax.reforged;

import io.github.vilguax.reforged.effect.AutoReplantHandler;
import io.github.vilguax.reforged.effect.AutoSmeltHandler;
import io.github.vilguax.reforged.effect.BlinkHandler;
import io.github.vilguax.reforged.effect.LifestealHandler;
import io.github.vilguax.reforged.effect.MagnetHandler;
import io.github.vilguax.reforged.effect.VeinMinerHandler;
import io.github.vilguax.reforged.entity.ModEntities;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Reforged implements ModInitializer {
	public static final String MOD_ID = "reforged";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[Reforged] Loading custom enchantments");

		// Les enchantements eux-memes sont definis en data (data/reforged/enchantment/*.json).
		// Ici on branche uniquement la logique custom Java.
		ModEntities.register();

		LifestealHandler.register();
		BlinkHandler.register();

		// L'ordre compte : les handlers de minage planifient des taches differees
		// (fin de tick) executees dans l'ordre d'enregistrement. Pipeline voulu :
		//   Auto-Replant (reserve 1 graine) -> Auto-Smelt (fond le reste) -> Magnet (ramasse).
		AutoReplantHandler.register();
		AutoSmeltHandler.register();
		VeinMinerHandler.register();
		MagnetHandler.register();
	}
}
