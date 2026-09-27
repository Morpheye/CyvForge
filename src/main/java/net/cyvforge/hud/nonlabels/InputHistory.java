package net.cyvforge.hud.nonlabels;

import net.cyvforge.CyvForge;
import net.cyvforge.config.CyvClientColorHelper;
import net.cyvforge.config.CyvClientConfig;
import net.cyvforge.event.events.InputHistoryManager;
import net.cyvforge.hud.structure.DraggableHUDElement;
import net.cyvforge.hud.structure.ScreenPosition;
import net.cyvforge.util.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import java.text.DecimalFormat;

public class InputHistory extends DraggableHUDElement {

    @Override
    public String getName() { return "InputHistory"; }

    @Override
    public String getDisplayName() { return "Input History"; }

    @Override
    public boolean enabledByDefault() { return false; }

    @Override
    public ScreenPosition getDefaultPosition() { return new ScreenPosition(10, 50); }

    private float getScale() {
        return CyvClientConfig.getInt("ih_size", 100) / 100.0f;
    }

    private int getLabelHeight() {
        return Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT + 2;
    }

    private int getDynamicColumnWidth(String prefix, String suffix) {
        StringBuilder dummy = new StringBuilder(prefix);
        dummy.append("-180.");
        for (int i = 0; i < CyvClientConfig.getInt("df", 5); i++) dummy.append("0");
        dummy.append(suffix);
        return Minecraft.getMinecraft().fontRendererObj.getStringWidth(dummy.toString());
    }

    @Override
    public int getWidth() {
        FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
        int baseWidth = font.getStringWidth("999-999t ") + font.getStringWidth("W A S D Jmp Spr Snk  ");

        if (CyvClientConfig.getBoolean("ih_airGroundIndicator", false) && CyvClientConfig.getBoolean("ih_groundJumpReset", true)) {
            baseWidth += font.getStringWidth("G ");
        }
        if (CyvClientConfig.getBoolean("ih_trackFacing", false)) {
            baseWidth += getDynamicColumnWidth("", "\u00B0  ");
        }
        if (CyvClientConfig.getBoolean("ih_trackTurnAmount", false)) {
            baseWidth += getDynamicColumnWidth("(", "\u00B0)  ");
        }
        return (int) (baseWidth * getScale());
    }

    @Override
    public int getHeight() {
        int maxRows = CyvClientConfig.getInt("ih_historyLength", 20);
        int baseHeight = Math.max(1, maxRows) * getLabelHeight();
        return (int) (baseHeight * getScale());
    }

    @Override
    public void render(ScreenPosition pos) {
        if (!this.isVisible) return;
        renderHistory(pos, false);
    }

    @Override
    public void renderDummy(ScreenPosition pos) {
        renderHistory(pos, true);
    }

    private void renderHistory(ScreenPosition pos, boolean dummy) {
        FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
        DecimalFormat df = CyvForge.df;
        float scale = getScale();

        boolean invert = CyvClientConfig.getBoolean("ih_invertList", false);
        boolean airGround = CyvClientConfig.getBoolean("ih_airGroundIndicator", false) && CyvClientConfig.getBoolean("ih_groundJumpReset", true);
        boolean trackFacing = CyvClientConfig.getBoolean("ih_trackFacing", false);
        boolean trackTurn = CyvClientConfig.getBoolean("ih_trackTurnAmount", false);
        boolean pressRelease = CyvClientConfig.getBoolean("ih_pressReleaseTicks", false) && CyvClientConfig.getBoolean("ih_groundJumpReset", true);

        String colorMode = CyvClientConfig.getString("ih_colorMode", "Color1 - Color2");
        int color1, color2;
        if (!this.isVisible) {
            color1 = 0xFFAAAAAA;
            color2 = 0xFFAAAAAA;
        } else {
            if (colorMode.equals("White")) {
                color1 = 0xFFFFFFFF;
                color2 = 0xFFFFFFFF;
            } else if (colorMode.equals("Color2 - Color1")) {
                color1 = (int) CyvClientColorHelper.color2.getDrawColor();
                color2 = (int) CyvClientColorHelper.color1.getDrawColor();
            } else {
                color1 = (int) CyvClientColorHelper.color1.getDrawColor();
                color2 = (int) CyvClientColorHelper.color2.getDrawColor();
            }
        }

        int maxRows = CyvClientConfig.getInt("ih_historyLength", 20);
        int baseWidth = (int)(getWidth() / scale);
        int baseHeight = maxRows * getLabelHeight();
        GlStateManager.pushMatrix();
        GlStateManager.translate(pos.getAbsoluteX(), pos.getAbsoluteY(), 0);
        GlStateManager.scale(scale, scale, 1.0f);

        GuiUtils.drawRoundedRect(-3, -3, baseWidth + 3, baseHeight + 1, 3, 0x40000000);

        if (!dummy && InputHistoryManager.history.isEmpty()) {
            GlStateManager.popMatrix();
            return;
        }

        int size = dummy ? Math.min(5, maxRows) : InputHistoryManager.history.size();
        if (size == 0) return;

        for (int i = 0; i < size; i++) {
            int yOffset = invert ? (baseHeight - getLabelHeight() - (i * getLabelHeight())) : (i * getLabelHeight());

            int historyIndex = dummy ? i : (size - 1 - i);
            InputHistoryManager.HistorySection sec = dummy ? null : InputHistoryManager.history.get(historyIndex);

            int currentX = 0;

            boolean isAir = dummy ? (i % 2 == 0) : sec.isAir;
            String inputs = dummy ? "W A Spr" : sec.getInputsString();
            float yawVal = dummy ? 45.5f : sec.yaw;
            float turnVal = dummy ? 12.3f : sec.turnAmount;

            String ticksText;
            if (dummy) {
                ticksText = pressRelease ? ((i * 3 + 1) + "-" + (i * 3 + 4) + "t") : ((i * 3 + 1) + "t");
            } else {
                if (pressRelease) {
                    if (sec.startTick == sec.endTick) {
                        ticksText = sec.startTick + "t";
                    } else {
                        ticksText = sec.startTick + "-" + sec.endTick + "t";
                    }
                } else {
                    ticksText = sec.ticks + "t";
                }
            }

            if (airGround) {
                String ag = isAir ? "A " : "G ";
                int agColor = isAir ? 0xFFFF5555 : 0xFF55FF55;
                font.drawStringWithShadow(ag, currentX, yOffset, agColor);
                currentX += font.getStringWidth("G ");
            }

            font.drawStringWithShadow(ticksText, currentX, yOffset, color1);
            currentX += font.getStringWidth("999-999t ");

            font.drawStringWithShadow(inputs, currentX, yOffset, color2);
            currentX += font.getStringWidth("W A S D Jmp Spr Snk  ");

            if (trackFacing) {
                String facingStr = df.format(net.cyvforge.event.events.ParkourTickListener.formatYaw(yawVal)) + "\u00B0";
                font.drawStringWithShadow(facingStr, currentX, yOffset, color2);
                currentX += getDynamicColumnWidth("", "\u00B0  ");
            }

            if (trackTurn) {
                String sign = turnVal > 0 ? "+" : "";
                String turnStr = "(" + sign + df.format(turnVal) + "\u00B0)";
                font.drawStringWithShadow(turnStr, currentX, yOffset, color2);
            }
        }

        GlStateManager.popMatrix();
    }
}