package net.cyvforge.hud.nonlabels;

import net.cyvforge.CyvForge;
import net.cyvforge.config.CyvClientColorHelper;
import net.cyvforge.config.CyvClientConfig;
import net.cyvforge.event.events.ParkourTickListener;
import net.cyvforge.hud.LabelBundle;
import net.cyvforge.hud.structure.DraggableHUDElement;
import net.cyvforge.hud.structure.ScreenPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import java.text.DecimalFormat;

public class TurnHUD extends LabelBundle {
    FontRenderer font = Minecraft.getMinecraft().fontRendererObj;

    public TurnHUD() {
        final DraggableHUDElement master = new DraggableHUDElement() {
            @Override public String getName() { return "turnHUDMaster"; }
            @Override public String getDisplayName() { return "Turning HUD"; }
            @Override public int getWidth() { return 0; }
            @Override public int getHeight() { return 0; }
            @Override public ScreenPosition getDefaultPosition() { return new ScreenPosition(0, 0); }
            @Override public void render(ScreenPosition pos) {}
            @Override public void renderDummy(ScreenPosition pos) {}
        };
        master.isDraggable = false;
        this.labels.add(master);

        this.labels.add(new DraggableHUDElement() {
            @Override public String getName() { return "turnHUDSingle"; }
            @Override public String getDisplayName() { return " "; }

            @Override
            public int getWidth() {
                if (!master.isEnabled || CyvClientConfig.getBoolean("splitTurningHUD", false)) return 0;
                return getLabelWidth("12", true);
            }

            @Override
            public int getHeight() {
                if (!master.isEnabled || CyvClientConfig.getBoolean("splitTurningHUD", false)) return 0;
                int a = Math.max(1, Math.min(CyvClientConfig.getInt("turnHUDAngleMin", 1), 12));
                int b = Math.max(1, Math.min(CyvClientConfig.getInt("turnHUDAngleMax", 12), 12));
                int rows = Math.abs(max(a, b) - Math.min(a, b)) + 1;
                return getLabelHeight() * rows;
            }

            @Override public ScreenPosition getDefaultPosition() { return new ScreenPosition(250, 100); }

            @Override
            public void render(ScreenPosition pos) {
                if (!master.isEnabled || !this.isVisible || CyvClientConfig.getBoolean("splitTurningHUD", false)) return;
                renderList(pos, false);
            }

            @Override
            public void renderDummy(ScreenPosition pos) {
                if (!master.isEnabled || CyvClientConfig.getBoolean("splitTurningHUD", false)) return;
                renderList(pos, true);
            }

            private void renderList(ScreenPosition pos, boolean dummy) {
                long color1 = (dummy && !this.isVisible) ? 0xFFAAAAAA : CyvClientColorHelper.color1.getDrawColor();
                long color2 = (dummy && !this.isVisible) ? 0xFFAAAAAA : CyvClientColorHelper.color2.getDrawColor();
                DecimalFormat df = CyvForge.df;
                int a = Math.max(1, Math.min(CyvClientConfig.getInt("turnHUDAngleMin", 1), 12));
                int b = Math.max(1, Math.min(CyvClientConfig.getInt("turnHUDAngleMax", 12), 12));

                int renderIndex = 0;
                for (int i = Math.min(a, b) - 1; i < Math.max(a, b); i++) {
                    String labelText = (i + 1) + ": ";
                    String val = dummy ? "0.00000" : df.format(ParkourTickListener.formatYaw(ParkourTickListener.turningAngles[i]));
                    int yOffset = renderIndex * getLabelHeight();

                    drawString(labelText, pos.getAbsoluteX() + 1, pos.getAbsoluteY() + 1 + yOffset, color1);
                    drawString(val + "\u00B0", pos.getAbsoluteX() + 1 + font.getStringWidth(labelText), pos.getAbsoluteY() + 1 + yOffset, color2);
                    renderIndex++;
                }
            }
        });

        for (int i = 0; i < 12; i++) {
            final int tickIndex = i;
            final int tickNum = i + 1;

            this.labels.add(new DraggableHUDElement() {
                @Override
                public String getName() {
                    return "labelTurnAngle" + tickNum;
                }

                @Override
                public String getDisplayName() {
                    return " ";
                }

                @Override
                public ScreenPosition getDefaultPosition() {
                    return new ScreenPosition(250, 100 + (tickIndex * 9));
                }

                @Override
                public int getWidth() {
                    if (!master.isEnabled || !isWithinRange() || !CyvClientConfig.getBoolean("splitTurningHUD", false)) return 0;
                    return getLabelWidth(String.valueOf(tickNum), true) + 20;
                }

                @Override
                public int getHeight() {
                    if (!master.isEnabled || !isWithinRange() || !CyvClientConfig.getBoolean("splitTurningHUD", false)) return 0;
                    return getLabelHeight();
                }

                @Override
                public void render(ScreenPosition pos) {
                    if (!master.isEnabled || !this.isVisible || !isWithinRange() || !CyvClientConfig.getBoolean("splitTurningHUD", false)) return;
                    long color1 = CyvClientColorHelper.color1.getDrawColor();
                    long color2 = CyvClientColorHelper.color2.getDrawColor();
                    DecimalFormat df = CyvForge.df;

                    String angle = df.format(ParkourTickListener.formatYaw(ParkourTickListener.turningAngles[tickIndex]));
                    drawString(tickNum + ": ", pos.getAbsoluteX() + 1, pos.getAbsoluteY() + 1, color1);
                    drawString(angle + "\u00B0", pos.getAbsoluteX() + 1 + font.getStringWidth(tickNum + ": "),
                            pos.getAbsoluteY() + 1, color2);
                }

                @Override
                public void renderDummy(ScreenPosition pos) {
                    if (!master.isEnabled || !isWithinRange() || !CyvClientConfig.getBoolean("splitTurningHUD", false)) return;
                    long color1 = this.isVisible ? CyvClientColorHelper.color1.getDrawColor() : 0xFFAAAAAA;
                    long color2 = this.isVisible ? CyvClientColorHelper.color2.getDrawColor() : 0xFFAAAAAA;
                    drawString(tickNum + ": ", pos.getAbsoluteX() + 1, pos.getAbsoluteY() + 1, color1);
                    drawString("0.00000\u00B0", pos.getAbsoluteX() + 1 + font.getStringWidth(tickNum + ": "),
                            pos.getAbsoluteY() + 1, color2);
                }

                private boolean isWithinRange() {
                    int a = Math.max(1, Math.min(CyvClientConfig.getInt("turnHUDAngleMin", 1), 12));
                    int b = Math.max(1, Math.min(CyvClientConfig.getInt("turnHUDAngleMax", 12), 12));
                    return tickNum >= Math.min(a, b) && tickNum <= Math.max(a, b);
                }
            });
        }
    }

    public int getLabelWidth(String s, boolean angle) {
        font = Minecraft.getMinecraft().fontRendererObj;
        StringBuilder str = new StringBuilder(s + ": 000.");
        for (int i = 0; i< CyvClientConfig.getInt("df", 5); i++) str.append("0");
        if (angle) str.append("\u00B0");
        return font.getStringWidth(str.toString());
    }

    public int getLabelHeight() {
        return 9;
    }
    private int max(int a, int b) {
        return a > b ? a : b;
    }
}