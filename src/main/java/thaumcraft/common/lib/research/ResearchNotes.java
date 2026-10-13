package thaumcraft.common.lib.research;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.items.IScribeTools;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.common.lib.utils.HexUtils;
import thaumcraft.common.lib.utils.NBTHelper;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * ResearchNotes - Core of the TC4 hex-grid research note mechanic.
 * Generates the hex-grid puzzle from a ResearchEntry (required aspects placed on the
 * outer ring of the canvas, canvas radius and blank hex count derived from complexity),
 * (de)serializes the note, and checks puzzle completion by validating that every required
 * aspect hex is connected through combinable aspect links the player has discovered.
 *
 * Ported from OpenTC4 ResearchManager (TC4) - hex-grid mechanic is TC4-only, not present
 * in TC6/ref. Only the storage layer (NBTHelper DataComponents) and knowledge access
 * (research pool capability) are adapted to the 1.21.1 target.
 *
 * @author Azanor
 */
public class ResearchNotes {

   /**
    * Creates a fresh hex-grid research note for the given research key.
    * The puzzle layout is generated from the research entry:
    * - radius = 1 + min(3, complexity) - canvas size depends on the research
    * - required aspects (tags) count = number of starting aspects on the outer ring
    * - blanks = complexity * 2 empty hexes removed from the canvas
    */
   public static ItemStack createNote(ItemStack stack, String key, Level world) {
      ResearchEntry rr = ResearchCategories.getResearch(key);
      if (rr == null) {
         return null;
      }
      Aspect[] aspects = rr.getRequiredAspects() != null ? rr.getRequiredAspects().getAspects() : new Aspect[0];
      if (aspects.length == 0) {
         return null;
      }
      Aspect primaryaspect = aspects[0];

      CompoundTag root = NBTHelper.getOrCreateTag(stack);
      root.putString("key", key);
      root.putInt("color", primaryaspect.getColor());
      root.putBoolean("complete", false);
      root.putInt("copies", 0);
      // Reset to the uncompleted model variant
      if (stack.getItem() instanceof thaumcraft.common.items.ItemResearchNotes) {
         stack.setDamageValue(0);
      }

      int radius = 1 + Math.min(3, rr.getComplexity());
      HashMap<String, HexUtils.Hex> hexLocs = HexUtils.generateHexes(radius);
      ArrayList<HexUtils.Hex> outerRing = HexUtils.distributeRingRandomly(radius, aspects.length, world.getRandom());
      HashMap<String, HexEntry> hexEntries = new HashMap<>();
      HashMap<String, HexUtils.Hex> hexes = new HashMap<>();

      for (HexUtils.Hex hex : hexLocs.values()) {
         hexes.put(hex.toString(), hex);
         hexEntries.put(hex.toString(), new HexEntry(null, 0));
      }

      int count = 0;
      for (HexUtils.Hex hex : outerRing) {
         hexes.put(hex.toString(), hex);
         hexEntries.put(hex.toString(), new HexEntry(aspects[count], 1));
         ++count;
      }

      if (rr.getComplexity() > 1) {
         int blanks = rr.getComplexity() * 2;
         HexUtils.Hex[] temp = hexes.values().toArray(new HexUtils.Hex[0]);

         while (blanks > 0) {
            int indx = world.getRandom().nextInt(temp.length);
            if (hexEntries.get(temp[indx].toString()) != null && hexEntries.get(temp[indx].toString()).type == 0) {
               boolean gtg = true;

               for (int n = 0; n < 6; n++) {
                  HexUtils.Hex neighbour = temp[indx].getNeighbour(n);
                  if (hexes.containsKey(neighbour.toString()) && hexEntries.get(neighbour.toString()).type == 1) {
                     int cc = 0;

                     for (int q = 0; q < 6; q++) {
                        if (hexes.containsKey(hexes.get(neighbour.toString()).getNeighbour(q).toString())) {
                           ++cc;
                        }

                        if (cc >= 2) {
                           break;
                        }
                     }

                     if (cc < 2) {
                        gtg = false;
                        break;
                     }
                  }
               }

               if (gtg) {
                  hexes.remove(temp[indx].toString());
                  hexEntries.remove(temp[indx].toString());
                  temp = hexes.values().toArray(new HexUtils.Hex[0]);
                  --blanks;
               }
            }
         }
      }

      ListTag gridtag = new ListTag();

      for (HexUtils.Hex hex : hexes.values()) {
         CompoundTag gt = new CompoundTag();
         gt.putByte("hexq", (byte) hex.q);
         gt.putByte("hexr", (byte) hex.r);
         gt.putByte("type", (byte) hexEntries.get(hex.toString()).type);
         if (hexEntries.get(hex.toString()).aspect != null) {
            gt.putString("aspect", hexEntries.get(hex.toString()).aspect.getTag());
         }
         gridtag.add(gt);
      }

      root.put("hexgrid", gridtag);
      return stack;
   }

