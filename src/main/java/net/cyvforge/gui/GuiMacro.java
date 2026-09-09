package net.cyvforge.gui;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import net.cyvforge.CyvForge;
import net.cyvforge.command.mpk.CommandMacro;
import net.cyvforge.config.CyvClientConfig;
import net.cyvforge.event.MacroFileInit;
import net.cyvforge.util.defaults.CyvGui;
import net.cyvforge.util.GuiUtils;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import net.minecraft.client.gui.GuiTextField;

public class GuiMacro extends CyvGui {
    int sizeX = 500;
    int sizeY = 300;

    private boolean focusFileNameOnInit = false;

    SubButton addRow;
    SubButton duplicateRow;
    SubButton deleteRow;

    GuiTextField fileName;
    SubButton loadFile;

    SubButton openList;

    public java.util.List<MacroLine> macroLines;
    int selectedIndex = -1;
    private int focusedColumn = 0;

    private MacroLine clipboard = null;
    private final java.util.List<java.util.List<MacroLine>> undoStack = new java.util.ArrayList<>();
    private static final int MAX_UNDO_STEPS = 50;

    ArrayList<String> recentMacros = new ArrayList<>();

    int deleteConfirmIndex = -1;

    float vScroll = 0;
    float scroll = 0;
    int maxScroll = 0;
    boolean scrollClicked = false;

    public GuiMacro() {
        super("Macro GUI");
    }

    @Override
    public void onResize(Minecraft mcIn, int w, int h) {
        mc.displayGuiScreen(null);
    }

