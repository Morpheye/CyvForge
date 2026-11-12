package net.cyvforge.gui;

import net.cyvforge.CyvForge;
import net.cyvforge.config.CyvClientConfig;
import net.cyvforge.event.events.ParkourTickListener;
import net.cyvforge.util.GuiUtils;
import net.cyvforge.util.RenderUtils;
import net.cyvforge.util.defaults.CyvGui;
import net.cyvforge.util.parkour.LandingAxis;
import net.cyvforge.util.parkour.LandingBlock;
import net.cyvforge.util.parkour.LandingMode;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.List;

public class GuiLb extends CyvGui {

    public static float autoRotationSpeed = 0.5F;   // rotation speed
    public static float mouseSensitivity = 0.6F;    // mouse drag speed
    public static float previewBrightness = 1.0F;   // brightness

    LandingBlock lb;
    SubButton landingModeButton;
    SubButton axisButton;
    SubButton bbVisibleButton;
    SubButton condVisibleButton;
    SubButton neoAndBlock;
    SubButton ignoreWallHitButton;
    GuiTextField targetTickField;

    // 3D preview camera parameters
    private float previewYaw = 45.0F;
    private float previewPitch = 30.0F;
    private float zoom = 7.0F;
    private boolean isDraggingPreview = false;
    private int lastMouseX, lastMouseY;
    private long lastFrameTime = 0;

    public GuiLb(LandingBlock b) {
        super("Landing Block GUI");
        this.lb = b;
        mc = Minecraft.getMinecraft();
    }

    @Override
    public void initGui() {
        if (lb == null) {
            CyvForge.sendChatMessage("NULL LB");
            mc.displayGuiScreen(null);
            return;
        }

        Keyboard.enableRepeatEvents(true);

        // init buttons
        this.landingModeButton = new SubButton("Landing Mode: " + lb.mode.toString(), this.width - 160, 10, 150, 20);
        this.axisButton = new SubButton("Axis: " + lb.axis.toString(), this.width - 160, 35, 150, 20);
        this.bbVisibleButton = new SubButton("BB Visible: " + CyvClientConfig.getBoolean("highlightLanding", false), this.width - 160, 60, 150, 20);
        this.condVisibleButton = new SubButton("Cond Visible: " + CyvClientConfig.getBoolean("highlightLandingCond", false), this.width - 160, 85, 150, 20);

        int labelWidth = fontRendererObj.getStringWidth("Target Tick: ");
        this.targetTickField = new GuiTextField(9, fontRendererObj, this.width - 160 + 8 + labelWidth, 116, 60, 12);
        this.targetTickField.setMaxStringLength(2);
        this.targetTickField.setText(lb.targetTick == -1 ? "" : String.valueOf(lb.targetTick));
        this.targetTickField.setEnableBackgroundDrawing(false);
        this.targetTickField.setVisible(true);

        this.neoAndBlock = new SubButton("Neo & Landing: " + lb.neoAndNormal, this.width - 160, 135, 150, 20);
        this.ignoreWallHitButton = new SubButton("Ignore Wall: " + lb.ignoreWallHit, this.width - 160, 160, 150, 20);

        this.landingModeButton.setEnabled(true);
        this.axisButton.setEnabled(true);
        this.bbVisibleButton.setEnabled(true);
        this.condVisibleButton.setEnabled(true);
        this.ignoreWallHitButton.setEnabled(true);

        if (lb.axis.equals(LandingAxis.both)) {
            this.axisButton.setText("Axis: Both");
        } else if (lb.axis.equals(LandingAxis.z)) {
            this.axisButton.setText("Axis: Z");
        } else {
            lb.axis = LandingAxis.x;
            this.axisButton.setText("Axis: X");
        }

        if (lb.mode.equals(LandingMode.landing)) {
            this.landingModeButton.setText("Landing Mode: Landing");
        } else if (lb.mode.equals(LandingMode.hit)) {
            this.landingModeButton.setText("Landing Mode: Hit");
        } else if (lb.mode.equals(LandingMode.z_neo)) {
            this.landingModeButton.setText("Landing Mode: Z Neo");
        } else if (lb.mode.equals(LandingMode.x_neo)) {
            this.landingModeButton.setText("Landing Mode: X Neo");
        } else {
            lb.mode = LandingMode.enter;
            this.landingModeButton.setText("Landing Mode: Enter");
        }

        this.neoAndBlock.setEnabled(lb.mode == LandingMode.z_neo);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawDefaultBackground();

        if (CyvClientConfig.getBoolean("lb3DPreview", true)) {
            render3DPreview(partialTicks);
        } else {
            GuiUtils.drawCenteredString("\u00A77(3D Preview is disabled in settings)", (this.width - 160) / 2, this.height / 2, 0x88AAAAAA, true);
        }

        final int BUTTON_X = this.width - 160;
        final int BUTTON_SIZE = 150;
        final int BUTTON_COUNT = 7;
        GuiUtils.drawRoundedRect(BUTTON_X - 4, 6,
                BUTTON_X + BUTTON_SIZE + 4, 10 + BUTTON_COUNT * 25,
                5, CyvForge.theme.background1);

        int fieldBg = targetTickField.isFocused() ? 0x80555555 : CyvForge.theme.shade2;
        GuiUtils.drawRoundedRect(BUTTON_X, 110, BUTTON_X + BUTTON_SIZE, 130, 5, fieldBg);
        GuiUtils.drawString("Target Tick: ", BUTTON_X + 8, 116, 0xFFFFFFFF, true);

        this.landingModeButton.draw(mouseX, mouseY);
        this.axisButton.draw(mouseX, mouseY);
        this.bbVisibleButton.draw(mouseX, mouseY);
        this.condVisibleButton.draw(mouseX, mouseY);
        this.targetTickField.drawTextBox();
        this.neoAndBlock.draw(mouseX, mouseY);
        this.ignoreWallHitButton.draw(mouseX, mouseY);
    }