   /**
    * Deserialize the note's puzzle data from the item stack.
    */
   public static ResearchNoteData getData(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return null;
      }
      CompoundTag root = NBTHelper.getTag(stack);
      if (root == null || !root.contains("key")) {
         return null;
      }

      ResearchNoteData data = new ResearchNoteData();
      data.key = root.getString("key");
      data.color = root.getInt("color");
      data.complete = root.getBoolean("complete");
      data.copies = root.getInt("copies");
      ListTag grid = root.getList("hexgrid", Tag.TAG_COMPOUND);

      for (int x = 0; x < grid.size(); x++) {
         CompoundTag nbt = grid.getCompound(x);
         int q = nbt.getByte("hexq");
         int r = nbt.getByte("hexr");
         int type = nbt.getByte("type");
         String tag = nbt.getString("aspect");
         Aspect aspect = (tag == null || tag.isEmpty()) ? null : Aspect.getAspect(tag);
         HexUtils.Hex hex = new HexUtils.Hex(q, r);
         data.hexEntries.put(hex.toString(), new HexEntry(aspect, type));
         data.hexes.put(hex.toString(), hex);
      }

      return data;
   }

   /**
    * Serialize the note's puzzle data back to the item stack.
    */
   public static void updateData(ItemStack stack, ResearchNoteData data) {
      CompoundTag root = NBTHelper.getOrCreateTag(stack);
      root.putString("key", data.key);
      root.putInt("color", data.color);
      root.putBoolean("complete", data.complete);
      root.putInt("copies", data.copies);
      ListTag gridtag = new ListTag();

      for (HexUtils.Hex hex : data.hexes.values()) {
         CompoundTag gt = new CompoundTag();
         gt.putByte("hexq", (byte) hex.q);
         gt.putByte("hexr", (byte) hex.r);
         gt.putByte("type", (byte) data.hexEntries.get(hex.toString()).type);
         if (data.hexEntries.get(hex.toString()).aspect != null) {
            gt.putString("aspect", data.hexEntries.get(hex.toString()).aspect.getTag());
         }
         gridtag.add(gt);
      }

      root.put("hexgrid", gridtag);
   }

   /**
    * Checks if all required aspect hexes (type 1) are connected through valid
    * aspect links. A link between two hexes is valid when one aspect is a component
    * of the other and the player has discovered both. On success the note is marked
    * complete and unused placed hexes (type 2) are removed.
    */
   public static boolean checkResearchCompletion(ItemStack contents, ResearchNoteData note, Player player) {
      ArrayList<String> checked = new ArrayList<>();
      ArrayList<String> main = new ArrayList<>();
      ArrayList<String> remains = new ArrayList<>();

      for (HexUtils.Hex hex : note.hexes.values()) {
         if (note.hexEntries.get(hex.toString()).type == 1) {
            main.add(hex.toString());
         }
      }

      for (HexUtils.Hex hex : note.hexes.values()) {
         if (note.hexEntries.get(hex.toString()).type == 1) {
            main.remove(hex.toString());
            checkConnections(note, hex, checked, main, remains, player);
            break;
         }
      }

      if (!main.isEmpty()) {
         return false;
      } else {
         ArrayList<String> remove = new ArrayList<>();

         for (HexUtils.Hex hex : note.hexes.values()) {
            if (note.hexEntries.get(hex.toString()).type != 1 && !remains.contains(hex.toString())) {
               remove.add(hex.toString());
            }
         }

         for (String s : remove) {
            note.hexEntries.remove(s);
            note.hexes.remove(s);
         }

         note.complete = true;
         updateData(contents, note);
         // Mark the note as "complete" for the item model (TC4 damage-64 trick).
         if (contents != null && contents.getItem() instanceof thaumcraft.common.items.ItemResearchNotes) {
            contents.setDamageValue(contents.getMaxDamage() - 1);
            // TC4: completed discoveries are epic
            contents.set(net.minecraft.core.component.DataComponents.RARITY, net.minecraft.world.item.Rarity.EPIC);
         }
         return true;
      }
   }

   private static void checkConnections(ResearchNoteData note, HexUtils.Hex hex, ArrayList<String> checked,
                                        ArrayList<String> main, ArrayList<String> remains, Player player) {
      checked.add(hex.toString());

      for (int a = 0; a < 6; a++) {
         HexUtils.Hex target = hex.getNeighbour(a);
         if (!checked.contains(target.toString()) && note.hexEntries.containsKey(target.toString())
                 && note.hexEntries.get(target.toString()).type >= 1) {
            Aspect aspect1 = note.hexEntries.get(hex.toString()).aspect;
            Aspect aspect2 = note.hexEntries.get(target.toString()).aspect;
            if (hasDiscoveredAspect(player, aspect1) && hasDiscoveredAspect(player, aspect2)
                    && (!aspect1.isPrimal() && (aspect1.getComponents()[0] == aspect2 || aspect1.getComponents()[1] == aspect2)
                    || !aspect2.isPrimal() && (aspect2.getComponents()[0] == aspect1 || aspect2.getComponents()[1] == aspect1))) {
               remains.add(target.toString());
               if (note.hexEntries.get(target.toString()).type == 1) {
                  main.remove(target.toString());
               }
               checkConnections(note, target, checked, main, remains, player);
            }
         }
      }
   }

   /**
    * True when the player has discovered the aspect (TC4's hasDiscoveredAspect:
    * primal aspects are always known; compound aspects stay known even when the
    * research pool amount is zero).
    */
   public static boolean hasDiscoveredAspect(Player player, Aspect aspect) {
      if (aspect == null) {
         return false;
      }
      IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
      return knowledge != null && knowledge.hasDiscoveredAspect(aspect);
   }

   public static boolean consumeInkFromTable(ItemStack stack, boolean doit) {
      if (stack != null && !stack.isEmpty() && stack.getItem() instanceof IScribeTools
              && stack.getDamageValue() < stack.getMaxDamage()) {
         if (doit) {
            stack.setDamageValue(stack.getDamageValue() + 1);
         }
         return true;
      }
      return false;
   }

   public static boolean consumeInkFromPlayer(Player player, boolean doit) {
      for (int a = 0; a < player.getInventory().getContainerSize(); a++) {
         ItemStack itemStack = player.getInventory().getItem(a);
         if (itemStack != null && !itemStack.isEmpty() && itemStack.getItem() instanceof IScribeTools
                 && itemStack.getDamageValue() < itemStack.getMaxDamage()) {
            if (doit) {
               itemStack.setDamageValue(itemStack.getDamageValue() + 1);
            }
            return true;
         }
      }
      return false;
   }

   public static class HexEntry {
      public Aspect aspect;
      public int type;

      public HexEntry(Aspect aspect, int type) {
         this.aspect = aspect;
         this.type = type;
      }
   }
}