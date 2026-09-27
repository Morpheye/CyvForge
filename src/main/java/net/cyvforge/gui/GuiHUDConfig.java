package net.cyvforge.gui;

import net.cyvforge.gui.config.ConfigPanel;
import net.cyvforge.gui.config.panels.*;
import net.cyvforge.hud.structure.DraggableHUDElement;
import net.cyvforge.util.defaults.CyvGui;
import net.cyvforge.util.PanelUtils;
import net.cyvforge.config.CyvClientConfig;
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
                PanelUtils.addDependantToggle(panels, "strafejamJamOnly", "- Strafejam Jam only", "detectStrafejam", screen);
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

            case "InputHistory":
                PanelUtils.addSlider(panels, "ih_size", "Size", 50, 100, screen);
                PanelUtils.addSlider(panels, "ih_historyLength", "History Length", 1, 50, screen);
                PanelUtils.addToggle(panels, "ih_alwaysVisible", "Inputs Always Visible", screen);

                panels.add(new ConfigPanelIntegerSlider(panels, "ih_visLength", "Visibility Length (ticks)", 10, 200, screen) {
                    @Override public boolean isEnabled() { return !CyvClientConfig.getBoolean("- ih_alwaysVisible", true); }
                });

                PanelUtils.addSwitcher(panels, "ih_colorMode", "Text Color", new String[]{"White", "Color1 - Color2", "Color2 - Color1"}, screen);

                PanelUtils.addToggle(panels, "ih_invertList", "Invert List", screen);
                PanelUtils.addToggle(panels, "ih_trackFacing", "Track Facing", screen);
                PanelUtils.addToggle(panels, "ih_trackTurnAmount", "Track Turn Amount", screen);
                PanelUtils.addOrDependantToggle(panels, "ih_noYawBreak", "- Don't Split on Yaw Change", "ih_trackFacing", "ih_trackTurnAmount", screen);
                PanelUtils.addToggle(panels, "ih_inputRelease", "Input Release (Jmp/Spr)", screen);

                PanelUtils.addToggle(panels, "ih_groundJumpReset", "Ground Jump Reset", screen);
                PanelUtils.addDependantToggle(panels, "ih_treatLandingAsAir", "- Treat Landing Tick as Air", "ih_groundJumpReset", screen);
                PanelUtils.addDependantToggle(panels, "ih_jumpNoBreak", "- No Line Split on Jump", "ih_groundJumpReset", screen);
                PanelUtils.addDependantToggle(panels, "ih_pressReleaseTicks", "- Show Press Release Ticks", "ih_groundJumpReset", screen);
                PanelUtils.addDependantToggle(panels, "ih_airGroundIndicator", "- Air Ground Indicator", "ih_groundJumpReset", screen);

                PanelUtils.addToggle(panels, "ih_ignoreSprUntilInput", "Ignore Spr Until Input", screen);
                PanelUtils.addToggle(panels, "ih_resetRMB", "Reset History (Right Click)", screen);
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