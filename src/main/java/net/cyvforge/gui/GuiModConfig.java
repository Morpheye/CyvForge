package net.cyvforge.gui;

import net.cyvforge.CyvForge;
import net.cyvforge.config.ColorTheme;
import net.cyvforge.config.CyvClientColorHelper;
import net.cyvforge.config.CyvClientConfig;
import net.cyvforge.event.events.GuiHandler;
import net.cyvforge.gui.config.ConfigPanel;
import net.cyvforge.util.PanelUtils;
import net.cyvforge.util.defaults.CyvGui;
import net.cyvforge.util.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;

public class GuiModConfig extends CyvGui {
    public int sizeX = 350;
    public int sizeY = 175;
    ArrayList<ConfigPanel> panels = new ArrayList<ConfigPanel>();
    ConfigPanel selectedPanel;
    ScaledResolution sr;
    SubButton backButton;

    ColorTheme theme;
    float vScroll = 0;
    float scroll = 0;
    int maxScroll = 0;
    boolean scrollClicked = false;

    protected final boolean fromLabels;

    public GuiModConfig(boolean fromLabels) {
        super("Mod Config");
        mc = Minecraft.getMinecraft();
        sr = new ScaledResolution(mc);
        fontRendererObj = mc.fontRendererObj;
        this.fromLabels = fromLabels;

        this.backButton = new SubButton("Back", sr.getScaledWidth()/2-sizeX/2-4, sr.getScaledHeight()/2-sizeY/2-21);
        this.theme = CyvForge.theme;

        this.updatePanels();

        maxScroll = (int) Math.max(0, fontRendererObj.FONT_HEIGHT * 2 * Math.ceil(panels.size()) - (sizeY-20));
        if (scroll > maxScroll) scroll = maxScroll;
        if (scroll < 0) scroll = 0;
    }

    @Override
    public void initGui() {
    }

