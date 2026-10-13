package thaumcraft.common.items.armor;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.items.baubles.ItemGirdleHover;
import thaumcraft.common.lib.compat.CuriosCompat;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.misc.PacketFlyToServer;
import thaumcraft.init.ModSounds;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TC4 {@code thaumcraft.common.items.armor.Hover}.
 *
 * The Thaumostatic Harness holds a jar of Potentia (TC4 {@code Aspect.ENERGY} =
 * "potentia") and, while activated with the hover key, lets the wearer fly like
 * in creative mode. The jar's contents are consumed one point every
 * {@link #EFFICIENCY} ticks; wearing a Thaumostatic Girdle speeds the wearer up
 * but also consumes fuel 20% faster.
 *
 * Port notes (1.21.1):
 * <ul>
 *   <li>The port has no filled-jar item (warded jars are block entities), so the
 *       harness stores its own Potentia reserve in {@code CustomData} under the
 *       TC4-style "jar" key. It is filled/emptied by clicking a warded jar
 *       ({@code TileJar}) or a filled Potentia phial.</li>
 *   <li>TC4's Haste enchantment is not present in the port yet, so the speed
 *       bonus from it is omitted.</li>
 *   <li>Flight is granted through {@link net.minecraft.world.entity.player.PlayerAbilities}
 *       (mayfly/flying) because 1.21.1 has no direct "capabilities" field.</li>
 * </ul>
 */
public class Hover {

    /** TC4: one Potentia is consumed every 360 ticks of flight. */
    public static final int EFFICIENCY = 360;

    /** Warded jar capacity (TC4 ItemJarFilled max). */
    public static final int JAR_CAPACITY = 250;

    /** TC4: while hovering, horizontal motion is multiplied by 0.7 (+0.075*haste, +0.21 with the girdle). */
    private static final float HOVER_SPEED = 0.7F;

    /** TC4: the girdle makes the harness use 20% more fuel but fly faster. */
    private static final float GIRDLE_FUEL_MOD = 0.8F;
    private static final float GIRDLE_SPEED_BONUS = 0.21F;

    /** TC4 kept these maps keyed by player entity id. */
    private static final Map<UUID, Boolean> hovering = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> timing = new ConcurrentHashMap<>();
    /** Players whose flight abilities were granted by the harness (to restore them safely). */
    private static final Set<UUID> flightGranted = ConcurrentHashMap.newKeySet();

    // ==================== Hover state ====================

    public static boolean getHover(Player player) {
        return hovering.getOrDefault(player.getUUID(), false);
    }

    public static void setHover(UUID playerId, boolean hover) {
        hovering.put(playerId, hover);
    }

    /**
     * TC4 Hover.toggleHover - toggles the hover state of a player wearing the harness.
     * Returns the new state (false if there is no fuel to start hovering).
     */
    public static boolean toggleHover(Player player, ItemStack armor) {
        boolean hover = getHover(player);
        if (!hover && getFuel(armor) <= 0) {
            return false;
        }

        hovering.put(player.getUUID(), !hover);
        if (player.level().isClientSide) {
            PacketHandler.sendToServer(new PacketFlyToServer(!hover));
            player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                    (!hover ? ModSounds.HH_ON.get() : ModSounds.HH_OFF.get()),
                    SoundSource.PLAYERS, 0.33F, 1.0F, false);
        }
        return !hover;
    }

    /**
     * TC4 Hover.handleHoverArmor - called every tick from the harness while worn.
     * Runs on both sides (the server keeps the abilities/fall damage in check,
     * the client applies the hover speed modifier).
     */
    public static void handleHoverArmor(Player player, ItemStack armor) {
        // TC4 ItemHoverHarness.onArmorTick skips creative players entirely
        if (player.getAbilities().instabuild || player.isSpectator()) {
            return;
        }

        UUID id = player.getUUID();

        // Restore the persisted state after a relog (TC4 read the "hover" byte from NBT)
        if (!hovering.containsKey(id)) {
            boolean stored = false;
            CustomData cd = armor.get(DataComponents.CUSTOM_DATA);
            if (cd != null && cd.copyTag().contains("hover")) {
                stored = cd.copyTag().getBoolean("hover");
            }
            hovering.put(id, stored);
            if (stored && player.level().isClientSide) {
                PacketHandler.sendToServer(new PacketFlyToServer(true));
            }
        }

        boolean hover = getHover(player);
        boolean client = player.level().isClientSide;

        if (hover && expendCharge(player, armor)) {
            if (client) {
                // TC4 client branch: creative-like flight + hover speed modifier
                player.getAbilities().flying = true;
                // 1.21.1 disables flight while standing on the ground (LocalPlayer.aiStep),
                // so give a small lift-off impulse like TC4's take-off
                if (player.onGround()) {
                    Vec3 motion = player.getDeltaMovement();
                    if (motion.y < 0.12D) {
                        player.setDeltaMovement(motion.x, 0.12D, motion.z);
                        player.hasImpulse = true;
                    }
                }
                applyHoverSpeed(player);
                playHoverSound(player);
            } else {
                // TC4 server branch: no fall damage while hovering
                player.fallDistance = 0.0F;
                grantServerFlight(player);
                setStoredHover(armor, true);
            }
        } else {
            if (hover) {
                toggleHover(player, armor);
            }
            if (client) {
                // Stop flying when the fuel ran out or the player switched hover off
                if (player.getAbilities().flying) {
                    player.getAbilities().flying = false;
                }
            } else {
                // TC4: a worn but inactive harness still softens the fall
                player.fallDistance *= 0.75F;
                restoreServerFlight(player);
                setStoredHover(armor, false);
            }
        }
    }

    /**
     * Called when the harness is no longer worn: drop the hover state and give
     * back the flight abilities we granted.
     */
    public static void onHarnessRemoved(Player player) {
        hovering.remove(player.getUUID());
        if (!player.level().isClientSide) {
            restoreServerFlight(player);
        }
    }

    // ==================== Fuel (TC4 jar) ====================

    /**
     * Potentia stored in the harness (0..{@link #JAR_CAPACITY}).
     * Stored in the TC4 "jar" slot as an AspectList so a real filled-jar item
     * could replace it later without changing the consumers.
     */
    public static int getFuel(ItemStack armor) {
        CustomData cd = armor.get(DataComponents.CUSTOM_DATA);
        if (cd == null) {
            return 0;
        }
        CompoundTag tag = cd.copyTag();
        if (!tag.contains("jar")) {
            return 0;
        }
        AspectList aspects = new AspectList();
        aspects.readFromNBT(tag.getCompound("jar"));
        return aspects.getAmount(Aspect.ENERGY);
    }

    public static void setFuel(ItemStack armor, int amount) {
        amount = Mth.clamp(amount, 0, JAR_CAPACITY);
        final int value = amount;
        CustomData.update(DataComponents.CUSTOM_DATA, armor, tag -> {
            CompoundTag jar = new CompoundTag();
            AspectList aspects = new AspectList();
            if (value > 0) {
                aspects.add(Aspect.ENERGY, value);
            }
            aspects.writeToNBT(jar);
            tag.put("jar", jar);
        });
    }

    /** Adds Potentia to the harness, returns the amount actually added. */
    public static int addFuel(ItemStack armor, int amount) {
        int fuel = getFuel(armor);
        int added = Math.min(amount, JAR_CAPACITY - fuel);
        if (added > 0) {
            setFuel(armor, fuel + added);
        }
        return added;
    }

    /** TC4 Hover.expendCharge: 1 Potentia per {@link #EFFICIENCY} ticks (288 with the girdle). */
    public static boolean expendCharge(Player player, ItemStack armor) {
        int fuel = getFuel(armor);
        if (fuel <= 0) {
            return false;
        }

        float mod = hasHoverGirdle(player) ? GIRDLE_FUEL_MOD : 1.0F;
        int charge = getCharge(armor);
        if ((float) charge < EFFICIENCY * mod) {
            setCharge(armor, charge + 1);
            return true;
        }

        setCharge(armor, 0);
        setFuel(armor, fuel - 1);
        return fuel - 1 > 0;
    }

    // ==================== Helpers ====================

    private static int getCharge(ItemStack armor) {
        CustomData cd = armor.get(DataComponents.CUSTOM_DATA);
        return cd == null ? 0 : cd.copyTag().getInt("charge");
    }

    private static void setCharge(ItemStack armor, int charge) {
        CustomData.update(DataComponents.CUSTOM_DATA, armor, tag -> tag.putInt("charge", charge));
    }

    private static void setStoredHover(ItemStack armor, boolean hover) {
        CustomData.update(DataComponents.CUSTOM_DATA, armor, tag -> tag.putBoolean("hover", hover));
    }

    /** TC4 checks the belt bauble slot for the Thaumostatic Girdle. */
    private static boolean hasHoverGirdle(Player player) {
        final boolean[] found = {false};
        CuriosCompat.forEachCuriosItemWithSlot(player, (slot, stack) -> {
            if (!found[0] && slot.startsWith("belt") && stack.getItem() instanceof ItemGirdleHover) {
                found[0] = true;
            }
        });
        return found[0];
    }

    /** TC4 client-side movement modifier while hovering. */
    private static void applyHoverSpeed(Player player) {
        float mod = HOVER_SPEED;
        if (hasHoverGirdle(player)) {
            mod += GIRDLE_SPEED_BONUS;
        }
        if (mod > 1.0F) {
            mod = 1.0F;
        }
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x * mod, motion.y, motion.z * mod);
    }

    /** TC4 played the "jacobs" ambience every 1200ms while hovering. */
    private static void playHoverSound(Player player) {
        long currentTime = System.currentTimeMillis();
        Long next = timing.get(player.getUUID());
        if (next == null || next < currentTime) {
            timing.put(player.getUUID(), currentTime + 1200L);
            player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                    ModSounds.JACOBS.get(), SoundSource.PLAYERS,
                    0.05F, 1.0F + player.level().random.nextFloat() * 0.05F, false);
        }
    }

    /** Grant creative-style flight so the server accepts the client's movement. */
    private static void grantServerFlight(Player player) {
        var abilities = player.getAbilities();
        boolean changed = false;
        if (!abilities.mayfly) {
            abilities.mayfly = true;
            changed = true;
        }
        if (!abilities.flying) {
            abilities.flying = true;
            changed = true;
        }
        if (changed) {
            player.onUpdateAbilities();
        }
        flightGranted.add(player.getUUID());
    }

    /** Restore the vanilla flight abilities once hovering stops (creative/spectator untouched). */
    private static void restoreServerFlight(Player player) {
        if (!flightGranted.remove(player.getUUID())) {
            return;
        }
        if (player.getAbilities().instabuild || player.isSpectator()) {
            return;
        }
        var abilities = player.getAbilities();
        abilities.mayfly = false;
        abilities.flying = false;
        player.onUpdateAbilities();
    }
}
