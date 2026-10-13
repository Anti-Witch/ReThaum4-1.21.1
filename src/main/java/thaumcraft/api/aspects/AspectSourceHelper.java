package thaumcraft.api.aspects;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Helper class for draining and finding essentia from nearby sources.
 * Uses reflection to call into the main Thaumcraft mod's EssentiaHandler.
 */
public class AspectSourceHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(AspectSourceHelper.class);

    private static Method drainEssentiaMethod;
    private static Method findEssentiaMethod;

    /**
     * This method is what is used to drain essentia from jars and other sources for things like
     * infusion crafting or powering the arcane furnace. A record of possible sources are kept track of
     * and refreshed as needed around the calling tile entity. This also renders the essentia trail particles.
     * Only 1 essentia is drained at a time.
     *
     * @param blockEntity the block entity that is draining the essentia
     * @param aspect the aspect that you are looking for
     * @param direction the direction from which you wish to drain. null seeks in all directions.
     * @param range how many blocks you wish to search for essentia sources
     * @return true if essentia was found and removed from a source
     */
    public static boolean drainEssentia(BlockEntity blockEntity, Aspect aspect, Direction direction, int range) {
        try {
            if (drainEssentiaMethod == null) {
                Class<?> handlerClass = Class.forName("thaumcraft.common.lib.events.EssentiaHandler");
                drainEssentiaMethod = handlerClass.getMethod("drainEssentia", 
                    BlockEntity.class, Aspect.class, Direction.class, int.class);
            }
            return (Boolean) drainEssentiaMethod.invoke(null, blockEntity, aspect, direction, range);
        } catch (Exception ex) {
            LOGGER.warn("[Thaumcraft API] Could not invoke thaumcraft.common.lib.events.EssentiaHandler method drainEssentia");
        }
        return false;
    }

    /**
     * This method returns if there is any essentia of the passed type that can be drained. It in no way checks how
     * much there is, only if an essentia container nearby contains at least 1 point worth.
     *
     * @param blockEntity the block entity that is checking the essentia
     * @param aspect the aspect that you are looking for
     * @param direction the direction from which you wish to drain. null seeks in all directions.
     * @param range how many blocks you wish to search for essentia sources
     * @return true if essentia was found
     */
    public static boolean findEssentia(BlockEntity blockEntity, Aspect aspect, Direction direction, int range) {
        try {
            if (findEssentiaMethod == null) {
                Class<?> handlerClass = Class.forName("thaumcraft.common.lib.events.EssentiaHandler");
                findEssentiaMethod = handlerClass.getMethod("findEssentia",
                    BlockEntity.class, Aspect.class, Direction.class, int.class);
            }
            return (Boolean) findEssentiaMethod.invoke(null, blockEntity, aspect, direction, range);
        } catch (Exception ex) {
            LOGGER.warn("[Thaumcraft API] Could not invoke thaumcraft.common.lib.events.EssentiaHandler method findEssentia");
        }
        return false;
    }
}




