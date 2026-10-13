package thaumcraft.api.golems.parts;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.golems.IGolemAPI;

/**
 * Defines a 3D model for a golem part.
 * Includes model location, texture, and attachment point.
 */
public class PartModel {

    private final ResourceLocation objModel;
    private final ResourceLocation texture;
    private final EnumAttachPoint attachPoint;

    public PartModel(ResourceLocation objModel, ResourceLocation objTexture, EnumAttachPoint attachPoint) {
        this.objModel = objModel;
        this.texture = objTexture;
        this.attachPoint = attachPoint;
    }

    public ResourceLocation getObjModel() {
        return objModel;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public EnumAttachPoint getAttachPoint() {
        return attachPoint;
    }

    /**
     * Check if a part should use the material's texture instead of the part's texture
     * @param partName The name of the model part
     * @return true if material texture should be used
     */
    public boolean useMaterialTextureForObjectPart(String partName) {
        return partName.startsWith("bm");
    }

    /**
     * Called before rendering an object part - override for custom transformations
     */
    public void preRenderObjectPart(String partName, IGolemAPI golem, float partialTicks, EnumLimbSide side) {
    }

    /**
     * Called after rendering an object part - override for cleanup
     */
    public void postRenderObjectPart(String partName, IGolemAPI golem, float partialTicks, EnumLimbSide side) {
    }

    /**
     * Override to modify arm rotation on X axis
     */
    public float preRenderArmRotationX(IGolemAPI golem, float partialTicks, EnumLimbSide side, float inputRot) {
        return inputRot;
    }

    /**
     * Override to modify arm rotation on Y axis
     */
    public float preRenderArmRotationY(IGolemAPI golem, float partialTicks, EnumLimbSide side, float inputRot) {
        return inputRot;
    }

    /**
     * Override to modify arm rotation on Z axis
     */
    public float preRenderArmRotationZ(IGolemAPI golem, float partialTicks, EnumLimbSide side, float inputRot) {
        return inputRot;
    }

    /**
     * Where the part attaches to the golem body
     */
    public enum EnumAttachPoint {
        ARMS,
        LEGS,
        BODY,
        HEAD
    }

    /**
     * Which side of the golem (for paired parts like arms/legs)
     */
    public enum EnumLimbSide {
        LEFT,
        RIGHT,
        MIDDLE
    }
}




