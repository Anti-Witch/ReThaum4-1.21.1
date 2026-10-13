package thaumcraft.common.lib.research;

import thaumcraft.common.lib.utils.HexUtils;

import java.util.HashMap;

/**
 * ResearchNoteData - Runtime data of the TC4 hex-grid research note.
 * Holds the puzzle state: every hex on the canvas, its aspect and type,
 * plus completion/copy tracking.
 * Ported 1:1 from OpenTC4 (TC4) - hex-grid mechanic is TC4-only.
 *
 * @author Azanor
 */
public class ResearchNoteData {
   public String key;
   public int color = 0;
   public HashMap<String, ResearchNotes.HexEntry> hexEntries = new HashMap<>();
   public HashMap<String, HexUtils.Hex> hexes = new HashMap<>();
   public boolean complete = false;
   public int copies = 0;

   public boolean isComplete() {
      return complete;
   }
}