    private void render3DPreview(float partialTicks) {
        if (lb == null || lb.pos == null || mc.theWorld == null) return;

        BlockPos center = lb.pos;
        World world = mc.theWorld;

        long now = System.currentTimeMillis();
        if (lastFrameTime == 0) lastFrameTime = now;
        float delta = (now - lastFrameTime) / 1000.0F;
        lastFrameTime = now;
        if (delta > 0.1F) delta = 0.1F;

        if (!isDraggingPreview) {
            previewYaw += autoRotationSpeed * delta * 60.0F;
        }

        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        float aspect = (float) this.width / (float) this.height;
        Project.gluPerspective(60.0F, aspect, 0.1F, 1000.0F);

        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();

        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);

        float screenShiftX = (this.width > 220) ? -0.85F : 0.0F;
        GlStateManager.translate(screenShiftX, 0.0F, -zoom);
        GlStateManager.rotate(previewPitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(previewYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(-0.5F, -0.5F, -0.5F);

        RenderHelper.enableStandardItemLighting();

        FloatBuffer lightBuf = BufferUtils.createFloatBuffer(4);
        lightBuf.put(new float[]{previewBrightness, previewBrightness, previewBrightness, 1.0F}).flip();
        GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, lightBuf);

        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        BlockRendererDispatcher dispatcher = mc.getBlockRendererDispatcher();

        final World finalWorld = world;
        final BlockPos finalCenter = center;

        final int radius = 2;

        IBlockAccess previewWorld = new IBlockAccess() {
            @Override
            public IBlockState getBlockState(BlockPos pos) {
                int dx = pos.getX() - finalCenter.getX();
                int dy = pos.getY() - finalCenter.getY();
                int dz = pos.getZ() - finalCenter.getZ();
                if (Math.abs(dx) > radius || Math.abs(dy) > radius || Math.abs(dz) > radius) {
                    return Blocks.air.getDefaultState();
                }
                return finalWorld.getBlockState(pos);
            }

            @Override
            public TileEntity getTileEntity(BlockPos pos) {
                int dx = pos.getX() - finalCenter.getX();
                int dy = pos.getY() - finalCenter.getY();
                int dz = pos.getZ() - finalCenter.getZ();
                if (Math.abs(dx) > radius || Math.abs(dy) > radius || Math.abs(dz) > radius) {
                    return null;
                }
                return finalWorld.getTileEntity(pos);
            }

            @Override
            public int getCombinedLight(BlockPos pos, int lightValue) {
                return finalWorld.getCombinedLight(pos, lightValue);
            }

            @Override
            public boolean isAirBlock(BlockPos pos) {
                int dx = pos.getX() - finalCenter.getX();
                int dy = pos.getY() - finalCenter.getY();
                int dz = pos.getZ() - finalCenter.getZ();
                if (Math.abs(dx) > radius || Math.abs(dy) > radius || Math.abs(dz) > radius) {
                    return true;
                }
                return finalWorld.isAirBlock(pos);
            }

            @Override
            public BiomeGenBase getBiomeGenForCoords(BlockPos pos) {
                return finalWorld.getBiomeGenForCoords(pos);
            }

            @Override
            public int getStrongPower(BlockPos pos, EnumFacing direction) {
                return 0;
            }

            @Override
            public WorldType getWorldType() {
                return finalWorld.getWorldType();
            }

            @Override
            public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
                int dx = pos.getX() - finalCenter.getX();
                int dy = pos.getY() - finalCenter.getY();
                int dz = pos.getZ() - finalCenter.getZ();
                if (Math.abs(dx) > radius || Math.abs(dy) > radius || Math.abs(dz) > radius) {
                    return false;
                }
                return finalWorld.isSideSolid(pos, side, _default);
            }

            @Override
            public boolean extendedLevelsInChunkCache() {
                return false;
            }
        };

