package thaumcraft.common.lib.events;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.common.items.armor.ItemFortressArmor;

import java.util.ArrayList;
import java.util.List;

/**
 * TC4 ItemFortressArmor implemented {@code ISpecialArmor}: every fortress piece
 * absorbed damage using its own ratio instead of the vanilla armor formula.
 *
 * <ul>
 *   <li>normal damage: defense / 25</li>
 *   <li>magic damage: defense / 35</li>
 *   <li>fire &amp; explosions: defense / 20</li>
 *   <li>unblockable damage: no special absorption</li>
 * </ul>
 *
 * The ratio of every piece is multiplied by the set factor
 * ({@link ItemFortressArmor#getSetBonus}) so a full set (and especially one
 * with a faceplate) protects far better than the sum of its parts. With all
 * three pieces and a mask the fire ratio reaches 110%, i.e. the wearer is
 * effectively immune to fire/explosions until the armor is used up - exactly
 * like TC4.
 *
 * 1.21.1 has no ISpecialArmor, so the difference between the TC4 ratio and the
 * vanilla armor reduction (which is still applied afterwards) is added here.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, bus = EventBusSubscriber.Bus.GAME)
public class FortressArmorEvents {

    private static final EquipmentSlot[] FORTRESS_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS
    };

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        DamageSource source = event.getSource();
        // TC4: source.isUnblockable() -> ratio 0
        if (source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return;
        }
        float amount = event.getAmount();
        if (amount <= 0.0F) {
            return;
        }

        // TC4 divisor: magic /35, fire & explosion /20, everything else /25
        double divisor = 25.0D;
        if (source.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            divisor = 35.0D;
        } else if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION)) {
            divisor = 20.0D;
        }

        double set = ItemFortressArmor.getSetBonus(player);
        double tc4Ratio = 0.0D;
        List<ItemStack> pieces = new ArrayList<>();
        List<EquipmentSlot> pieceSlots = new ArrayList<>();
        for (EquipmentSlot slot : FORTRESS_SLOTS) {
            ItemStack piece = player.getItemBySlot(slot);
            if (piece.getItem() instanceof ItemFortressArmor fortress) {
                tc4Ratio += fortress.getDefense() / divisor * set;
                pieces.add(piece);
                pieceSlots.add(slot);
            }
        }
        if (tc4Ratio <= 0.0D || pieces.isEmpty()) {
            return;
        }

        // The vanilla armor formula still runs after this event; only apply the
        // part of the TC4 ratio that vanilla does not already cover.
        float armor = player.getArmorValue();
        float toughness = (float) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        double vanillaRatio = 1.0D - net.minecraft.world.damagesource.CombatRules
                .getDamageAfterAbsorb(player, amount, source, armor, toughness) / amount;
        if (tc4Ratio <= vanillaRatio) {
            return;
        }

        double target = amount * (1.0D - tc4Ratio);
        double remaining = Math.max(0.05D, 1.0D - vanillaRatio);
        event.setAmount((float) Math.max(0.0D, target / remaining));

        // TC4 damageArmor: the armor wears down by the absorbed damage
        // (fall damage never damages fortress armor).
        if (!source.is(DamageTypeTags.IS_FALL)) {
            float absorbed = amount - (float) target;
            int perPiece = Math.max(1, Math.round(absorbed / pieces.size()));
            for (int i = 0; i < pieces.size(); i++) {
                pieces.get(i).hurtAndBreak(perPiece, player, pieceSlots.get(i));
            }
        }
    }
}
