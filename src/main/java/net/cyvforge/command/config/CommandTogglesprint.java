package net.cyvforge.command.config;

import net.cyvforge.CyvForge;
import net.cyvforge.config.CyvClientConfig;
import net.cyvforge.keybinding.KeybindingTogglesprint;
import net.cyvforge.util.defaults.CyvCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.ICommandSender;

public class CommandTogglesprint extends CyvCommand {
    public CommandTogglesprint() {
        super("togglesprint");
        this.hasArgs = false;
        this.helpString = "Toggles auto-sprint mode.";

        this.aliases.add("ts");
        this.aliases.add("sprint");
    }

    @Override
    public void run(ICommandSender sender, String[] args) {
        boolean newState = !KeybindingTogglesprint.sprintToggled;
        KeybindingTogglesprint.sprintToggled = newState;

        CyvClientConfig.set("togglesprint", newState);
        net.cyvforge.event.ConfigLoader.save(CyvForge.config, false);

        GameSettings settings = Minecraft.getMinecraft().gameSettings;
        if (!newState) {
            KeyBinding.setKeyBindState(settings.keyBindSprint.getKeyCode(), false);
        }

        String status = newState ? "enabled" : "disabled";
        CyvForge.sendChatMessage("Toggle sprint has been " + status + ".");
    }
}