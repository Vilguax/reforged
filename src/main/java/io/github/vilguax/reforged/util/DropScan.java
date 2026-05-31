package io.github.vilguax.reforged.util;

import net.minecraft.entity.ItemEntity;

/**
 * Detection des drops "frais" pour le pipeline de minage (Auto-Replant,
 * Auto-Smelt, Magnet, Faucheur).
 *
 * Ces handlers reagissent a une casse en differant a la fin du tick
 * ({@code server.execute}) puis en scannant les ItemEntity apparus. On ne peut
 * pas filtrer sur {@code age == 0} : selon la charge serveur la tache differee
 * s'execute le meme tick (age 0) ou un a deux ticks plus tard (age >= 1), ce qui
 * faisait rater les drops par intermittence. On tolere donc une courte fenetre :
 * assez large pour absorber ce decalage, assez courte pour ne jamais aspirer des
 * objets qui trainaient deja au sol.
 */
public final class DropScan {
	/** Age max (en ticks) d'un drop considere comme issu de la casse courante. */
	public static final int FRESH_MAX_AGE = 5;

	private DropScan() {
	}

	public static boolean isFresh(ItemEntity entity) {
		return entity.getItemAge() <= FRESH_MAX_AGE;
	}
}
