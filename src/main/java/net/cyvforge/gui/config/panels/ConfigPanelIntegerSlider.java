package net.cyvforge.gui.config.panels;

import net.cyvforge.CyvForge;
import net.cyvforge.config.CyvClientConfig;
import net.cyvforge.gui.config.ConfigPanel;
import net.cyvforge.util.defaults.CyvGui;
import net.cyvforge.util.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;

import java.util.ArrayList;

public class ConfigPanelIntegerSlider implements ConfigPanel {
    public int sliderValue;
    public String configOption;
    public String displayString;
    public final int minValue;
    private final int maxValue;
    public final int index;
    public CyvGui screenIn;

    private int xPosition;
    private int yPosition;
    private int sizeX;
    private int sizeY;

    private boolean dragging = false;

    public ConfigPanelIntegerSlider(ArrayList<ConfigPanel> array, String configOption, String displayString, int minValue, int maxValue, CyvGui screenIn) {
        this.index = array.size();
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.displayString = displayString;
        this.configOption = configOption;
        this.screenIn = screenIn;

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        sizeX = screenIn.getSizeX()-20;
        sizeY = Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT*3/2;
        this.xPosition = sr.getScaledWidth()/2-screenIn.getSizeX()/2+10;
        this.yPosition = sr.getScaledHeight()/2-screenIn.getSizeY()/2+10 + (index * Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT * 2);
        this.sliderValue = CyvClientConfig.getInt(configOption, 0);
        this.sliderValue = MathHelper.clamp_int(this.sliderValue, minValue, maxValue);
    }

    @Override
    public void draw(int mouseX, int mouseY, int scroll) {
        boolean active = isEnabled();

        if (active && this.dragging) {
            if (org.lwjgl.input.Mouse.isButtonDown(0)) {
                this.mouseDragged(mouseX, mouseY + scroll);
            } else {
                this.dragging = false;
            }
        }

        int textColor = active ? 0xFFFFFFFF : 0xFF777777;
        int bgColor;
        int sliderColor;

        if (!active) {
            bgColor = 0x80555555;
            sliderColor = 0x80555555;
        } else {
            bgColor = this.mouseInBounds(mouseX, mouseY + scroll) ? CyvForge.theme.shade1 : CyvForge.theme.shade2;
            sliderColor = CyvForge.theme.mainBase();
        }

        //text label
        GuiUtils.drawString(this.displayString, this.xPosition, this.yPosition+this.sizeY/2-Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT/2+1-scroll, textColor, active);
        //bg
        GuiUtils.drawRoundedRect(this.xPosition+this.sizeX/2, this.yPosition-scroll, this.xPosition+this.sizeX, this.yPosition+this.sizeY-scroll, 3, bgColor);
        //slider
        float valRatio = (float)(sliderValue - minValue) / (maxValue - minValue);
        int thumbX = this.xPosition + this.sizeX / 2 + (int)((sizeX / 2) * valRatio);

        GuiUtils.drawRoundedRect(thumbX - 3, this.yPosition-1-scroll,
                thumbX + 3, this.yPosition+this.sizeY+1-scroll, 1, sliderColor);
        //amount
        GuiUtils.drawCenteredString(" "+this.sliderValue, this.xPosition+this.sizeX*3/4, this.yPosition+this.sizeY/2-Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT/2+1-scroll, textColor, active);

    }

    @Override
    public void setPos(int x, int y, int width) {
        this.xPosition = x;
        this.yPosition = y;
        this.sizeX = width;
    }

    @Override public int getIndex() {
        return this.index;
    }

    @Override
    public void mouseDragged(int mouseX, int mouseY) {
        if (!isEnabled()) return;

        this.sliderValue = (int)((mouseX+2-(this.xPosition+this.sizeX/2))/(float)(this.sizeX/2) * (this.maxValue - this.minValue)) + this.minValue;
        this.sliderValue = (int) MathHelper.clamp_double(this.sliderValue, this.minValue, this.maxValue);
        CyvClientConfig.set(this.configOption, this.sliderValue);
        onValueChange();
    }

    @Override
    public boolean mouseInBounds(int mouseX, int mouseY) {
        if (isEnabled() && mouseX > this.xPosition+this.sizeX/2 && mouseY > this.yPosition
                && mouseX < this.xPosition+this.sizeX && mouseY < this.yPosition+this.sizeY) return true;
        return false;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseInBounds(mouseX, mouseY) && mouseButton == 0) {
            this.dragging = true;
            mouseDragged(mouseX, mouseY);
        }
    }


    @Override
    public void keyTyped(char typedChar, int keyCode) {
        // TODO Auto-generated method stub

    }

    @Override
    public void save() {
        CyvClientConfig.set(this.configOption, this.sliderValue);
    }


}