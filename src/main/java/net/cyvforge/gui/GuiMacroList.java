package net.cyvforge.gui;

import net.cyvforge.CyvForge;
import net.cyvforge.config.CyvClientConfig;
import net.cyvforge.event.MacroFileInit;
import net.cyvforge.util.GuiUtils;
import net.cyvforge.util.defaults.CyvGui;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GuiMacroList extends CyvGui {
    private final GuiMacro parent;
    private int sizeX = 250;
    private int sizeY = 300;

    private GuiTextField searchField;
    private List<String> allMacros = new ArrayList<>();
    private List<String> filteredMacros = new ArrayList<>();

    private float scroll = 0;
    private float vScroll = 0;
    private int maxScroll = 0;

    private int deleteConfirmIndex = -1;

    public GuiMacroList(GuiMacro parent) {
        super("Macro List");
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        if (sizeY > height - 40) sizeY = height - 40;
        int x = width / 2 - sizeX / 2;
        int y = height / 2 - sizeY / 2;

        this.searchField = new GuiTextField(0, fontRendererObj, x + 25, y + 46, sizeX - 50, 12);
        this.searchField.setEnableBackgroundDrawing(false);
        this.searchField.setFocused(true);
        this.searchField.setMaxStringLength(32);

        loadFiles();
    }

    private void loadFiles() {
        allMacros.clear();
        File folder = MacroFileInit.macroFile.getParentFile();
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File f : files) {
                    String name = f.getName().replace(".json", "");
                    if (!name.equalsIgnoreCase("macro")) {
                        allMacros.add(name);
                    }
                }
            }
        }
        updateSearch();
    }

    private void updateSearch() {
        String query = searchField.getText().toLowerCase();
        filteredMacros = allMacros.stream()
                .filter(name -> name.toLowerCase().contains(query))
                .collect(Collectors.toList());

        int listAreaHeight = sizeY - 80;
        maxScroll = Math.max(0, (filteredMacros.size() * 22) - listAreaHeight);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int x = width / 2 - sizeX / 2;
        int y = height / 2 - sizeY / 2;

        GuiUtils.drawRoundedRect(x, y, x + sizeX, y + sizeY, 5, CyvForge.theme.background1);
        GuiUtils.drawRoundedRect(x, y, x + sizeX, y + 25, 5, CyvForge.theme.shade1);

        String title = "Macro List";
        fontRendererObj.drawString(title, x + (sizeX / 2) - (fontRendererObj.getStringWidth(title) / 2), y + 9, 0xFFFFFFFF);

        // Search bar
        int searchMargin = 20;
        GuiUtils.drawRoundedRect(x + searchMargin, y + 40, x + sizeX - searchMargin, y + 60, 2, CyvForge.theme.shade2);
        if (searchField.getText().isEmpty() && !searchField.isFocused()) {
            fontRendererObj.drawString("Search...", x + searchMargin + 5, y + 46, 0x888888);
        }
        searchField.drawTextBox();

        // List
        int listY = y + 70;
        int listHeight = sizeY - 80;
        int sf = sr.getScaleFactor();

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * sf, (height - (listY + listHeight)) * sf, sizeX * sf, listHeight * sf);

        for (int i = 0; i < filteredMacros.size(); i++) {
            int rowY = (int) (listY + (i * 22) - scroll);
            int rowHeight = 18;
            int rowMargin = 15;

            if (rowY + 20 > listY && rowY < listY + listHeight) {
                boolean hovered = mouseX >= x + rowMargin && mouseX <= x + sizeX - rowMargin && mouseY >= rowY && mouseY <= rowY + rowHeight;

                GuiUtils.drawRoundedRect(x + rowMargin, rowY, x + sizeX - rowMargin, rowY + rowHeight, 2, (hovered || deleteConfirmIndex == i) ? 0x60FFFFFF : 0x20000000);
                fontRendererObj.drawString(filteredMacros.get(i), x + rowMargin + 5, rowY + 5, 0xFFFFFFFF);

                // Buttons
                if (hovered || deleteConfirmIndex == i) {
                    int btnCopyX = x + sizeX - rowMargin - 35;
                    int btnDelX = x + sizeX - rowMargin - 18;

                    if (deleteConfirmIndex == i) {
                        boolean confHover = mouseX >= btnCopyX && mouseX <= btnDelX + 15 && mouseY >= rowY + 1 && mouseY <= rowY + 17;
                        GuiUtils.drawRoundedRect(btnCopyX, rowY + 1, btnDelX + 15, rowY + 17, 2, confHover ? 0xFFFF0000 : 0xFFCC0000);
                        fontRendererObj.drawString("Sure?", btnCopyX + 2, rowY + 5, 0xFFFFFFFF);
                    } else {
                        boolean cnHover = mouseX >= btnCopyX && mouseX <= btnCopyX + 15 && mouseY >= rowY + 1 && mouseY <= rowY + 17;
                        fontRendererObj.drawString("C", btnCopyX + 4, rowY + 5, cnHover ? 0xFF00FFFF : 0xAAFFFFFF);

                        boolean dlHover = mouseX >= btnDelX && mouseX <= btnDelX + 15 && mouseY >= rowY + 1 && mouseY <= rowY + 17;
                        fontRendererObj.drawString("X", btnDelX + 4, rowY + 5, dlHover ? 0xFFFF0000 : 0xAAFFFFFF);
                    }
                }
            }
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // Scrollbar
        if (maxScroll > 0) {
            int sbX = x + sizeX - 5;
            int sbH = Math.max(15, (int) ((float) listHeight * listHeight / (maxScroll + listHeight)));
            int sbY = listY + (int) (scroll / maxScroll * (listHeight - sbH));
            Gui.drawRect(sbX, listY, sbX + 2, listY + listHeight, 0x20FFFFFF);
            Gui.drawRect(sbX, sbY, sbX + 2, sbY + sbH, 0x80FFFFFF);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        searchField.mouseClicked(mouseX, mouseY, mouseButton);
        int x = width / 2 - sizeX / 2;
        int y = height / 2 - sizeY / 2;
        int listY = y + 70;
        int listHeight = sizeY - 80;

        if (mouseY >= listY && mouseY <= listY + listHeight) {
            for (int i = 0; i < filteredMacros.size(); i++) {
                int rowY = (int) (listY + (i * 22) - scroll);
                int rowMargin = 15;
                int rowWidth = sizeX - (rowMargin * 2);

                if (mouseX >= x + rowMargin && mouseX <= x + sizeX - rowMargin && mouseY >= rowY && mouseY <= rowY + 18) {
                    String macroName = filteredMacros.get(i);
                    int btnCopyX = x + sizeX - rowMargin - 35;
                    int btnDelX = x + sizeX - rowMargin - 18;

                    if (deleteConfirmIndex == i) {
                        if (mouseX >= btnCopyX) {
                            deleteMacroFile(macroName);
                        }
                        deleteConfirmIndex = -1;
                        return;
                    }

                    if (mouseX >= btnCopyX && mouseX <= btnCopyX + 15) {
                        copyMacroFile(macroName);
                        return;
                    }

                    if (mouseX >= btnDelX) {
                        deleteConfirmIndex = i;
                        return;
                    }

                    selectMacro(macroName);
                    return;
                }
            }
        }
        deleteConfirmIndex = -1;
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void copyMacroFile(String name) {
        try {
            File folder = MacroFileInit.macroFile.getParentFile();
            File src = new File(folder, name + ".json");
            File dest = new File(folder, name + "-copy.json");
            if (src.exists()) {
                com.google.common.io.Files.copy(src, dest);
                loadFiles();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void deleteMacroFile(String name) {
        try {
            java.io.File folder = MacroFileInit.macroFile.getParentFile();
            java.io.File fileToDelete = new java.io.File(folder, name + ".json");

            if (fileToDelete.exists()) {
                fileToDelete.delete();
            }

            String recent = CyvClientConfig.getString("recentMacros", "");
            List<String> recentList = new ArrayList<>(java.util.Arrays.asList(recent.split(",")));
            if (recentList.remove(name)) {
                CyvClientConfig.set("recentMacros", String.join(",", recentList));
                net.cyvforge.event.ConfigLoader.save(CyvForge.config, false);
            }

            if (CyvClientConfig.getString("currentMacro", "macro").equals(name)) {
                CyvClientConfig.set("currentMacro", "macro");
            }

            loadFiles();
            deleteConfirmIndex = -1;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void selectMacro(String name) {
        parent.addToRecent(name);
        CyvClientConfig.set("currentMacro", name);
        mc.displayGuiScreen(new GuiMacro());
    }

    @Override
    public void handleMouseInput() throws IOException {
        int dw = Mouse.getEventDWheel();
        if (dw != 0) vScroll -= dw * 0.15;
        super.handleMouseInput();
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        if (searchField.isFocused()) {
            searchField.textboxKeyTyped(typedChar, keyCode);
            updateSearch();
            if (keyCode == Keyboard.KEY_RETURN && !filteredMacros.isEmpty()) selectMacro(filteredMacros.get(0));
        }
        if (keyCode == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
    }

    @Override
    public void updateScreen() {
        searchField.updateCursorCounter();
        scroll += vScroll;
        vScroll *= 0.7;
        if (scroll < 0) scroll = 0;
        if (scroll > maxScroll) scroll = maxScroll;
    }
}