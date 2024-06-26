package jss.notfine.mixins.early.minecraft.clouds;

import jss.notfine.core.Settings;
import jss.notfine.gui.options.named.GraphicsQualityOff;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.IRenderHandler;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RenderGlobal.class, priority = 990)
public abstract class MixinRenderGlobal {

    /**
     * @author jss2a98aj
     * @reason Adjust how cloud render mode is selected.
     */
    @Overwrite
    public void renderClouds(float partialTicks) {
        IRenderHandler renderer;
        if ((renderer = theWorld.provider.getCloudRenderer()) != null) {
            renderer.render(partialTicks, theWorld, mc);
            return;
        }
        if (mc.theWorld.provider.isSurfaceWorld()) {
            renderCloudsFancy(partialTicks);
        }
    }

    /**
     * @author jss2a98aj
     * @reason Adjust fancy cloud render.
     */
    @Overwrite
    public void renderCloudsFancy(float partialTicks) {
        final Tessellator tessellator = Tessellator.instance;
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        renderEngine.bindTexture(locationCloudsPng);

        final double cloudTick = (float)cloudTickCounter + partialTicks;

        //Scale
        final float cloudScale = (int)Settings.CLOUD_SCALE.option.getStore();
        final float cloudSegmentWidth = 12.0F * cloudScale;
        final float cloudSegmentHeight = 4.0F * cloudScale;
        GL11.glScalef(cloudSegmentWidth, 1.0F, cloudSegmentWidth);

        //Camera offsets
        final float cameraOffsetY = (float)(mc.renderViewEntity.lastTickPosY + (mc.renderViewEntity.posY - mc.renderViewEntity.lastTickPosY) * (double)partialTicks);
        double cameraOffsetX = (mc.renderViewEntity.prevPosX + (mc.renderViewEntity.posX - mc.renderViewEntity.prevPosX) * (double)partialTicks + cloudTick * 0.029999999329447746D) / (double)cloudSegmentWidth;
        double cameraOffsetZ = (mc.renderViewEntity.prevPosZ + (mc.renderViewEntity.posZ - mc.renderViewEntity.prevPosZ) * (double)partialTicks) / (double)cloudSegmentWidth + 0.33000001311302185D;

        //Wind
        final float windSpeed = 0.00390625F;
        final float windX = (float)MathHelper.floor_double(cameraOffsetX) * windSpeed;
        final float windZ = (float)MathHelper.floor_double(cameraOffsetZ) * windSpeed;

        //Camera offsets (cont)
        cameraOffsetX -= MathHelper.floor_double(cameraOffsetX / 2048.0D) * 2048;
        cameraOffsetZ -= MathHelper.floor_double(cameraOffsetZ / 2048.0D) * 2048;

        //Camera relatives
        float cloudHeight = (int)theWorld.provider.getCloudHeight();
        //Allows the setting to work with RFG and similar without hardcoding.
        //The minimum height check is so stuff like Aether cloud height doesn't get messed up.
        if(cloudHeight >= 96) {
            cloudHeight = (int)Settings.CLOUD_HEIGHT.option.getStore();
        }
        final float cloudElevation = cloudHeight - cameraOffsetY + 0.33F;
        final float cameraRelativeX = (float)(cameraOffsetX - (double)MathHelper.floor_double(cameraOffsetX));
        final float cameraRelativeZ = (float)(cameraOffsetZ - (double)MathHelper.floor_double(cameraOffsetZ));

        //Colors
        final Vec3 color = theWorld.getCloudColour(partialTicks);
        float red = (float)color.xCoord;
        float green = (float)color.yCoord;
        float blue = (float)color.zCoord;
        if (mc.gameSettings.anaglyph) {
            final float anaglyphRed = (red * 30.0F + green * 59.0F + blue * 11.0F) / 100.0F;
            final float anaglyphGreen = (red * 30.0F + green * 70.0F) / 100.0F;
            final float anaglyphBlue = (red * 30.0F + blue * 70.0F) / 100.0F;
            red = anaglyphRed;
            green = anaglyphGreen;
            blue = anaglyphBlue;
        }

        final float segmentWidth = 8f;
        final int renderRadius = (int)((int)Settings.RENDER_DISTANCE_CLOUDS.option.getStore() / cloudScale);
        final float edgeOverlap = 0.0001f;//0.001F;
        final GraphicsQualityOff cloudMode = (GraphicsQualityOff)Settings.MODE_CLOUDS.option.getStore();
        final boolean fancy = cloudMode == GraphicsQualityOff.FANCY || cloudMode == GraphicsQualityOff.DEFAULT && mc.gameSettings.fancyGraphics;

        final float top = cloudElevation + cloudSegmentHeight;
        final float topOffset = top - edgeOverlap;

        for (int pass = 0; pass < 2; ++pass) {
            if (pass == 0) {
                GL11.glColorMask(false, false, false, false);
            } else if (mc.gameSettings.anaglyph) {
                if (EntityRenderer.anaglyphField == 0) {
                    GL11.glColorMask(false, true, true, true);
                } else {
                    GL11.glColorMask(true, false, false, true);
                }
            } else {
                GL11.glColorMask(true, true, true, true);
            }

            final float min = (-renderRadius + 1f) * segmentWidth;
            final float max = (renderRadius + 1f) * segmentWidth;

            final float minX = min - cameraRelativeX;
            final float maxX = max - cameraRelativeX;
            final float minZ = min - cameraRelativeZ;
            final float maxZ = max - cameraRelativeZ;

            final float UVMinX = min * windSpeed + windX;
            final float UVMaxX = max * windSpeed + windX;
            final float UVMinZ = min * windSpeed + windZ;
            final float UVMaxZ = max * windSpeed + windZ;

            //Bottom
            if (!fancy || cloudElevation > -cloudSegmentHeight - 1.0F) {
                tessellator.startDrawingQuads();
                tessellator.setColorRGBA_F(red * 0.7F, green * 0.7F, blue * 0.7F, 0.8F);
                tessellator.setNormal(0.0F, -1.0F, 0.0F);
                tessellator.addVertexWithUV(minX, cloudElevation, maxZ, UVMinX, UVMaxZ);
                tessellator.addVertexWithUV(maxX, cloudElevation, maxZ, UVMaxX, UVMaxZ);
                tessellator.addVertexWithUV(maxX, cloudElevation, minZ, UVMaxX, UVMinZ);
                tessellator.addVertexWithUV(minX, cloudElevation, minZ, UVMinX, UVMinZ);
                tessellator.draw();
            }

            if(fancy) {
                //Top
                if (cloudElevation <= cloudSegmentHeight + 1.0F) {
                    tessellator.startDrawingQuads();
                    tessellator.setColorRGBA_F(red, green, blue, 0.8F);
                    tessellator.setNormal(0.0F, 1.0F, 0.0F);
                    tessellator.addVertexWithUV(minX, topOffset, maxZ, UVMinX, UVMaxZ);
                    tessellator.addVertexWithUV(maxX, topOffset, maxZ, UVMaxX, UVMaxZ);
                    tessellator.addVertexWithUV(maxX, topOffset, minZ, UVMaxX, UVMinZ);
                    tessellator.addVertexWithUV(minX, topOffset, minZ, UVMinX, UVMinZ);
                    tessellator.draw();
                }

                //sides
                int segment, side;

                tessellator.startDrawingQuads();
                tessellator.setColorRGBA_F(red * 0.9F, green * 0.9F, blue * 0.9F, 0.8F);
                for (segment = -renderRadius + 1; segment <= renderRadius; ++segment) {
                    final float segmentOffsetX = segment * segmentWidth;
                    final float startX = segmentOffsetX - cameraRelativeX;
                    if (segment > -1) {
                        tessellator.setNormal(-1.0F, 0.0F, 0.0F);
                        for (side = 0; side < segmentWidth; ++side) {
                            final float sideStartX = startX + (float) side;
                            final float UVSide = (segmentOffsetX + (float) side + 0.5F) * windSpeed + windX;
                            tessellator.addVertexWithUV(sideStartX, cloudElevation, maxZ, UVSide, UVMaxZ);
                            tessellator.addVertexWithUV(sideStartX, top,            maxZ, UVSide, UVMaxZ);
                            tessellator.addVertexWithUV(sideStartX, top,            minZ, UVSide, UVMinZ);
                            tessellator.addVertexWithUV(sideStartX, cloudElevation, minZ, UVSide, UVMinZ);
                        }
                    }
                    if (segment <= 1) {
                        tessellator.setNormal(1.0F, 0.0F, 0.0F);
                        for (side = 0; side < segmentWidth; ++side) {
                            final float sideEndXOverlap = startX + (float) side + 1.0F - edgeOverlap;
                            final float UVSide = (segmentOffsetX + (float) side + 0.5F) * windSpeed + windX;
                            tessellator.addVertexWithUV(sideEndXOverlap, cloudElevation,    maxZ, UVSide, UVMaxZ);
                            tessellator.addVertexWithUV(sideEndXOverlap, top,               maxZ, UVSide, UVMaxZ);
                            tessellator.addVertexWithUV(sideEndXOverlap, top,               minZ, UVSide, UVMinZ);
                            tessellator.addVertexWithUV(sideEndXOverlap, cloudElevation,    minZ, UVSide, UVMinZ);
                        }
                    }
                }
                tessellator.draw();

                tessellator.startDrawingQuads();
                tessellator.setColorRGBA_F(red * 0.8F, green * 0.8F, blue * 0.8F, 0.8F);
                for (segment = -renderRadius + 1; segment <= renderRadius; ++segment) {
                    final float segmentOffsetZ = segment * segmentWidth;
                    final float startZ = segmentOffsetZ - cameraRelativeZ;
                    if (segment > -1) {
                        tessellator.setNormal(0.0F, 0.0F, -1.0F);
                        for (side = 0; side < segmentWidth; ++side) {
                            final float sideStartZ = startZ + (float) side;
                            final float UVSide = (segmentOffsetZ + (float) side + 0.5F) * windSpeed + windZ;
                            tessellator.addVertexWithUV(minX, top,              sideStartZ, UVMinX, UVSide);
                            tessellator.addVertexWithUV(maxX, top,              sideStartZ, UVMaxX, UVSide);
                            tessellator.addVertexWithUV(maxX, cloudElevation,   sideStartZ, UVMaxX, UVSide);
                            tessellator.addVertexWithUV(minX, cloudElevation,   sideStartZ, UVMinX, UVSide);
                        }
                    }
                    if (segment <= 1) {
                        tessellator.setNormal(0.0F, 0.0F, 1.0F);
                        for (side = 0; side < segmentWidth; ++side) {
                            final float sideEndZOverlap = startZ + (float) side + 1.0F - edgeOverlap;
                            final float UVSide = (segmentOffsetZ + (float) side + 0.5F) * windSpeed + windZ;
                            tessellator.addVertexWithUV(minX, top,              sideEndZOverlap, UVMinX, UVSide);
                            tessellator.addVertexWithUV(maxX, top,              sideEndZOverlap, UVMaxX, UVSide);
                            tessellator.addVertexWithUV(maxX, cloudElevation,   sideEndZOverlap, UVMaxX, UVSide);
                            tessellator.addVertexWithUV(minX, cloudElevation,   sideEndZOverlap, UVMinX, UVSide);
                        }
                    }
                }
                tessellator.draw();
            }
        }

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

    @Shadow @Final
    private static ResourceLocation locationCloudsPng;
    @Shadow @Final
    private TextureManager renderEngine;

    @Shadow private WorldClient theWorld;
    @Shadow private Minecraft mc;
    @Shadow private int cloudTickCounter;

}