        worldrenderer.begin(7, DefaultVertexFormats.BLOCK);
        worldrenderer.setTranslation(-center.getX(), -center.getY(), -center.getZ());

        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos bPos = center.add(dx, dy, dz);
                    try {
                        IBlockState state = previewWorld.getBlockState(bPos);
                        Block block = state.getBlock();
                        if (block == null || block.getMaterial() == Material.air) continue;
                        dispatcher.renderBlock(state, bPos, previewWorld, worldrenderer);
                    } catch (Throwable ignored) {}
                }
            }
        }

        tessellator.draw();
        worldrenderer.setTranslation(0.0D, 0.0D, 0.0D);

        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos bPos = center.add(dx, dy, dz);
                    try {
                        TileEntity te = previewWorld.getTileEntity(bPos);
                        if (te != null) {
                            TileEntityRendererDispatcher.instance.renderTileEntityAt(
                                    te,
                                    bPos.getX() - center.getX(),
                                    bPos.getY() - center.getY(),
                                    bPos.getZ() - center.getZ(),
                                    partialTicks
                            );
                        }
                    } catch (Throwable ignored) {}
                }
            }
        }

        boolean isMomentum = (lb == ParkourTickListener.momentumBlock);
        int r = isMomentum ? 255 : 0;
        int g = isMomentum ? 0 : 192;
        int b = isMomentum ? 0 : 255;

        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        AxisAlignedBB condBB = new AxisAlignedBB(
                lb.xMinCond - center.getX() + 0.3, lb.smallestY() - center.getY(),
                lb.zMinCond - center.getZ() + 0.3, lb.xMaxCond - center.getX() - 0.3,
                lb.largestY() - center.getY(), lb.zMaxCond - center.getZ() - 0.3
        );
        RenderUtils.drawFilledBox(condBB.expand(0.001, 0.001, 0.001), r, g, b, 30);
        RenderUtils.drawBoxOutline(condBB.expand(0.001, 0.001, 0.001), r, g, b, 85);

        for (int boxIndex = 0; boxIndex < lb.bb.length; boxIndex++) {
            AxisAlignedBB bb = lb.bb[boxIndex];
            double rMinX = bb.minX;
            double rMaxX = bb.maxX;
            double rMinZ = bb.minZ;
            double rMaxZ = bb.maxZ;

            if ((bb.maxX - bb.minX) > 0.35) {
                if (lb.xMinWall(boxIndex) != null) rMinX = Math.max(bb.minX, lb.xMinWall(boxIndex) + 0.3);
                if (lb.xMaxWall(boxIndex) != null) rMaxX = Math.min(bb.maxX, lb.xMaxWall(boxIndex) - 0.3);
            }

            if ((bb.maxZ - bb.minZ) > 0.35) {
                if (lb.zMinWall(boxIndex) != null) rMinZ = Math.max(bb.minZ, lb.zMinWall(boxIndex) + 0.3);
                if (lb.zMaxWall(boxIndex) != null) rMaxZ = Math.min(bb.maxZ, lb.zMaxWall(boxIndex) - 0.3);
            }

            if (rMinX >= rMaxX || rMinZ >= rMaxZ) {
                rMinX = bb.minX;
                rMaxX = bb.maxX;
                rMinZ = bb.minZ;
                rMaxZ = bb.maxZ;
            }

            AxisAlignedBB previewBB = new AxisAlignedBB(
                    rMinX - center.getX(), bb.minY - center.getY(), rMinZ - center.getZ(),
                    rMaxX - center.getX(), bb.maxY - center.getY(), rMaxZ - center.getZ()
            );

            RenderUtils.drawFilledBox(previewBB.expand(0.002, 0.002, 0.002), r, g, b, 120);
            RenderUtils.drawBoxOutline(previewBB.expand(0.002, 0.002, 0.002), r, g, b, 255);
        }

        lightBuf.clear();
        lightBuf.put(new float[]{0.4F, 0.4F, 0.4F, 1.0F}).flip();
        GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, lightBuf);

        RenderHelper.disableStandardItemLighting();

        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();

        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();

        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.disableDepth();
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (CyvClientConfig.getBoolean("lb3DPreview", true)) {
            int dWheel = Mouse.getEventDWheel();
            if (dWheel != 0) {
                zoom -= (dWheel > 0 ? 0.6F : -0.6F);
                zoom = Math.max(3.0F, Math.min(18.0F, zoom));
            }
        }
    }

    @Override
    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (CyvClientConfig.getBoolean("lb3DPreview", true) && isDraggingPreview && clickedMouseButton == 0) {
            float dx = mouseX - lastMouseX;
            float dy = mouseY - lastMouseY;
            previewYaw += dx * mouseSensitivity;
            previewPitch += dy * mouseSensitivity;
            previewPitch = Math.max(-89.0F, Math.min(89.0F, previewPitch));
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (state == 0) {
            isDraggingPreview = false;
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.targetTickField.isFocused()) {
            if (Character.isDigit(typedChar) || keyCode == 14 || keyCode == 211 || keyCode == 203 || keyCode == 205) {
                this.targetTickField.textboxKeyTyped(typedChar, keyCode);

                try {
                    String text = targetTickField.getText();
                    lb.targetTick = text.isEmpty() ? -1 : Integer.parseInt(text);
                } catch (Exception ignored) {}
            }

            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_ESCAPE) {
                targetTickField.setFocused(false);
            }
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(null);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseEvent) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseEvent);

        if (CyvClientConfig.getBoolean("lb3DPreview", true) && mouseEvent == 0 && mouseX < this.width - 165) {
            isDraggingPreview = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }

        if (mouseX >= this.width - 160 && mouseX <= this.width - 160 + 150 && mouseY >= 110 && mouseY <= 130) {
            this.targetTickField.setFocused(true);
        } else {
            this.targetTickField.setFocused(false);
        }

        if (this.landingModeButton.clicked(mouseX, mouseY, mouseEvent)) {
            LandingMode mode = lb.mode;
            if (mode.equals(LandingMode.landing)) {
                lb.mode = LandingMode.hit;
                this.landingModeButton.setText("Landing Mode: Hit");
            } else if (mode.equals(LandingMode.hit)) {
                lb.mode = LandingMode.z_neo;
                this.landingModeButton.setText("Landing Mode: Z Neo");
            } else if (mode.equals(LandingMode.z_neo)) {
                lb.mode = LandingMode.x_neo;
                this.landingModeButton.setText("Landing Mode: X Neo");
            } else if (mode.equals(LandingMode.x_neo)) {
                lb.mode = LandingMode.enter;
                this.landingModeButton.setText("Landing Mode: Enter");
            } else {
                lb.mode = LandingMode.landing;
                this.landingModeButton.setText("Landing Mode: Landing");
            }

            this.neoAndBlock.setEnabled(lb.mode == LandingMode.z_neo);
            return;
        }

        if (this.axisButton.clicked(mouseX, mouseY, mouseEvent)) {
            LandingAxis mode = lb.axis;
            if (mode.equals(LandingAxis.both)) {
                lb.axis = LandingAxis.z;
                this.axisButton.setText("Axis: Z");
            } else if (mode.equals(LandingAxis.z)) {
                lb.axis = LandingAxis.x;
                this.axisButton.setText("Axis: X");
            } else {
                lb.axis = LandingAxis.both;
                this.axisButton.setText("Axis: Both");
            }
            return;
        }

        if (this.bbVisibleButton.clicked(mouseX, mouseY, mouseEvent)) {
            CyvClientConfig.set("highlightLanding", !CyvClientConfig.getBoolean("highlightLanding", false));
            this.bbVisibleButton.setText("BB Visible: " + CyvClientConfig.getBoolean("highlightLanding", false));
            return;
        }

        if (this.condVisibleButton.clicked(mouseX, mouseY, mouseEvent)) {
            CyvClientConfig.set("highlightLandingCond", !CyvClientConfig.getBoolean("highlightLandingCond", false));
            this.condVisibleButton.setText("Cond Visible: " + CyvClientConfig.getBoolean("highlightLandingCond", false));
            return;
        }

        if (this.neoAndBlock.clicked(mouseX, mouseY, mouseEvent)) {
            lb.neoAndNormal = !lb.neoAndNormal;
            this.neoAndBlock.setText("Neo & Landing: " + lb.neoAndNormal);
            return;
        }

        if (this.ignoreWallHitButton.clicked(mouseX, mouseY, mouseEvent)) {
            lb.ignoreWallHit = !lb.ignoreWallHit;
            this.ignoreWallHitButton.setText("Ignore Wall: " + lb.ignoreWallHit);
            return;
        }
    }

    @Override
    public void updateScreen() {
        if (lb == null) mc.displayGuiScreen(null);
        if (this.targetTickField != null) this.targetTickField.updateCursorCounter();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }
}