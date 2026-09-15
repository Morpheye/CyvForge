package net.cyvforge.util;

import net.cyvforge.config.CyvClientConfig;
import net.cyvforge.gui.config.ConfigPanel;
import net.cyvforge.gui.config.panels.*;
import net.cyvforge.util.defaults.CyvGui;
import java.util.ArrayList;

public class PanelUtils {

    // Action
    public static void addAction(ArrayList<ConfigPanel> panels, String name, Runnable action, CyvGui screen) {
        panels.add(new ConfigPanelAction(panels, name, action, screen));
    }

    public static void addDependantAction(ArrayList<ConfigPanel> panels, String name, Runnable action, String dep, CyvGui screen) {
        panels.add(new ConfigPanelAction(panels, name, action, screen) {
            @Override public boolean isEnabled() { return CyvClientConfig.getBoolean(dep, false); }
        });
    }

    // DecimalEntry
    public static void addDecimal(ArrayList<ConfigPanel> panels, String key, String name, CyvGui screen) {
        panels.add(new ConfigPanelDecimalEntry(panels, key, name, screen));
    }

    public static void addDependantDecimal(ArrayList<ConfigPanel> panels, String key, String name, String dep, CyvGui screen) {
        panels.add(new ConfigPanelDecimalEntry(panels, key, name, screen) {
            @Override public boolean isEnabled() { return CyvClientConfig.getBoolean(dep, false); }
        });
    }

    // EmptySpace
    public static void addSpace(ArrayList<ConfigPanel> panels, CyvGui screen) {
        panels.add(new ConfigPanelEmptySpace(panels, screen));
    }

    // IntegerSlider
    public static void addSlider(ArrayList<ConfigPanel> panels, String key, String name, int min, int max, CyvGui screen) {
        panels.add(new ConfigPanelIntegerSlider(panels, key, name, min, max, screen));
    }

    public static void addSlider(ArrayList<ConfigPanel> panels, String key, String name, int min, int max, CyvGui screen, Runnable onChange) {
        panels.add(new ConfigPanelIntegerSlider(panels, key, name, min, max, screen) {
            @Override public void onValueChange() { if (onChange != null) onChange.run(); }
        });
    }

    public static void addDependantSlider(ArrayList<ConfigPanel> panels, String key, String name, int min, int max, String dep, CyvGui screen) {
        panels.add(new ConfigPanelIntegerSlider(panels, key, name, min, max, screen) {
            @Override public boolean isEnabled() { return CyvClientConfig.getBoolean(dep, false); }
        });
    }

    // OptionSwitcher
    public static <T> void addSwitcher(ArrayList<ConfigPanel> panels, String key, String name, T[] options, CyvGui screen) {
        panels.add(new ConfigPanelOptionSwitcher<T>(panels, key, name, options, screen));
    }

    public static <T> void addSwitcher(ArrayList<ConfigPanel> panels, String key, String name, T[] options, CyvGui screen, Runnable onChange) {
        panels.add(new ConfigPanelOptionSwitcher<T>(panels, key, name, options, screen) {
            @Override public void onValueChange() { if (onChange != null) onChange.run(); }
        });
    }

    public static <T> void addDependantSwitcher(ArrayList<ConfigPanel> panels, String key, String name, T[] options, String dep, CyvGui screen) {
        panels.add(new ConfigPanelOptionSwitcher<T>(panels, key, name, options, screen) {
            @Override public boolean isEnabled() { return CyvClientConfig.getBoolean(dep, false); }
        });
    }

    // StringEntry
    public static void addString(ArrayList<ConfigPanel> panels, String key, String name, CyvGui screen) {
        panels.add(new ConfigPanelStringEntry(panels, key, name, screen));
    }

    public static void addDependantString(ArrayList<ConfigPanel> panels, String key, String name, String dep, CyvGui screen) {
        panels.add(new ConfigPanelStringEntry(panels, key, name, screen) {
            @Override public boolean isEnabled() { return net.cyvforge.config.CyvClientConfig.getBoolean(dep, false); }
        });
    }

    // Toggle
    public static void addToggle(ArrayList<ConfigPanel> panels, String key, String name, CyvGui screen) {
        panels.add(new ConfigPanelToggle(panels, key, name, screen));
    }

    public static void addToggle(ArrayList<ConfigPanel> panels, String key, String name, CyvGui screen, Runnable onChange) {
        panels.add(new ConfigPanelToggle(panels, key, name, screen) {
            @Override public void onValueChange() { if (onChange != null) onChange.run(); }
        });
    }

    public static void addDependantToggle(ArrayList<ConfigPanel> panels, String key, String name, String dep, CyvGui screen) {
        panels.add(new ConfigPanelToggle(panels, key, name, screen) {
            @Override public boolean isEnabled() { return CyvClientConfig.getBoolean(dep, false); }
        });
    }

    public static void addOrDependantToggle(ArrayList<ConfigPanel> panels, String key, String name, String dep1, String dep2, CyvGui screen) {
        panels.add(new ConfigPanelToggle(panels, key, name, screen) {
            @Override public boolean isEnabled() {
                return CyvClientConfig.getBoolean(dep1, false) || CyvClientConfig.getBoolean(dep2, false);
            }
        });
    }

    // ToggleMode
    public static void addToggleMode(ArrayList<ConfigPanel> panels, String key, String name, String s1, String s2, CyvGui screen) {
        panels.add(new ConfigPanelToggleModes(panels, key, name, s1, s2, screen));
    }

    public static void addDependantToggleMode(ArrayList<ConfigPanel> panels, String key, String name, String s1, String s2, String dep, CyvGui screen) {
        panels.add(new ConfigPanelToggleModes(panels, key, name, s1, s2, screen) {
            @Override public boolean isEnabled() { return net.cyvforge.config.CyvClientConfig.getBoolean(dep, false); }
        });
    }
}