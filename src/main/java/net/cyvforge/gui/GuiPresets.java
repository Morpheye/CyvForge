package net.cyvforge.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.cyvforge.CyvForge;
import net.cyvforge.config.CyvClientConfig;
import net.cyvforge.event.ConfigLoader;
import net.cyvforge.hud.HUDManager;
import net.cyvforge.hud.structure.DraggableHUDElement;
import net.cyvforge.hud.structure.ScreenPosition;
import net.cyvforge.util.GuiUtils;
import net.cyvforge.util.defaults.CyvGui;
import net.cyvforge.util.HUDPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GuiPresets extends CyvGui {
    int sizeX = 220, sizeY = 240;
    public ArrayList<PresetLine> presetLines = new ArrayList<>();
    SubButton addNewButton, backBtn;
    File hudPresetDir, kbPresetDir;
    float scroll = 0, vScroll = 0;
    int maxScroll = 0;

    public enum PresetType { HUD, KEYBINDS }
    public static PresetType currentTab = PresetType.HUD;

    private static boolean migrated = false;

    public static class KeybindPreset {
        public String presetName;
        public Map<String, Integer> keybinds = new HashMap<>();
        public KeybindPreset() {}
        public KeybindPreset(String name) { this.presetName = name; }
    }

    public static synchronized void ensureDirectories() {
        if (migrated) return;
        migrated = true;

        File baseDir = new File(Minecraft.getMinecraft().mcDataDir, "config/cyvforge");
        File oldHudDir = new File(baseDir, "presets");
        File oldKbDir = new File(baseDir, "keybinds");

        File newHudDir = new File(oldHudDir, "configs");
        File newKbDir = new File(oldHudDir, "keybinds");

        if (!newHudDir.exists()) newHudDir.mkdirs();
        if (!newKbDir.exists()) newKbDir.mkdirs();

        // Used when i needed to migrate existing files in the old directories to the new ones in 1.12
        if (oldHudDir.exists() && oldHudDir.isDirectory()) {
            File[] files = oldHudDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && (f.getName().endsWith(".json") || f.getName().endsWith(".cfg"))) {
                        try {
                            Files.move(f.toPath(), new File(newHudDir, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        if (oldKbDir.exists() && oldKbDir.isDirectory()) {
            File[] files = oldKbDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) {
                        try {
                            Files.move(f.toPath(), new File(newKbDir, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            try {
                if (oldKbDir.list() == null || oldKbDir.list().length == 0) {
                    oldKbDir.delete();
                }
            } catch (Exception ignored) {}
        }
    }

    public static File getHudPresetDir() {
        ensureDirectories();
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "config/cyvforge/presets/configs");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }
    //

    public static File getKbPresetDir() {
        ensureDirectories();
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "config/cyvforge/presets/keybinds");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public GuiPresets() {
        super("HUD & Keybind Presets");
        this.hudPresetDir = getHudPresetDir();
        this.kbPresetDir = getKbPresetDir();
    }

    private static File getKbSelFile() {
        return new File(getKbPresetDir(), ".selected");
    }

    private static File getHUDConfigFile(String presetName) {
        String name = presetName.endsWith(".json") ? presetName.substring(0, presetName.length() - 5) + ".cfg" : presetName + ".cfg";
        return new File(getHudPresetDir(), name);
    }

    private static void saveHUDConfig(String presetName) {
        if (presetName == null || presetName.equals("none") || presetName.isEmpty()) return;
        try {
            ConfigLoader.save(CyvForge.config, false);
            File configFile = ConfigLoader.configFile;
            File presetFile = getHUDConfigFile(presetName);
            if (configFile == null || !configFile.isFile()) return;
            File parent = presetFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            Files.copy(configFile.toPath(), presetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static boolean loadHUDConfig(String presetName) {
        File presetFile = getHUDConfigFile(presetName);
        File configFile = ConfigLoader.configFile;
        if (!presetFile.isFile() || configFile == null) return false;
        try {
            Files.copy(presetFile.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            for (CyvClientConfig.ConfigValue<?> value : CyvForge.config.configFields.values()) value.reset();
            ConfigLoader.read(CyvForge.config);
            setSelectedName(PresetType.HUD, presetName);
            ConfigLoader.save(CyvForge.config, false);
            return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    private static String getSelectedName(PresetType type) {
        if (type == PresetType.HUD) return CyvClientConfig.getString("selectedPreset", "none");
        File selFile = getKbSelFile();
        if (selFile.isFile()) {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(selFile), "UTF-8"))) {
                String s = r.readLine();
                if (s != null && !s.trim().isEmpty()) return s.trim();
            } catch (IOException e) { e.printStackTrace(); }
        }
        return CyvClientConfig.getString("selectedKbPreset", "none");
    }

    private static void setSelectedName(PresetType type, String name) {
        name = (name == null || name.isEmpty()) ? "none" : name;
        if (type == PresetType.HUD) { CyvClientConfig.set("selectedPreset", name); return; }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(getKbSelFile()), "UTF-8")) {
            w.write(name);
        } catch (IOException e) { e.printStackTrace(); }
        CyvClientConfig.set("selectedKbPreset", name);
    }

    private static JsonObject readHUDData(File file, String name) {
        if (file.isFile()) {
            try (Reader r = new InputStreamReader(new FileInputStream(file), "UTF-8")) {
                JsonElement element = new JsonParser().parse(r);
                if (element != null && element.isJsonObject()) return element.getAsJsonObject();
            } catch (Exception e) { e.printStackTrace(); }
        }
        return new Gson().toJsonTree(new HUDPreset(name)).getAsJsonObject();
    }

    private static JsonObject getHUDPositions(JsonObject data) {
        if (!data.has("positions") || !data.get("positions").isJsonObject()) data.add("positions", new JsonObject());
        return data.getAsJsonObject("positions");
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        int startX = this.width / 2 - sizeX / 2, startY = this.height / 2 - sizeY / 2;
        this.backBtn = new SubButton("Back", startX, startY - 25, 40, 14);
        this.addNewButton = new SubButton("Create New Preset", this.width / 2 - 75, startY + sizeY - 25, 150, 20);
        this.backBtn.setEnabled(true);
        this.addNewButton.setEnabled(true);
        if (currentTab == PresetType.HUD) saveCurrentTabState();
        refreshPresets();
    }

    public void refreshPresets() {
        presetLines.clear();
        File activeDir = (currentTab == PresetType.HUD) ? hudPresetDir : kbPresetDir;
        File[] files = activeDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) for (File f : files) presetLines.add(new PresetLine(f));

        String selected = getSelectedName(currentTab);
        boolean found = false;
        for (PresetLine line : presetLines) if (line.file.getName().equals(selected)) found = true;

        if (!found && !presetLines.isEmpty()) {
            PresetLine toApply = presetLines.get(0);
            setSelectedName(currentTab, toApply.file.getName());
            toApply.applyPreset();
            if (currentTab == PresetType.HUD) saveHUDConfig(toApply.file.getName());
            ConfigLoader.save(CyvForge.config, false);
        } else if (presetLines.isEmpty()) setSelectedName(currentTab, "none");
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawDefaultBackground();
        int startX = this.width / 2 - sizeX / 2, startY = this.height / 2 - sizeY / 2, listHeight = sizeY - 70;
        maxScroll = Math.max(0, presetLines.size() * 22 - listHeight);

        // Back Button
        boolean backHover = mouseX >= startX && mouseX <= startX + 40 && mouseY >= startY - 25 && mouseY <= startY - 11;
        GuiUtils.drawRoundedRect(startX, startY - 25, startX + 40, startY - 11, 5, backHover ? 0xBF333333 : 0xBF000000);
        GuiUtils.drawCenteredString("Back", startX + 20, startY - 22, 0xFFFFFFFF, false);

        GuiUtils.drawRoundedRect(startX - 5, startY - 5, startX + sizeX + 5, startY + sizeY + 5, 5, CyvForge.theme.background1);
        GuiUtils.drawCenteredString(currentTab == PresetType.HUD ? "Config Presets" : "Keybind Presets", this.width / 2, startY + 5, 0xFFFFFFFF, true);

        // Tabs
        int tabY = startY + 20, tabW = 95;
        boolean hoverHud = mouseX >= startX + 10 && mouseX <= startX + 10 + tabW && mouseY >= tabY && mouseY <= tabY + 16;
        GuiUtils.drawRoundedRect(startX + 10, tabY, startX + 10 + tabW, tabY + 16, 2, currentTab == PresetType.HUD ? 0x80FFFFFF : (hoverHud ? 0x60FFFFFF : 0x40000000));
        GuiUtils.drawCenteredString("Config", startX + 10 + tabW / 2, tabY + 4, currentTab == PresetType.HUD ? 0xFFFFFF00 : 0xFFFFFFFF, false);

        boolean hoverKb = mouseX >= startX + 115 && mouseX <= startX + 115 + tabW && mouseY >= tabY && mouseY <= tabY + 16;
        GuiUtils.drawRoundedRect(startX + 115, tabY, startX + 115 + tabW, tabY + 16, 2, currentTab == PresetType.KEYBINDS ? 0x80FFFFFF : (hoverKb ? 0x60FFFFFF : 0x40000000));
        GuiUtils.drawCenteredString("Keybinds", startX + 115 + tabW / 2, tabY + 4, currentTab == PresetType.KEYBINDS ? 0xFFFFFF00 : 0xFFFFFFFF, false);

        this.addNewButton.draw(mouseX, mouseY);

        int factor = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(startX * factor, (height - (startY + sizeY - 30)) * factor, sizeX * factor, listHeight * factor);

        String currentSelected = getSelectedName(currentTab);
        for (int i = 0; i < presetLines.size(); i++) {
            int yPos = (int) (startY + 45 + (i * 22) - scroll);
            PresetLine line = presetLines.get(i);
            line.draw(yPos, mouseX, mouseY);
            if (line.file.getName().equals(currentSelected)) GuiUtils.drawRectOutline(startX + 1, yPos - 4, startX + sizeX - 2, yPos + 18, 0xFFFFFFFF);
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        if (maxScroll > 0) {
            int scrollHeight = Math.max(10, (int) ((float) listHeight * listHeight / (maxScroll + listHeight)));
            int scrollPos = (int) (scroll / maxScroll * (listHeight - scrollHeight));
            Gui.drawRect(startX - 3, startY + 40, startX - 1, startY + 40 + listHeight, 0x30FFFFFF);
            Gui.drawRect(startX - 3, startY + 40 + scrollPos, startX - 1, startY + 40 + scrollPos + scrollHeight, 0xFFFFFFFF);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseEvent) throws IOException {
        int startX = this.width / 2 - sizeX / 2, startY = this.height / 2 - sizeY / 2;
        if (mouseX >= startX && mouseX <= startX + 40 && mouseY >= startY - 25 && mouseY <= startY - 11) { mc.displayGuiScreen(new GuiMPK()); return; }

        int tabY = startY + 20;
        if (mouseY >= tabY && mouseY <= tabY + 16) {
            if (mouseX >= startX + 10 && mouseX <= startX + 105 && currentTab != PresetType.HUD) {
                saveCurrentTabState(); currentTab = PresetType.HUD; scroll = 0; refreshPresets(); return;
            }
            if (mouseX >= startX + 115 && mouseX <= startX + 210 && currentTab != PresetType.KEYBINDS) {
                saveCurrentTabState(); currentTab = PresetType.KEYBINDS; scroll = 0; refreshPresets(); return;
            }
        }

        if (this.addNewButton.clicked(mouseX, mouseY, mouseEvent)) { saveCurrentAsNew(); refreshPresets(); return; }

        ArrayList<PresetLine> temp = new ArrayList<>(presetLines);
        for (int i = 0; i < temp.size(); i++) {
            int yPos = (int) (startY + 45 + (i * 22) - scroll);
            if (mouseY >= startY + 40 && mouseY <= startY + sizeY - 30) {
                if (temp.get(i).mouseClicked(yPos, mouseX, mouseY, mouseEvent)) break;
            }
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) { saveCurrentTabState(); mc.displayGuiScreen(new GuiMPK()); return; }
        for (PresetLine line : new ArrayList<>(presetLines)) {
            if (line.nameField.isFocused()) {
                line.nameField.textboxKeyTyped(typedChar, keyCode);
                if (keyCode == Keyboard.KEY_RETURN) { line.saveName(); refreshPresets(); return; }
            }
        }
    }

    private void saveCurrentAsNew() {
        File dir = (currentTab == PresetType.HUD) ? hudPresetDir : kbPresetDir;
        int number = presetLines.size() + 1;
        String name = "Preset " + number;
        File file = new File(dir, name + ".json");
        while (file.exists()) { number++; name = "Preset " + number; file = new File(dir, name + ".json"); }

        if (currentTab == PresetType.HUD) {
            HUDPreset p = new HUDPreset(name);
            for (DraggableHUDElement e : HUDManager.registeredRenderers) {
                ScreenPosition pos = (e.position != null) ? e.position : (e.load() != null ? e.load() : e.getDefaultPosition());
                p.positions.put(e.getName(), pos.getAbsoluteX() + "," + pos.getAbsoluteY() + "," + e.isVisible + "," + e.isEnabled);
            }
            saveObj(file, p);
        } else {
            KeybindPreset p = new KeybindPreset(name);
            for (KeyBinding kb : mc.gameSettings.keyBindings) p.keybinds.put(kb.getKeyDescription(), kb.getKeyCode());
            saveObj(file, p);
        }
        setSelectedName(currentTab, name + ".json");
        if (currentTab == PresetType.HUD) saveHUDConfig(name + ".json");
        ConfigLoader.save(CyvForge.config, false);
    }

    public static void saveObj(File f, Object o) {
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8")) {
            new GsonBuilder().setPrettyPrinting().create().toJson(o, w);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void saveCurrentTabState() {
        if (currentTab == PresetType.HUD) saveCurrentLayoutToSelected();
        else saveCurrentKeybindsToSelected();
    }

    public static void saveCurrentLayoutToSelected() {
        String sel = getSelectedName(PresetType.HUD);
        if (sel.equals("none") || sel.isEmpty()) return;
        File f = new File(getHudPresetDir(), sel);
        if (!f.exists()) return;

        JsonObject data = readHUDData(f, sel.replace(".json", ""));
        JsonObject positions = getHUDPositions(data);
        for (DraggableHUDElement e : HUDManager.registeredRenderers) {
            ScreenPosition pos = (e.position != null) ? e.position : (e.load() != null ? e.load() : e.getDefaultPosition());
            positions.addProperty(e.getName(), pos.getAbsoluteX() + "," + pos.getAbsoluteY() + "," + e.isVisible + "," + e.isEnabled);
        }
        data.add("positions", positions);
        data.remove("configs");
        saveObj(f, data);
        saveHUDConfig(sel);
    }

    public static void saveCurrentKeybindsToSelected() {
        String sel = getSelectedName(PresetType.KEYBINDS);
        if (sel.equals("none") || sel.isEmpty()) return;
        File f = new File(getKbPresetDir(), sel);
        if (!f.isFile()) return;

        KeybindPreset p = new KeybindPreset(sel.replace(".json", ""));
        for (KeyBinding kb : Minecraft.getMinecraft().gameSettings.keyBindings) p.keybinds.put(kb.getKeyDescription(), kb.getKeyCode());
        saveObj(f, p);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        saveCurrentTabState();
        super.onGuiClosed();
    }

    class PresetLine {
        File file; String presetName; GuiTextField nameField; SubButton editBtn, deleteBtn;

        public PresetLine(File f) {
            this.file = f;
            this.presetName = f.getName().replace(".json", "");
            nameField = new GuiTextField(0, fontRendererObj, 0, 0, 110, 14);
            nameField.setText(presetName);
            nameField.setEnableBackgroundDrawing(true);
            editBtn = new SubButton("Edit", 0, 0, 40, 14);
            deleteBtn = new SubButton("X", 0, 0, 15, 14);
        }

        public void draw(int y, int mx, int my) {
            int xStart = width / 2 - sizeX / 2 + 5;
            nameField.xPosition = xStart; nameField.yPosition = y;
            editBtn.setX(xStart + 120); editBtn.setY(y);
            deleteBtn.setX(xStart + 165); deleteBtn.setY(y);
            nameField.drawTextBox(); editBtn.draw(mx, my); deleteBtn.draw(mx, my);
        }

        public void saveName() {
            String newN = nameField.getText().trim();
            if (newN.isEmpty() || newN.equals(presetName)) { nameField.setText(presetName); nameField.setFocused(false); return; }
            File newF = new File(file.getParentFile(), newN + ".json");
            if (newF.exists()) { nameField.setText(presetName); nameField.setFocused(false); return; }

            String oldName = file.getName();
            boolean wasSelected = getSelectedName(currentTab).equals(oldName);

            if (file.renameTo(newF)) {
                if (currentTab == PresetType.HUD) {
                    File oldConfig = getHUDConfigFile(oldName), newConfig = getHUDConfigFile(newF.getName());
                    if (oldConfig.isFile()) {
                        try { Files.move(oldConfig.toPath(), newConfig.toPath(), StandardCopyOption.REPLACE_EXISTING); } catch (IOException e) { e.printStackTrace(); }
                    }
                }
                if (wasSelected) setSelectedName(currentTab, newF.getName());
                this.file = newF; this.presetName = newN; nameField.setText(newN);
                if (wasSelected && currentTab == PresetType.HUD) saveCurrentLayoutToSelected();
            } else { nameField.setText(presetName); }
            nameField.setFocused(false); refreshPresets(); ConfigLoader.save(CyvForge.config, false);
        }

        public boolean mouseClicked(int y, int mx, int my, int btn) {
            int xStart = width / 2 - sizeX / 2 + 5;
            nameField.mouseClicked(mx, my, btn);
            if (mx >= xStart && mx <= xStart + sizeX && my >= y && my <= y + 16) {
                if (mx >= xStart + 165 && mx <= xStart + 185) {
                    boolean wasSelected = getSelectedName(currentTab).equals(file.getName());
                    if (file.delete()) {
                        if (currentTab == PresetType.HUD) getHUDConfigFile(file.getName()).delete();
                        if (wasSelected) setSelectedName(currentTab, "none");
                        refreshPresets();
                    }
                    return true;
                }
                saveCurrentTabState(); setSelectedName(currentTab, file.getName()); applyPreset();
                if (mx >= xStart + 120 && mx <= xStart + 160) {
                    if (currentTab == PresetType.HUD) {
                        mc.displayGuiScreen(new GuiHUDPositions(true, true));
                    } else {
                        mc.displayGuiScreen(new net.minecraft.client.gui.GuiControls(GuiPresets.this, mc.gameSettings) {
                            @Override
                            public void onGuiClosed() {
                                KeyBinding.resetKeyBindingArrayAndHash();
                                mc.gameSettings.saveOptions();
                                saveCurrentKeybindsToSelected();
                                super.onGuiClosed();
                            }
                        });
                    }
                }
                ConfigLoader.save(CyvForge.config, false); return true;
            }
            return false;
        }

        public void applyPreset() {
            try (Reader r = new InputStreamReader(new FileInputStream(file), "UTF-8")) {
                if (currentTab == PresetType.HUD) {
                    boolean configLoaded = loadHUDConfig(file.getName());
                    JsonElement element = new JsonParser().parse(r);
                    if (element == null || !element.isJsonObject()) return;

                    JsonObject data = element.getAsJsonObject();
                    JsonObject positions = getHUDPositions(data);
                    for (DraggableHUDElement e : HUDManager.registeredRenderers) {
                        JsonElement posElement = positions.get(e.getName());
                        if (posElement != null && posElement.isJsonPrimitive()) {
                            String[] p = posElement.getAsString().split(",");
                            if (p.length >= 2) { e.save(new ScreenPosition(Integer.parseInt(p[0]), Integer.parseInt(p[1]))); e.load(); }
                            if (p.length >= 3) e.isVisible = Boolean.parseBoolean(p[2]);
                            if (p.length >= 4) e.setEnabled(Boolean.parseBoolean(p[3]));
                        }
                    }
                } else {
                    KeybindPreset d = new Gson().fromJson(r, KeybindPreset.class);
                    if (d != null && d.keybinds != null) {
                        for (KeyBinding kb : mc.gameSettings.keyBindings) {
                            Integer code = d.keybinds.get(kb.getKeyDescription());
                            if (code != null) kb.setKeyCode(code);
                        }
                    }
                    KeyBinding.resetKeyBindingArrayAndHash(); mc.gameSettings.saveOptions();
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int eventDWheel = org.lwjgl.input.Mouse.getEventDWheel();

        if (eventDWheel != 0) {
            vScroll -= eventDWheel * 0.08f;
        }
    }

    @Override
    public void updateScreen() {
        scroll += vScroll; vScroll *= 0.7f;
        if (scroll < 0) scroll = 0;
        if (scroll > maxScroll) scroll = maxScroll;
    }
}