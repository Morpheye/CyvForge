package net.cyvforge.gui;

import net.cyvforge.gui.config.ConfigPanel;
import net.cyvforge.gui.config.panels.*;
import net.cyvforge.hud.structure.DraggableHUDElement;
import net.cyvforge.util.defaults.CyvGui;
import net.cyvforge.util.PanelUtils;
import java.util.ArrayList;

public class GuiHUDConfig {

    public static void loadSettingsFor(DraggableHUDElement label, ArrayList<ConfigPanel> panels, CyvGui screen) {
        String name = label.getName();

        switch (name) {
            case "labelYaw":
                PanelUtils.addToggle(panels, "showFacingAxis", "Show Axis", screen);
                PanelUtils.addToggle(panels, "frameBased", "Frame Based", screen);
                break;

            case "labelLastTiming":
                PanelUtils.addToggle(panels, "showMilliseconds", "Show Milliseconds", screen);
                PanelUtils.addToggle(panels, "detectWobble", "Wobble Timing", screen);
                PanelUtils.addToggle(panels, "detectStrafejam", "Strafejam Timing", screen);
                PanelUtils.addDependantToggle(panels, "strafejamJamOnly", "Strafejam Jam only", "detectStrafejam", screen);
                PanelUtils.addSwitcher(panels, "markDisplay", "Mark Display", new String[] {"Sidestep", "Timing"}, screen);
                break;

            case "labelLastSidestep":
                PanelUtils.addSwitcher(panels, "markDisplay", "Mark Display", new String[] {"Sidestep", "Timing"}, screen);
                break;

            case "labelLastInput":
                PanelUtils.addToggle(panels, "WADdisplay", "WAD Display ", screen);
                break;

            case "keystrokes":
                panels.add(new ConfigPanelIntegerSlider(panels, "keystrokesSize", "Size", 40, 200, screen));
                break;

            case "turnHUDMaster":
                PanelUtils.addSlider(panels, "turnHUDAngleMin", "Min Angle", 1, 12, screen);
                PanelUtils.addSlider(panels, "turnHUDAngleMax", "Max Angle", 1, 12, screen);
                PanelUtils.addToggle(panels, "splitTurningHUD", "Split the Ticks", screen);
                break;

            case "labelBlips":
                PanelUtils.addToggle(panels, "simpleBlip", "Simplified Format", screen);
                break;

            case "labelRuntime":
                PanelUtils.addToggle(panels, "resetRunOnLand", "Reset on Land", screen);
                PanelUtils.addToggle(panels, "hideRuntimeIfZero", "Hide if 0", screen);
                PanelUtils.addToggle(panels, "hideRuntimeLabelName", "Hide Label Name", screen);
                break;
        }
    }
}