    @Override
    public void initGui() { //initialize the macro
        ArrayList<ArrayList<String>> macro;
        this.macroLines = new ArrayList<>();

        Keyboard.enableRepeatEvents(true);

        this.addRow = new SubButton("Add Row", (sr.getScaledWidth() + sizeY +30)/2 - 75, sr.getScaledHeight()/2 - sizeY/2 + 40);
        this.duplicateRow = new SubButton("Duplicate Row", (sr.getScaledWidth() + sizeY +30)/2 - 75, sr.getScaledHeight()/2 - sizeY/2 + 60);
        this.deleteRow = new SubButton("Delete Row", (sr.getScaledWidth() + sizeY +30)/2 - 75, sr.getScaledHeight()/2 - sizeY/2 + 80);

        this.fileName = new GuiTextField(0, fontRendererObj, (sr.getScaledWidth() + sizeY +30)/2 - 72, sr.getScaledHeight()/2 - sizeY/2+fontRendererObj.FONT_HEIGHT/2 + 20, 90, fontRendererObj.FONT_HEIGHT*2);
        fileName.setEnableBackgroundDrawing(false);
        fileName.setText(CyvClientConfig.getString("currentMacro", "macro"));
        this.loadFile = new SubButton("Load", (sr.getScaledWidth() + sizeY +30)/2 + 25, sr.getScaledHeight()/2 - sizeY/2 + 20);
        loadFile.setSizeX(50);
        loadFile.setEnabled(true);

        this.macroLines.clear();
        try {
            MacroFileInit.swapFile(CyvClientConfig.getString("currentMacro", "macro"));
            Gson gson = new Gson();

            if (MacroFileInit.macroFile.exists()) {
                try (FileReader fr = new FileReader(MacroFileInit.macroFile);
                     JsonReader reader = new JsonReader(fr)) {
                    macro = gson.fromJson(reader, new ArrayList<ArrayList<String>>().getClass());
                }
            } else {
                macro = new ArrayList<ArrayList<String>>();
            }
        } catch (Exception e) {
            macro = new ArrayList<ArrayList<String>>();
        }

        if (macro != null) {
            try {
                for (ArrayList<String> line : macro) {
                    try {
                        MacroLine macroLine = new MacroLine();

                        macroLine.w = Boolean.valueOf(line.get(0));
                        macroLine.a = Boolean.valueOf(line.get(1));
                        macroLine.s = Boolean.valueOf(line.get(2));
                        macroLine.d = Boolean.valueOf(line.get(3));
                        macroLine.jump = Boolean.valueOf(line.get(4));
                        macroLine.sprint = Boolean.valueOf(line.get(5));
                        macroLine.sneak = Boolean.valueOf(line.get(6));
                        //macroLine.rmb = Boolean.valueOf(line.get(7));

                        //macroLine.yawField.setText(""+Double.valueOf(line.get(8)));
                        //macroLine.pitchField.setText(""+Double.valueOf(line.get(9)));

                        // transisition fix for when i added RMB
                        if (line.size() == 9) {
                            macroLine.rmb = false;

                            double yVal = Double.valueOf(line.get(7));
                            double pVal = Double.valueOf(line.get(8));
                            macroLine.yawField.setText(yVal == 0.0 ? "" : String.valueOf(yVal));
                            macroLine.pitchField.setText(pVal == 0.0 ? "" : String.valueOf(pVal));
                        } else {
                            macroLine.rmb = Boolean.valueOf(line.get(7));

                            double yVal = Double.valueOf(line.get(8));
                            double pVal = Double.valueOf(line.get(9));
                            macroLine.yawField.setText(yVal == 0.0 ? "" : String.valueOf(yVal));
                            macroLine.pitchField.setText(pVal == 0.0 ? "" : String.valueOf(pVal));
                        }

                        macroLines.add(macroLine);

                    } catch (Exception e) {}
                }
            } catch (Exception e) {}
        }

        String rawRecent = CyvClientConfig.getString("recentMacros", "");
        recentMacros.clear();
        if (!rawRecent.isEmpty()) {
            recentMacros.addAll(Arrays.asList(rawRecent.split(",")));
        }

        int recentX = (sr.getScaledWidth() + sizeY + 30) / 2 - 75;
        int recentY = sr.getScaledHeight() / 2 - sizeY / 2 + 115;
        this.openList = new SubButton("List", recentX + 90, recentY - 15);
        this.openList.setSizeX(60);
        this.openList.setEnabled(true);

        maxScroll = (int) Math.max(0, Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT * 2 * Math.ceil(macroLines.size()) - (sizeY-20));
        if (scroll > maxScroll) scroll = maxScroll;
        if (scroll < 0) scroll = 0;

        if (this.focusFileNameOnInit) {
            this.fileName.setFocused(true);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawDefaultBackground();

        int listHeight = sizeY - 40;
        maxScroll = (int) Math.max(0, (macroLines.size() * fontRendererObj.FONT_HEIGHT * 2) - listHeight);
        if (scroll > maxScroll) scroll = maxScroll;
        if (scroll < 0) scroll = 0;

        GuiUtils.drawRoundedRect(sr.getScaledWidth()/2 - sizeX/2 - 15, sr.getScaledHeight()/2 - sizeY/2 - 4,
                sr.getScaledWidth()/2 + sizeX/2 + 14, sr.getScaledHeight()/2 + sizeY/2 + 4, 5, CyvForge.theme.background1);

        this.addRow.draw(mouseX, mouseY);
        this.duplicateRow.draw(mouseX, mouseY);
        this.deleteRow.draw(mouseX, mouseY);
        this.openList.draw(mouseX, mouseY);

        GuiUtils.drawRoundedRect((sr.getScaledWidth() + sizeY +30)/2 - 75, sr.getScaledHeight()/2 - sizeY/2 + 20,
                (sr.getScaledWidth() + sizeY +30)/2 + 20, sr.getScaledHeight()/2 - sizeY/2 + 20 + fontRendererObj.FONT_HEIGHT*7/4,
                3, CyvForge.theme.shade2);
        this.fileName.drawTextBox();
        this.loadFile.draw(mouseX, mouseY);

        int recentX = (sr.getScaledWidth() + sizeY + 30) / 2 - 75;
        int recentY = sr.getScaledHeight() / 2 - sizeY / 2 + 115;
        int recentWidth = 150;

        GuiUtils.drawString("Recent Macros:", recentX, recentY - 12, 0xFF7FB6C4, true);

        for (int i = 0; i < recentMacros.size(); i++) {
            String mName = recentMacros.get(i);
            int rowY = recentY + (i * 18);

            boolean rowHovered = mouseX >= recentX && mouseX <= recentX + recentWidth && mouseY >= rowY && mouseY <= rowY + 15;

            GuiUtils.drawRoundedRect(recentX, rowY, recentX + recentWidth, rowY + 15, 2, rowHovered ? 0x60FFFFFF : 0x30000000);
            fontRendererObj.drawString(mName, recentX + 5, rowY + 4, 0xFFFFFFFF);

            if (rowHovered || deleteConfirmIndex == i) {
                int btnCopyX = recentX + recentWidth - 36;
                int btnDelX = recentX + recentWidth - 18;

                if (deleteConfirmIndex == i) {
                    boolean confirmHover = mouseX >= btnCopyX && mouseX <= btnDelX + 15 && mouseY >= rowY + 1 && mouseY <= rowY + 14;
                    GuiUtils.drawRoundedRect(btnCopyX, rowY + 1, btnDelX + 15, rowY + 14, 2, confirmHover ? 0xFFFF0000 : 0xFFCC0000);
                    fontRendererObj.drawString("Sure?", btnCopyX + 2, rowY + 4, 0xFFFFFFFF);
                } else {
                    boolean copyHover = mouseX >= btnCopyX && mouseX <= btnCopyX + 15 && mouseY >= rowY + 1 && mouseY <= rowY + 14;
                    if (copyHover) GuiUtils.drawRoundedRect(btnCopyX, rowY + 1, btnCopyX + 15, rowY + 14, 2, 0x807FB6C4);
                    fontRendererObj.drawString("C", btnCopyX + 5, rowY + 4, copyHover ? 0xFF00FFFF : 0xAAFFFFFF);

                    boolean delHover = mouseX >= btnDelX && mouseX <= btnDelX + 15 && mouseY >= rowY + 1 && mouseY <= rowY + 14;
                    if (delHover) GuiUtils.drawRoundedRect(btnDelX, rowY + 1, btnDelX + 15, rowY + 14, 2, 0x80FF0000);
                    fontRendererObj.drawString("X", btnDelX + 5, rowY + 4, delHover ? 0xFFFF0000 : 0xAAFFFFFF);
                }
            }
        }

        this.addRow.setEnabled(true);
        if (this.selectedIndex > -1 && this.selectedIndex < this.macroLines.size()) {
            this.duplicateRow.setEnabled(true);
            this.deleteRow.setEnabled(true);
        } else {
            this.duplicateRow.setEnabled(false);
            this.deleteRow.setEnabled(false);
        }


        int centerx = sr.getScaledWidth() * sr.getScaleFactor() / 2;
        int centery = sr.getScaledHeight() * sr.getScaleFactor() / 2;
        int scaleFactor = sr.getScaleFactor();

        GuiUtils.drawString("Inputs:", sr.getScaledWidth()/2 - sizeX/2 + 13, 5 + sr.getScaledHeight()/2 - sizeY/2, 0xFFFFFFFF, true);
        GuiUtils.drawString("Yaw:", sr.getScaledWidth()/2 - 34, 5 + sr.getScaledHeight()/2 - sizeY/2, 0xFFFFFFFF, true);
        GuiUtils.drawString("Pitch:", sr.getScaledWidth()/2 - 1, 5 + sr.getScaledHeight()/2 - sizeY/2, 0xFFFFFFFF, true);

        GL11.glScissor(centerx - ((sizeX + 20)*scaleFactor/2),
                centery - (sizeY*scaleFactor/2) + 3,
                sizeX*scaleFactor, sizeY*scaleFactor - (fontRendererObj.FONT_HEIGHT * scaleFactor * 2));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        int index = 0;
        for (MacroLine l : macroLines) {
            int yHeight = (int) ((index + 1) * mc.fontRendererObj.FONT_HEIGHT*2 - scroll + (sr.getScaledHeight()/2 - sizeY/2));
            GuiUtils.drawString(""+(index+1), sr.getScaledWidth()/2 - sizeX/2 - 10,
                    yHeight + fontRendererObj.FONT_HEIGHT*2/3, 0xFFFFFFFF);
            l.drawEntry(index, (int) scroll, mouseX, mouseY, index == this.selectedIndex);
            index++;
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
        int color = CyvForge.theme.border2;
        if (mouseX > sr.getScaledWidth()/2+sizeX/2+2 && mouseX < sr.getScaledWidth()/2+sizeX/2+8 &&
                mouseY > amount && mouseY < amount+scrollbarHeight) {
            color = CyvForge.theme.border1;
        }

        GuiUtils.drawRoundedRect(sr.getScaledWidth()/2+sizeX/2+2, amount,
                sr.getScaledWidth()/2+sizeX/2+8, amount+scrollbarHeight, 3, color);

    }

    @Override
    public void handleMouseInput() throws IOException {
        int eventDWheel = net.cyvforge.event.events.GuiHandler.scrollBuffer;
        net.cyvforge.event.events.GuiHandler.scrollBuffer = 0;

        if (eventDWheel != 0) {
            vScroll -= eventDWheel * 0.05;
        }
        super.handleMouseInput();
    }

    public void addToRecent(String name) {
        if (name == null || name.isEmpty() || name.equals("macro")) return;

        recentMacros.remove(name);
        recentMacros.add(0, name);

        if (recentMacros.size() > 10) {
            recentMacros.remove(recentMacros.size() - 1);
        }

        CyvClientConfig.set("recentMacros", String.join(",", recentMacros));
    }

    private void copyMacroFile(String originalName) {
        try {
            java.io.File folder = MacroFileInit.macroFile.getParentFile();
            java.io.File source = new java.io.File(folder, originalName + ".json");
            String newName = originalName + "-copy";
            java.io.File dest = new java.io.File(folder, newName + ".json");

            if (source.exists()) {
                com.google.common.io.Files.copy(source, dest);
                addToRecent(newName);
                net.cyvforge.event.ConfigLoader.save(CyvForge.config, false);

                CyvClientConfig.set("currentMacro", newName);
                mc.displayGuiScreen(new GuiMacro());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteMacroFile(String name) {
        try {
            java.io.File folder = MacroFileInit.macroFile.getParentFile();
            java.io.File fileToDelete = new java.io.File(folder, name + ".json");

            boolean isCurrent = CyvClientConfig.getString("currentMacro", "macro").equals(name);

            if (isCurrent) {
                CyvClientConfig.set("currentMacro", "macro");
                MacroFileInit.swapFile("macro");
            }

            if (fileToDelete.exists()) {
                if (fileToDelete.delete()) {
                    System.out.println("Deleted file: " + name);
                } else {
                    fileToDelete.deleteOnExit();
                    System.out.println("Failed to delete immediately, scheduled for exit: " + name);
                }
            }

            recentMacros.remove(name);
            CyvClientConfig.set("recentMacros", String.join(",", recentMacros));

            net.cyvforge.event.ConfigLoader.save(CyvForge.config, false);

            mc.displayGuiScreen(new GuiMacro());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseEvent) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseEvent);

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

        this.fileName.mouseClicked(mouseX, mouseY, mouseEvent);

        int index=0;
        for (MacroLine l : macroLines) {
            if (l.isPressed(index, mouseX, mouseY, mouseEvent)) {
                this.selectedIndex = index;

                l.yawField.mouseClicked(mouseX, mouseY, mouseEvent);
                l.pitchField.mouseClicked(mouseX, mouseY, mouseEvent);

                if (l.yawField.isFocused()) {
                    focusedColumn = 1;
                } else if (l.pitchField.isFocused()) {
                    focusedColumn = 2;
                } else {
                    focusedColumn = 0;
                        int keyCode = mouseEvent - 100;
                        if (keyCode == mc.gameSettings.keyBindForward.getKeyCode()) {
                            l.w = !l.w;
                        } else if (keyCode == mc.gameSettings.keyBindLeft.getKeyCode()) {
                            l.a = !l.a;
                        } else if (keyCode == mc.gameSettings.keyBindBack.getKeyCode()) {
                            l.s = !l.s;
                        } else if (keyCode == mc.gameSettings.keyBindRight.getKeyCode()) {
                            l.d = !l.d;
                        } else if (keyCode == mc.gameSettings.keyBindJump.getKeyCode()) {
                            l.jump = !l.jump;
                        } else if (keyCode == mc.gameSettings.keyBindSprint.getKeyCode()) {
                            l.sprint = !l.sprint;
                        } else if (keyCode == mc.gameSettings.keyBindSneak.getKeyCode()) {
                            l.sneak = !l.sneak;
                        } else if (keyCode == mc.gameSettings.keyBindUseItem.getKeyCode()) {
                            l.rmb = !l.rmb;
                        }
                    }
                updateFocusAndScroll();
                return;
            }
            index++;
        }

        if (this.addRow.clicked(mouseX, mouseY, mouseEvent)) {
            try {
                if (!(this.selectedIndex > -1 && this.selectedIndex < this.macroLines.size())) {
                    this.macroLines.add(new MacroLine());
                    maxScroll = (int) Math.max(0, Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT * 2 * Math.ceil(macroLines.size()) - (sizeY-20));
                    this.scroll = this.maxScroll;
                    this.selectedIndex = this.macroLines.size()-1;
                } else {
                    this.macroLines.add(selectedIndex+1, new MacroLine());
                }
            } catch (Exception e) {e.printStackTrace();}
        } else if (this.duplicateRow.clicked(mouseX, mouseY, mouseEvent)) {
            try {
                MacroLine oldLine = this.macroLines.get(this.selectedIndex);
                MacroLine newLine = new MacroLine();

                newLine.w = oldLine.w;
                newLine.a = oldLine.a;
                newLine.s = oldLine.s;
                newLine.d = oldLine.d;
                newLine.jump = oldLine.jump;
                newLine.sprint = oldLine.sprint;
                newLine.sneak = oldLine.sneak;
                newLine.rmb = oldLine.rmb;

                newLine.yawField.setText(oldLine.yawField.getText());
                newLine.pitchField.setText(oldLine.pitchField.getText());

                this.macroLines.add(selectedIndex, newLine);
            } catch (Exception e) {}
        } else if (this.deleteRow.clicked(mouseX, mouseY, mouseEvent)) {
            try {
                this.macroLines.remove(selectedIndex);
            } catch (Exception e) {}
        } else if (this.loadFile.clicked(mouseX, mouseY, mouseEvent)) {
            String name = this.fileName.getText();
            addToRecent(name);
            CyvClientConfig.set("currentMacro", name);
            net.cyvforge.event.ConfigLoader.save(CyvForge.config, false);
            mc.displayGuiScreen(new GuiMacro());
            return;
        } else if (this.openList.clicked(mouseX, mouseY, mouseEvent)) {
            mc.displayGuiScreen(new GuiMacroList(this));
            return;
        }

        int rX = (sr.getScaledWidth() + sizeY + 30) / 2 - 75;
        int rY = sr.getScaledHeight() / 2 - sizeY / 2 + 115;
        int rVisHeight = sizeY - 125;
        int rWidth = 150;

        for (int i = 0; i < recentMacros.size(); i++) {
            int rowY = rY + (i * 18);

            if (mouseY >= rY && mouseY <= rY + rVisHeight) {
                if (mouseX >= rX && mouseX <= rX + rWidth && mouseY >= rowY && mouseY <= rowY + 15) {

                    int btnCopyX = rX + rWidth - 36;
                    int btnDelX = rX + rWidth - 18;

                    if (deleteConfirmIndex == i) {
                        if (mouseX >= btnCopyX && mouseX <= btnDelX + 15) {
                            deleteMacroFile(recentMacros.get(i));
                            deleteConfirmIndex = -1;
                        } else {
                            deleteConfirmIndex = -1;
                        }
                        return;
                    }

                    if (mouseX >= btnCopyX && mouseX <= btnCopyX + 15) {
                        copyMacroFile(recentMacros.get(i));
                        return;
                    }

                    if (mouseX >= btnDelX && mouseX <= btnDelX + 15) {
                        deleteConfirmIndex = i;
                        return;
                    }

                    String selectedMacro = recentMacros.get(i);
                    addToRecent(selectedMacro);
                    CyvClientConfig.set("currentMacro", selectedMacro);
                    net.cyvforge.event.ConfigLoader.save(CyvForge.config, false);
                    mc.displayGuiScreen(new GuiMacro());
                    return;
                }
            }
        }
        deleteConfirmIndex = -1;

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

    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);

        if (this.fileName.isFocused()) {
            if (keyCode == Keyboard.KEY_RETURN) {
                String name = this.fileName.getText();
                if (!name.isEmpty() && !name.equals("macro")) {
                    addToRecent(name);
                    net.cyvforge.config.CyvClientConfig.set("currentMacro", name);
                    GuiMacro nextGui = new GuiMacro();
                    nextGui.focusFileNameOnInit = true;
                    mc.displayGuiScreen(nextGui);
                }
                return;
            }
            this.fileName.textboxKeyTyped(typedChar, keyCode);
            return;
        }

        boolean requireMod = CyvClientConfig.getBoolean("macroShortcutsRequireMod", true);
        boolean isMod = Keyboard.isKeyDown(Keyboard.KEY_LMENU) ||
                Keyboard.isKeyDown(Keyboard.KEY_RMENU) ||
                Keyboard.isKeyDown(Keyboard.KEY_TAB);

        if (isMod && selectedIndex == -1 && !macroLines.isEmpty()) {
            selectedIndex = 0;
            focusedColumn = 0;
            updateFocusAndScroll();
        }

        boolean anyFieldFocused = false;
        if (selectedIndex != -1 && selectedIndex < macroLines.size()) {
            MacroLine l = macroLines.get(selectedIndex);
            if (l.yawField.isFocused() || l.pitchField.isFocused()) anyFieldFocused = true;
        }

        if (isMod || (!requireMod && !anyFieldFocused)) {
            if (keyCode == Keyboard.KEY_Z) { // Alt+Z = Undo
                undo();
                return;
            }
            if (keyCode == Keyboard.KEY_B) { // Alt+B = Add
                saveState();
                int insertAt = (selectedIndex == -1) ? macroLines.size() : selectedIndex + 1;
                macroLines.add(insertAt, new MacroLine());
                selectedIndex = insertAt;
                focusedColumn = 0;
                updateFocusAndScroll();
                return;
            }

            if (selectedIndex != -1 && selectedIndex < macroLines.size()) {
                if (keyCode == Keyboard.KEY_C) { // Alt+C = Copy
                    this.clipboard = cloneLine(macroLines.get(selectedIndex));
                    return;
                }
                if (keyCode == Keyboard.KEY_X) { // Alt+X = Cut
                    saveState();
                    macroLines.remove(selectedIndex);
                    if (selectedIndex >= macroLines.size()) selectedIndex = macroLines.size() - 1;
                    updateFocusAndScroll();
                    return;
                }
                if (keyCode == Keyboard.KEY_V) { // Alt+V = Paste
                    if (clipboard != null) {
                        saveState();
                        macroLines.add(selectedIndex + 1, cloneLine(clipboard));
                        selectedIndex++;
                        updateFocusAndScroll();
                    }
                    return;
                }
            }
        }

        if (selectedIndex != -1) {
            if (keyCode == Keyboard.KEY_UP || (isMod && keyCode == Keyboard.KEY_W)) {
                if (selectedIndex > 0) {
                    selectedIndex--;
                    updateFocusAndScroll();
                }
                return;
            }
            if (keyCode == Keyboard.KEY_DOWN || (isMod && keyCode == Keyboard.KEY_S)) {
                if (selectedIndex < macroLines.size() - 1) {
                    selectedIndex++;
                    updateFocusAndScroll();
                }
                return;
            }
            if ((!isMod && keyCode == Keyboard.KEY_LEFT) || (isMod && keyCode == Keyboard.KEY_A)) {
                if (focusedColumn > 0) {
                    focusedColumn--;
                    updateFocusAndScroll();
                    return;
                }
            }

            if ((!isMod && keyCode == Keyboard.KEY_RIGHT) || (isMod && keyCode == Keyboard.KEY_D)) {
                if (focusedColumn < 2) {
                    focusedColumn++;
                    updateFocusAndScroll();
                    return;
                }
            }
        }

        if (this.selectedIndex > -1 && this.selectedIndex < macroLines.size()) {
            MacroLine l = this.macroLines.get(this.selectedIndex);

            if (l.yawField.isFocused() || l.pitchField.isFocused()) {
                GuiTextField activeField = l.yawField.isFocused() ? l.yawField : l.pitchField;

                boolean isNumber = (typedChar >= '0' && typedChar <= '9');
                boolean isSymbol = (typedChar == '.' || typedChar == '-');
                boolean isControl = (keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE ||
                        keyCode == Keyboard.KEY_LEFT || keyCode == Keyboard.KEY_RIGHT ||
                        keyCode == Keyboard.KEY_HOME || keyCode == Keyboard.KEY_END);

                if (isNumber || isSymbol || isControl) {
                    activeField.textboxKeyTyped(typedChar, keyCode);
                }
                return;
            } else if (focusedColumn == 0 && !isMod) {
                if (keyCode == mc.gameSettings.keyBindForward.getKeyCode()) {
                    l.w = !l.w;
                } else if (keyCode == mc.gameSettings.keyBindLeft.getKeyCode()) {
                    l.a = !l.a;
                } else if (keyCode == mc.gameSettings.keyBindBack.getKeyCode()) {
                    l.s = !l.s;
                } else if (keyCode == mc.gameSettings.keyBindRight.getKeyCode()) {
                    l.d = !l.d;
                } else if (keyCode == mc.gameSettings.keyBindJump.getKeyCode()) {
                    l.jump = !l.jump;
                } else if (keyCode == mc.gameSettings.keyBindSprint.getKeyCode()) {
                    l.sprint = !l.sprint;
                } else if (keyCode == mc.gameSettings.keyBindSneak.getKeyCode()) {
                    l.sneak = !l.sneak;
                } else if (keyCode == mc.gameSettings.keyBindUseItem.getKeyCode()) {
                    l.rmb = !l.rmb;
                }
            }

        }
    }

    private void updateFocusAndScroll() {
        if (selectedIndex == -1) return;

        for (int i = 0; i < macroLines.size(); i++) {
            MacroLine line = macroLines.get(i);
            if (i == selectedIndex) {
                line.yawField.setFocused(focusedColumn == 1);
                line.pitchField.setFocused(focusedColumn == 2);
            } else {
                line.yawField.setFocused(false);
                line.pitchField.setFocused(false);
            }
        }

        int rowHeight = fontRendererObj.FONT_HEIGHT * 2;
        int listHeight = sizeY - 40;
        int selectedY = (selectedIndex + 1) * rowHeight;

        if (selectedY - scroll < rowHeight) {
            scroll = selectedY - rowHeight;
        }
        else if (selectedY - scroll > listHeight) {
            scroll = selectedY - listHeight;
        }
    }

    private MacroLine cloneLine(MacroLine original) {
        MacroLine newLine = new MacroLine();
        newLine.w = original.w; newLine.a = original.a;
        newLine.s = original.s; newLine.d = original.d;
        newLine.jump = original.jump; newLine.sprint = original.sprint;
        newLine.sneak = original.sneak; newLine.rmb = original.rmb;
        newLine.yawField.setText(original.yawField.getText());
        newLine.pitchField.setText(original.pitchField.getText());
        return newLine;
    }

    private void saveState() {
        java.util.List<MacroLine> snapshot = new java.util.ArrayList<>();
        for (MacroLine l : this.macroLines) {
            snapshot.add(cloneLine(l));
        }
        undoStack.add(snapshot);
        if (undoStack.size() > MAX_UNDO_STEPS) {
            undoStack.remove(0);
        }
    }

    private void undo() {
        if (!undoStack.isEmpty()) {
            this.macroLines = undoStack.remove(undoStack.size() - 1);
            if (selectedIndex >= macroLines.size()) selectedIndex = macroLines.size() - 1;
            updateFocusAndScroll();
        }
    }

    @Override
    public void updateScreen() {
        if (this.selectedIndex > -1 && this.selectedIndex < macroLines.size()) {
            MacroLine l = this.macroLines.get(this.selectedIndex);

            l.yawField.updateCursorCounter();
            l.pitchField.updateCursorCounter();
        }

        this.fileName.updateCursorCounter();

        int listHeight = sizeY - 40;
        maxScroll = (int) Math.max(0, (macroLines.size() * fontRendererObj.FONT_HEIGHT * 2) - listHeight);
        //smooth scrolling
        this.scroll += this.vScroll;
        this.vScroll *= 0.75;

        if (this.fileName.getText().length() < 1 || this.fileName.getText().length() > 32) this.loadFile.setEnabled(false);
        else if (this.fileName.getText().equals(CyvClientConfig.getString("currentMacro", "macro"))) this.loadFile.setEnabled(false);
        else this.loadFile.setEnabled(true);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();

        if (macroLines == null || macroLines.isEmpty()) return;

        String current = CyvClientConfig.getString("currentMacro", "macro");

        //save macro
        try {
            FileWriter fileWriter = new FileWriter(MacroFileInit.macroFile, false);
            Gson gson = new Gson();
            ArrayList<ArrayList<String>> macroList = new ArrayList<ArrayList<String>>();

            fileWriter.write("[" + System.getProperty("line.separator"));

            for (MacroLine line : this.macroLines) {
                ArrayList<String> macroString = new ArrayList<String>();
                macroString.add(line.w ? "true" : "false");
                macroString.add(line.a ? "true" : "false");
                macroString.add(line.s ? "true" : "false");
                macroString.add(line.d ? "true" : "false");
                macroString.add(line.jump ? "true" : "false");
                macroString.add(line.sprint ? "true" : "false");
                macroString.add(line.sneak ? "true" : "false");
                macroString.add(line.rmb ? "true" : "false");

                try {
                    macroString.add(Double.parseDouble(line.yawField.getText()) + "");
                } catch (NumberFormatException e) {
                    macroString.add("0.0");
                }

                try {
                    macroString.add(Double.parseDouble(line.pitchField.getText()) + "");
                } catch (NumberFormatException e) {
                    macroString.add("0.0");
                }

                macroList.add(macroString);
                fileWriter.write(gson.toJson(macroString) + (macroLines.indexOf(line) == macroLines.size()-1 ? "" : ",") + System.getProperty("line.separator"));
            }

            fileWriter.write("]");

            CommandMacro.macro = macroList;
            String json = gson.toJson(macroList.toArray());
            fileWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class MacroLine {
        int xStart = sr.getScaledWidth()/2 - sizeX/2 + 10;
        int width = sizeX/2 + 20;
        int height = mc.fontRendererObj.FONT_HEIGHT*2;

        public boolean w, a, s, d, jump, sprint, sneak, rmb;

        GuiTextField yawField, pitchField;

        public MacroLine() {
            this.yawField = new GuiTextField(7, fontRendererObj, 0, 0, 33, Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT);
            this.pitchField = new GuiTextField(8, fontRendererObj, 0, 0, 33, Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT);
            this.yawField.setEnableBackgroundDrawing(false);
            this.pitchField.setEnableBackgroundDrawing(false);

            this.yawField.setText("");
            this.pitchField.setText("");
        }

        public void drawEntry(int slotIndex, int scroll, int mouseX, int mouseY, boolean isSelected) {
            int yHeight = (slotIndex + 1) * mc.fontRendererObj.FONT_HEIGHT*2 - scroll + (sr.getScaledHeight()/2 - sizeY/2);
            GuiUtils.drawRoundedRect(xStart, yHeight + 1,
                    xStart + width, yHeight + height - 1,
                    3, isSelected ? CyvForge.theme.shade1 : CyvForge.theme.shade2);

            StringBuilder string = new StringBuilder();
            if (w) string.append("W ");
            if (a) string.append("A ");
            if (s) string.append("S ");
            if (d) string.append("D ");
            if (jump) string.append("Jump ");
            if (sprint) string.append("Spr ");
            if (sneak) string.append("Snk ");
            if (rmb) string.append("RMB ");

            GuiUtils.drawString(string.toString(), xStart + 4, yHeight + height/3, 0xFFFFFFFF);

            this.yawField.yPosition = yHeight + mc.fontRendererObj.FONT_HEIGHT*3/4;
            this.pitchField.yPosition = yHeight + mc.fontRendererObj.FONT_HEIGHT*3/4;
            this.yawField.xPosition = sr.getScaledWidth()/2 - 42;
            this.pitchField.xPosition = sr.getScaledWidth()/2 - 5;

            GuiUtils.drawRoundedRect(sr.getScaledWidth()/2 - 46, yHeight + 2,
                    sr.getScaledWidth()/2 - 41+31, yHeight + mc.fontRendererObj.FONT_HEIGHT*2 - 2,
                    2, CyvForge.theme.highlight);
            GuiUtils.drawRoundedRect(sr.getScaledWidth()/2 - 8, yHeight + 2,
                    sr.getScaledWidth()/2 - 3+31, yHeight + mc.fontRendererObj.FONT_HEIGHT*2 - 2,
                    2, CyvForge.theme.highlight);

            this.yawField.drawTextBox();
            this.pitchField.drawTextBox();

        }

        public boolean isPressed(int slotIndex, int mouseX, int mouseY, int mouseEvent) {
            float yHeight = (slotIndex + 1) * mc.fontRendererObj.FONT_HEIGHT*2 - scroll + (sr.getScaledHeight()/2 - sizeY/2);
            if (mouseX > xStart && mouseX < xStart + width && mouseY > yHeight && mouseY < yHeight + height) {
                return true;
            }

            return false;
        }

        public void mouseClicked(int slotIndex, int mouseX, int mouseY, int mouseEvent) {
            float yHeight = (slotIndex + 1) * mc.fontRendererObj.FONT_HEIGHT*2 - scroll + (sr.getScaledHeight()/2 - sizeY/2);
            if (!(mouseX > xStart && mouseX < xStart + width && mouseY > yHeight && mouseY < yHeight + height)) {
                return;
            }

            this.yawField.mouseClicked(mouseX, mouseY, mouseEvent);
            this.pitchField.mouseClicked(mouseX, mouseY, mouseEvent);

        }

    }

}