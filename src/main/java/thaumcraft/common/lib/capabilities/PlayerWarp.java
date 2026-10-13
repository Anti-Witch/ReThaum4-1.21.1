package thaumcraft.common.lib.capabilities;

import javax.annotation.Nonnull;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.Thaumcraft;
import thaumcraft.api.capabilities.IPlayerWarp;

/**
 * PlayerWarp - Implementation of the IPlayerWarp capability.
 * Tracks warp (corruption) levels for a player.
 * 
 * @author Azanor
 * Ported to NeoForge 1.21.1
 */
public class PlayerWarp {
    
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "warp");
    
    /**
     * Default implementation of IPlayerWarp
     */
    public static class DefaultImpl implements IPlayerWarp {
        
        private int permanentWarp = 0;
        private int normalWarp = 0;
        private int temporaryWarp = 0;
        private int counter = 0;
        
        @Override
        public void clear() {
            permanentWarp = 0;
            normalWarp = 0;
            temporaryWarp = 0;
            counter = 0;
        }
        
        @Override
        public int get(@Nonnull EnumWarpType type) {
            return switch (type) {
                case PERMANENT -> permanentWarp;
                case NORMAL -> normalWarp;
                case TEMPORARY -> temporaryWarp;
            };
        }
        
        @Override
        public void set(@Nonnull EnumWarpType type, int amount) {
            amount = Math.max(0, amount);
            switch (type) {
                case PERMANENT -> permanentWarp = amount;
                case NORMAL -> normalWarp = amount;
                case TEMPORARY -> temporaryWarp = amount;
            }
        }
        
        @Override
        public int add(@Nonnull EnumWarpType type, int amount) {
            int current = get(type);
            int newValue = Math.max(0, current + amount);
            set(type, newValue);
            return newValue;
        }
        
        @Override
        public int getTotalWarp() {
            return permanentWarp + normalWarp + temporaryWarp;
        }
        
        @Override
        public int getPermanentWarp() {
            return permanentWarp + normalWarp;
        }
        
        @Override
        public boolean shouldTick() {
            return temporaryWarp > 0;
        }
        
        @Override
        public int getCounter() {
            return counter;
        }
        
        @Override
        public void setCounter(int count) {
            this.counter = count;
        }
        
        @Override
        public void sync(@Nonnull ServerPlayer player) {
            thaumcraft.common.lib.network.PacketHandler.sendToPlayer(
                new thaumcraft.common.lib.network.playerdata.PacketSyncWarp(player), 
                player
            );
        }
        
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("perm", permanentWarp);
            tag.putInt("norm", normalWarp);
            tag.putInt("temp", temporaryWarp);
            tag.putInt("counter", counter);
            return tag;
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            return serializeNBT();
        }
        
        public void deserializeNBT(CompoundTag tag) {
            if (tag == null) {
                return;
            }
            permanentWarp = tag.getInt("perm");
            normalWarp = tag.getInt("norm");
            temporaryWarp = tag.getInt("temp");
            counter = tag.getInt("counter");
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
            deserializeNBT(tag);
        }
    }
}
