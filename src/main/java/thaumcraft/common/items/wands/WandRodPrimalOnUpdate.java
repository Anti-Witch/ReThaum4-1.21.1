package thaumcraft.common.items.wands;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.IWandRodOnUpdate;

import java.util.ArrayList;
import java.util.List;

/**
 * WandRodPrimalOnUpdate - TC4 rod regeneration behaviour.
 * <p>
 * Ported from OpenTC4 common/items/wands/WandRodPrimalOnUpdate:
 * - a rod bound to one aspect adds 1 vis of that aspect every 200 ticks while
 *   below {@code maxVis / 10}
 * - the primal staff (no bound aspect) adds 1 vis to a random primal aspect
 *   below the same threshold every 50 ticks
 */
public class WandRodPrimalOnUpdate implements IWandRodOnUpdate {

    private final Aspect aspect;
    private final List<Aspect> primals;

    public WandRodPrimalOnUpdate(Aspect aspect) {
        this.aspect = aspect;
        this.primals = Aspect.getPrimalAspects();
    }

    public WandRodPrimalOnUpdate() {
        this.aspect = null;
        this.primals = Aspect.getPrimalAspects();
    }

    @Override
    public void onUpdate(ItemStack itemstack, Player player) {
        if (!(itemstack.getItem() instanceof WandItem wand)) {
            return;
        }
        float upperBound = wand.getMaxVis(itemstack) / 10.0F;
        if (this.aspect != null) {
            if (player.tickCount % 200 == 0 && wand.getVis(itemstack, this.aspect) < upperBound) {
                wand.storeVis(itemstack, this.aspect, wand.getVis(itemstack, this.aspect) + 1.0F);
            }
        } else if (player.tickCount % 50 == 0) {
            List<Aspect> candidates = new ArrayList<>();
            for (Aspect primal : this.primals) {
                if (wand.getVis(itemstack, primal) < upperBound) {
                    candidates.add(primal);
                }
            }
            if (!candidates.isEmpty()) {
                Aspect chosen = candidates.get(player.level().random.nextInt(candidates.size()));
                wand.storeVis(itemstack, chosen, wand.getVis(itemstack, chosen) + 1.0F);
            }
        }
    }
}