    @Override
    public void onResize(Minecraft mcIn, int w, int h) {
        mc.displayGuiScreen(null);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        //background
        this.drawDefaultBackground();
        this.theme = CyvForge.theme;

        //draw the menu background
        GuiUtils.drawRoundedRect(sr.getScaledWidth()/2-sizeX/2-4, sr.getScaledHeight()/2-sizeY/2-4,
                sr.getScaledWidth()/2+sizeX/2+14, sr.getScaledHeight()/2+sizeY/2+4, 10, theme.background1);

        //buttons
        this.backButton.draw(mouseX, mouseY);

        //begin scissoring (I am a very mature individual who does not have a dirty mind)
        int centerx = sr.getScaledWidth() * sr.getScaleFactor() / 2;
        int centery = sr.getScaledHeight() * sr.getScaleFactor() / 2;
        int scaleFactor = sr.getScaleFactor();
        GL11.glScissor(centerx - (sizeX*scaleFactor/2), centery - (sizeY*scaleFactor/2), sizeX*scaleFactor, sizeY*scaleFactor);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        for (ConfigPanel p : this.panels) {
            p.draw(mouseX, mouseY + (int)scroll, (int)scroll);
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        //draw scrollbar
        int scrollbarHeight = (int) ((sizeY - 8)/(0.01*maxScroll+1));
        if (scroll > maxScroll) scroll = maxScroll;
        if (scroll < 0) scroll = 0;

        int top = sr.getScaledHeight()/2-sizeY/2+4;
        int bottom = sr.getScaledHeight()/2+sizeY/2-4 - scrollbarHeight;
        int amount = (int) (top + (bottom - top) * ((float) scroll/maxScroll));

        if (maxScroll == 0) amount = top;

        //color
        int color = theme.border2;
        if (mouseX > sr.getScaledWidth()/2+sizeX/2+2 && mouseX < sr.getScaledWidth()/2+sizeX/2+8 &&
                mouseY > amount && mouseY < amount+scrollbarHeight) {
            color = theme.border1;
        }

        GuiUtils.drawRoundedRect(sr.getScaledWidth()/2+sizeX/2+2, amount,
                sr.getScaledWidth()/2+sizeX/2+8, amount+scrollbarHeight, 3, color);
    }

    @Override
    public void updateScreen() {
        if (this.selectedPanel != null) this.selectedPanel.update();

        //smooth scrolling
        this.scroll += this.vScroll;
        this.vScroll *= 0.75;

        if (scroll > maxScroll) scroll = maxScroll;
        if (scroll < 0) scroll = 0;
    }

    private void updatePanels() {
        this.panels.clear();

        // globals
        PanelUtils.addSwitcher(panels, "color1", "Color 1", CyvClientColorHelper.colorStrings, this, () -> {
            CyvClientColorHelper.setColor1(CyvClientConfig.getString("color1", "aqua"));
        });
        PanelUtils.addSwitcher(panels, "color2", "Color 2", CyvClientColorHelper.colorStrings, this, () -> {
            CyvClientColorHelper.setColor2(CyvClientConfig.getString("color2", "aqua"));
        });
        PanelUtils.addSwitcher(panels, "theme", "Color Theme", ColorTheme.getThemes(), this, () -> {
            CyvForge.theme = ColorTheme.valueOf(CyvClientConfig.getString("theme", "CYVISPIRIA"));
        });
        PanelUtils.addString(panels, "chatPrefix", "Chat Prefix", this);
        PanelUtils.addToggle(panels, "whiteChat", "Color2 always white in chat", this);
        PanelUtils.addSlider(panels, "df", "Decimal Precision", 1, 16, this, () -> {
            CyvForge.df.setMaximumFractionDigits(CyvClientConfig.getInt("df", 5));
        });
        PanelUtils.addToggle(panels, "trimZeroes", "Trim Zeroes", this, () -> {
            if (CyvClientConfig.getBoolean("trimZeroes", true)) {
                CyvForge.df.setMinimumFractionDigits(0);
            } else {
                CyvForge.df.setMinimumFractionDigits(CyvClientConfig.getInt("df", 5));
            }
        });

        // mpk
        PanelUtils.addSpace(panels, this);
        PanelUtils.addToggle(panels, "sendLbChatOffset", "Send Landing Offset", this);
        PanelUtils.addToggle(panels, "sendMmChatOffset", "Send Momentum Offset", this);
        PanelUtils.addToggle(panels, "highlightLanding", "Highlight Landing Blocks", this);
        PanelUtils.addToggle(panels, "highlightLandingCond", "Highlight Landing Conditions", this);
        PanelUtils.addToggle(panels, "momentumPbCancelling", "Momentum PB Cancelling", this);

        // macro
        PanelUtils.addSpace(panels, this);
        PanelUtils.addToggle(panels, "smoothMacro", "Smooth Macro", this);
        PanelUtils.addToggle(panels, "macroShortcutsRequireMod", "Shortcuts Require TAB/ALT", this);
        PanelUtils.addSwitcher(panels, "macroHUDColor", "Macro HUD Color", new String[] {"Default", "Color1", "Color2"}, this);
        PanelUtils.addToggle(panels, "macroClipEnabled", "Macro Clip", this);
        PanelUtils.addDependantSlider(panels, "macroClipTicks", "Number of ticks to clip", 1, 500, "macroClipEnabled", this);

        // inertia
        PanelUtils.addSpace(panels, this);
        PanelUtils.addToggle(panels, "inertiaEnabled", "Inertia Listener Enabled", this);
        PanelUtils.addDependantSlider(panels, "inertiaTick", "Air tick", 1, 12, "inertiaEnabled", this);
        PanelUtils.addDependantDecimal(panels, "inertiaMin", "Min Speed", "inertiaEnabled", this);
        PanelUtils.addDependantDecimal(panels, "inertiaMax", "Max Speed", "inertiaEnabled", this);
        PanelUtils.addDependantSwitcher(panels, "inertiaAxis", "Inertia Axis", new Character[] {'x', 'z'}, "inertiaEnabled", this);
        PanelUtils.addDependantSwitcher(panels, "inertiaGroundType", "Ground Type", new String[] {"normal", "ice", "slime"}, "inertiaEnabled", this);

        // position checker
        PanelUtils.addSpace(panels, this);
        PanelUtils.addToggle(panels, "positionCheckerEnabled", "Position Checker Enabled", this);
        PanelUtils.addDependantSlider(panels, "positionCheckerTick", "Air tick", 1, 12, "positionCheckerEnabled", this);
        PanelUtils.addDependantDecimal(panels, "positionCheckerRadius", "Radius", "positionCheckerEnabled", this);
        PanelUtils.addDependantAction(panels, "Copy current position with radius", () -> {
            net.cyvforge.command.CommandPositionChecker.setMark();
            this.updatePanels();
        }, "positionCheckerEnabled", this) ;
        PanelUtils.addDependantDecimal(panels, "positionCheckerMinX", "Min X", "positionCheckerEnabled", this);
        PanelUtils.addDependantDecimal(panels, "positionCheckerMaxX", "Max X", "positionCheckerEnabled", this);
        PanelUtils.addDependantDecimal(panels, "positionCheckerMinZ", "Min Z", "positionCheckerEnabled", this);
        PanelUtils.addDependantDecimal(panels, "positionCheckerMaxZ", "Max Z", "positionCheckerEnabled", this);
        PanelUtils.addDependantToggle(panels, "positionCheckerZNeo", "Z Neo Mode", "positionCheckerEnabled", this);

        // checkpoints
        PanelUtils.addSpace(panels, this);
        PanelUtils.addToggle(panels, "antiCP", "Anti-Checkpoint", this);
        PanelUtils.addDependantSlider(panels, "antiCPDelay", "Anti-CP Delay (s)", 1, 10, "antiCP", this);
        PanelUtils.addToggle(panels, "singleplayerCheckpointsEnabled", "Custom Checkpoints Enabled", this);
        PanelUtils.addSlider(panels, "generatorDyeColor", "Generator Dye Color", 0, 15, this);
        PanelUtils.addSlider(panels, "generatorItemSlot", "Generator Hotbar Slot", 0, 8, this);

        maxScroll = (int) Math.max(0, fontRendererObj.FONT_HEIGHT * 2 * Math.ceil(panels.size()) - (sizeY-20));
        if (scroll > maxScroll) scroll = maxScroll;
        if (scroll < 0) scroll = 0;
    }

    @Override
    public void mouseClickMove(int x, int y, int mouseButton, long time) {
        if (this.scrollClicked) {
            int scrollbarHeight = (int) ((sizeY - 8)/(0.01*maxScroll+1));
            int top = sr.getScaledHeight()/2-sizeY/2+4;
            int bottom = sr.getScaledHeight()/2+sizeY/2-4 - scrollbarHeight;

            scroll = (int) ((float) (y - (sr.getScaledHeight()/2-this.sizeY/2) - scrollbarHeight/2) /(bottom - top) * maxScroll);

            if (scroll > maxScroll) scroll = maxScroll;
            if (scroll < 0) scroll = 0;
        }

        if (this.selectedPanel != null) {
            this.selectedPanel.mouseDragged(x, y);
        }


    }

    @Override
    public void handleMouseInput() {
        int eventDWheel = GuiHandler.scrollBuffer;
        GuiHandler.scrollBuffer = 0;

        if (eventDWheel != 0 && (!scrollClicked || !org.lwjgl.input.Mouse.isButtonDown(0))) {
            vScroll -= eventDWheel * 0.05;
        }

        try {
            super.handleMouseInput();
        } catch (IOException e) {}
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int scrollbarHeight = (int) ((sizeY - 8)/(0.01*maxScroll+1));
        int top = sr.getScaledHeight()/2-sizeY/2+4;
        int bottom = sr.getScaledHeight()/2+sizeY/2-4 - scrollbarHeight;
        int amount = (int) (top + (bottom - top) * ((float) scroll/maxScroll));

        if (mouseX > sr.getScaledWidth()/2+sizeX/2+2 && mouseX < sr.getScaledWidth()/2+sizeX/2+8 &&
                mouseY > amount && mouseY < amount+scrollbarHeight) {
            this.scrollClicked = true;
            return;
        } else {
            this.scrollClicked = false;
        }

        if (this.backButton.clicked(mouseX, mouseY, mouseButton)) {
            if (this.selectedPanel != null) this.selectedPanel.unselect();

            if (fromLabels) Minecraft.getMinecraft().displayGuiScreen(new GuiMPK());
            else Minecraft.getMinecraft().displayGuiScreen(null);
            return;
        }

        if (mouseX < sr.getScaledWidth()/2-sizeX/2-4 || mouseX > sr.getScaledWidth()/2+sizeX/2+14 ||
                mouseY < sr.getScaledHeight()/2-sizeY/2-4 || mouseY > sr.getScaledHeight()/2+sizeY/2+4) {
            if (this.selectedPanel != null) this.selectedPanel.unselect();

            this.selectedPanel = null;
            return;
        }

        for (ConfigPanel p : this.panels) {
            if (p.isEnabled() && p.mouseInBounds(mouseX, mouseY+(int)scroll)) {
                if (this.selectedPanel != null && this.selectedPanel != p) this.selectedPanel.unselect();

                p.mouseClicked(mouseX, mouseY+(int)scroll, mouseButton);
                this.selectedPanel = p;
                p.select();
                return;
            }
        }

        if (this.selectedPanel != null) {
            this.selectedPanel.unselect();
        }
        this.selectedPanel = null;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) { //exit the gui
            if (fromLabels) Minecraft.getMinecraft().displayGuiScreen(new GuiMPK());
            else Minecraft.getMinecraft().displayGuiScreen(null);
            return;
        }

        if (this.selectedPanel != null) this.selectedPanel.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        for (ConfigPanel p : this.panels) p.save();
        this.updatePanels();

    }

    class SubButton {
        String text;
        int x, y;
        int sizeX = 80;
        int sizeY = 15;

        SubButton(String text, int x, int y) {
            this.text = text;
            this.x = x;
            this.y = y;
        }

        void draw(int mouseX, int mouseY) {
            boolean mouseDown = (mouseX > x && mouseX < x + sizeX && mouseY > y && mouseY < y + sizeY);
            GuiUtils.drawRoundedRect(x, y, x+sizeX, y+sizeY, 5, mouseDown ? theme.highlight : theme.background1);
            GuiUtils.drawCenteredString(this.text, x+sizeX/2, y+sizeY/2-fontRendererObj.FONT_HEIGHT/2, 0xFFFFFFFF, true);
        }

        boolean clicked(double mouseX, double mouseY, int mouseButton) {
            if (!(mouseX > x && mouseX < x+sizeX && mouseY > y && mouseY < y+sizeY && mouseButton == 0)) return false;
            else return true;
        }

    }

    @Override public int getSizeX() { return this.sizeX; }
    @Override public int getSizeY() { return this.sizeY; }
}
