package net.cyvforge.event.events;

import net.cyvforge.CyvForge;
import net.cyvforge.command.mpk.CommandMacro;
import net.cyvforge.config.CyvClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;

public class MacroListener {
    static boolean macroEnded = false;

    static ArrayList<Float> partialYawChange = new ArrayList<Float>();
    static ArrayList<Float> partialPitchChange = new ArrayList<Float>();

    static double lastPartial = 0;

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        if (!CyvClientConfig.getBoolean("smoothMacro", false)) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP mcPlayer = mc.thePlayer;
        GameSettings options = mc.gameSettings;

        float renderTickTime = event.renderTickTime;

        if (CommandMacro.macroRunning > 1) {
            try {
                if (renderTickTime - lastPartial < 0.1) return;
                int index = CommandMacro.macro.size() - CommandMacro.macroRunning;
                ArrayList<String> macro = CommandMacro.macro.get(index+1);
                double yawChange = Double.parseDouble(macro.get(8)) * (renderTickTime - lastPartial);
                double pitchChange = Double.parseDouble(macro.get(9)) * (renderTickTime - lastPartial);

                double smallestAngle = (float) (1.2 * Math.pow((0.6 * options.mouseSensitivity + 0.2), 3));
                yawChange = smallestAngle * Math.round(yawChange/smallestAngle);
                pitchChange = smallestAngle * Math.round(pitchChange/smallestAngle);
                mcPlayer.rotationYaw += (float) yawChange;
                mcPlayer.rotationPitch += (float) pitchChange;
                lastPartial = renderTickTime;
                partialYawChange.add((float) yawChange);
                partialPitchChange.add((float) pitchChange);
            } catch (Exception f) {
                f.printStackTrace();
            }

        }


    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        GameSettings options = mc.gameSettings;

        //parse json file
        if (CommandMacro.macroRunning != 0) {
            if (mc.isGamePaused()) { //stop macro if the game is paused
                KeyBinding.unPressAllKeys();
                macroEnded = false;
                CommandMacro.macroRunning = 0;
                return;
            }

            //stop the macro if it has reached the end
            if (CommandMacro.macroRunning == 1) {
                try {
                    KeyBinding.unPressAllKeys();
                } catch (Exception f) {
                    f.printStackTrace();
                }
                macroEnded = false;
                CommandMacro.macroRunning = 0;
                return;
            }

            try {
                macroEnded = false;
                int index = CommandMacro.macro.size() - CommandMacro.macroRunning;
                ArrayList<String> macro = CommandMacro.macro.get(index + 1);

                //index starts at 1 and works its way to the length of the macro
                //macro.get(index)[x], x = 0: w, 1: a, 2: s, 3: d, 4: jump, 5: sprint, 6: sneak, 7: RMB, 8/9: yaw/pitch

                KeyBinding.setKeyBindState(options.keyBindForward.getKeyCode(), Boolean.parseBoolean(macro.get(0)));
                KeyBinding.setKeyBindState(options.keyBindLeft.getKeyCode(), Boolean.parseBoolean(macro.get(1)));
                KeyBinding.setKeyBindState(options.keyBindBack.getKeyCode(), Boolean.parseBoolean(macro.get(2)));
                KeyBinding.setKeyBindState(options.keyBindRight.getKeyCode(), Boolean.parseBoolean(macro.get(3)));
                KeyBinding.setKeyBindState(options.keyBindJump.getKeyCode(), Boolean.parseBoolean(macro.get(4)));
                KeyBinding.setKeyBindState(options.keyBindSprint.getKeyCode(), Boolean.parseBoolean(macro.get(5)));
                KeyBinding.setKeyBindState(options.keyBindSneak.getKeyCode(), Boolean.parseBoolean(macro.get(6)));
                KeyBinding.setKeyBindState(options.keyBindUseItem.getKeyCode(), Boolean.parseBoolean(macro.get(7)));

                float yawChange = Float.parseFloat(macro.get(8));
                float pitchChange = Float.parseFloat(macro.get(9));

                //undo partialtick turns
                for (int i = partialYawChange.size() - 1; i >= 0; i--) {
                    player.rotationYaw -= partialYawChange.get(i);
                    player.rotationPitch -= partialPitchChange.get(i);
                }

                player.rotationYaw += yawChange;
                player.rotationPitch += pitchChange;
                partialYawChange.clear(); partialPitchChange.clear(); lastPartial = 0;
                CommandMacro.macroRunning = CommandMacro.macroRunning - 1;

                if (CommandMacro.macroRunning == 0) {
                    macroEnded = true;

                    try {
                        KeyBinding.unPressAllKeys();
                        macroEnded = false;
                    } catch (Exception f) {
                        f.printStackTrace();
                    }

                }

            } catch (Exception e1) {
                CyvForge.sendChatMessage("Error occurred in running macro.");
                e1.printStackTrace();
                CommandMacro.macroRunning = 0;
                KeyBinding.unPressAllKeys();
                macroEnded = false;
            }
        }
    }
}
