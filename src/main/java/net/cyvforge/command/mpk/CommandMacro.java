package net.cyvforge.command.mpk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import net.cyvforge.CyvForge;
import net.cyvforge.config.CyvClientConfig;
import net.cyvforge.event.MacroFileInit;
import net.cyvforge.event.events.GuiHandler;
import net.cyvforge.gui.GuiMacro;
import net.cyvforge.util.defaults.CyvCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommandSender;
import net.cyvforge.event.events.MacroRecorder;
import java.util.List;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

public class CommandMacro extends CyvCommand {
    public CommandMacro() {
        super("macro");
        this.helpString = "Open the parkour macro GUI.";
    }

    public static int macroRunning = 0;
    public static boolean isRecording = false;
    public static ArrayList<ArrayList<String>> macro = new ArrayList<ArrayList<String>>();
    //[w][a][s][d][space][sprint][sneak][yaw][pitch]

    @Override
    public void run(ICommandSender sender, String[] args) {
        if (args.length > 0) {
            String sub = args[0].toLowerCase();
            if (sub.equals("run")) runMacro(args);
            else if (sub.equals("stop")) {
                if (isRecording) stopRecording();
                macroRunning = 1;
            }
            else if (sub.equals("rec") || sub.equals("record") || sub.equals("recording")) {
                if (args.length > 1 && args[1].equalsIgnoreCase("start")) {
                    startRecording();
                } else if (args.length > 1 && args[1].equalsIgnoreCase("stop")) {
                    stopRecording();
                }
            }
            else if (sub.equals("clip")) {
                clipMacro();
            }
        } else {
            GuiHandler.setScreen(new GuiMacro());
        }
    }

    public static void startRecording() {
        if (isRecording) {
            CyvForge.sendChatMessage("Already recording!");
            return;
        }

        String nextRecName = MacroFileInit.getNextAvailableName("Rec");
        CyvClientConfig.set("currentMacro", nextRecName);
        GuiMacro.addToRecent(nextRecName);

        net.cyvforge.event.ConfigLoader.save(CyvForge.config, false);
        MacroFileInit.swapFile(nextRecName);

        macro.clear();
        isRecording = true;
        CyvForge.sendChatMessage("Recording started: " + nextRecName);
    }

    public static void stopRecording() {
        if (!isRecording) return;
        isRecording = false;
        saveMacroToFile();
        CyvForge.sendChatMessage("Recording stopped. Saved to: " + MacroFileInit.macroFile.getName());
    }

    public static void clipMacro() {
        if (!CyvClientConfig.getBoolean("macroClipEnabled", false)) {
            CyvForge.sendChatMessage("Macro clip is disabled in settings!");
            return;
        }

        if (MacroRecorder.clipBuffer.isEmpty()) {
            CyvForge.sendChatMessage("No movement data to clip!");
            return;
        }

        String nextClipName = MacroFileInit.getNextAvailableName("Clip");
        CyvClientConfig.set("currentMacro", nextClipName);
        GuiMacro.addToRecent(nextClipName);

        net.cyvforge.event.ConfigLoader.save(CyvForge.config, false);
        MacroFileInit.swapFile(nextClipName);

        macro.clear();
        macro.addAll(new ArrayList<>(MacroRecorder.clipBuffer));
        saveMacroToFile();

        CyvForge.sendChatMessage("Clipped last " + macro.size() + " ticks to " + nextClipName);
    }

    public static void saveMacroToFile() {
        if (MacroFileInit.macroFile == null) {
            MacroFileInit.swapFile(CyvClientConfig.getString("currentMacro", "macro"));
        }

        try (FileWriter writer = new FileWriter(MacroFileInit.macroFile)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(macro, writer);
        } catch (Exception e) {
            e.printStackTrace();
            CyvForge.sendChatMessage("Failed to save macro file!");
        }
    }

    public static void runMacro(String[] args) {
        MacroFileInit.swapFile(CyvClientConfig.getString("currentMacro", "macro"));
        if (!Minecraft.getMinecraft().isSingleplayer()) {
            CyvForge.sendChatMessage("No permission to run macro.");
            return;
        }

        try {
            if (macroRunning == 0) {
                Gson gson = new Gson();
                JsonReader reader = new JsonReader(new FileReader(MacroFileInit.macroFile));
                //macro = gson.fromJson(reader, ArrayList.class);

                // transisition fix for when i added RMB
                ArrayList<ArrayList<String>> rawMacro = gson.fromJson(reader, ArrayList.class);
                macro = new ArrayList<>();
                for (ArrayList<String> line : rawMacro) {
                    if (line.size() == 9) {
                        line.add(7, "false");
                    }
                    macro.add(line);
                }

                macroRunning = macro.size() + 1; //MovementListener starts macro
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            CyvForge.sendChatMessage("Macro file doesn't exist.");
        }
    }

    @Override
    public List<String> getTabCompletions(String[] args) {
        if (args.length == 2) return java.util.Arrays.asList("run", "record", "clip");
        if (args.length == 3 && (args[1].equalsIgnoreCase("rec") || args[1].equalsIgnoreCase("record") || args[1].equalsIgnoreCase("recording"))) {
            return java.util.Arrays.asList("start", "stop");
        }
        return new java.util.ArrayList<>();
    }

    public static void addToArray(boolean w, boolean a, boolean s, boolean d, boolean space, boolean sprint, boolean sneak, boolean rmb,  float yaw, float pitch) {
        ArrayList<String> params = new ArrayList<String>();

        params.add(w+"");
        params.add(a+"");
        params.add(s+"");
        params.add(d+"");
        params.add(space+"");
        params.add(sprint+"");
        params.add(sneak+"");
        params.add(rmb+"");
        params.add(yaw+"");
        params.add(pitch+"");


        macro.add(params);
    }

}
