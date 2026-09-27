package net.cyvforge.event;

import net.cyvforge.CyvForge;
import net.cyvforge.config.ColorTheme;
import net.cyvforge.config.CyvClientColorHelper;
import net.cyvforge.config.CyvClientConfig;
import net.cyvforge.hud.HUDManager;
import net.cyvforge.hud.structure.DraggableHUDElement;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;

public class ConfigLoader {
    public static final String NAME = "config.txt";
    public static final String PATH = "config/cyvforge/";
    public static final String FILEPATH = PATH + NAME;

    public static File configFile;

    public static void init(CyvClientConfig cfg) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> save(CyvForge.config, true)));

        File dir = new File(PATH);
        if (!dir.exists()) dir.mkdirs();
        dir = new File(PATH);
        configFile = new File(FILEPATH);

        try {
            configFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        LogManager.getLogger().info("Config file loaded!");

        read(cfg);

    }

    public static void read(CyvClientConfig cfg) {
        try (BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile)))) {
            String s;
            while ((s = bufferedreader.readLine()) != null) {
                String[] parts = s.split("=", 2);
                try {
                    if (parts.length == 2 && cfg.configFields.containsKey(parts[0])) {
                        cfg.configFields.get(parts[0]).set(parts[1]);
                    }
                } catch (Exception e) {
                    LogManager.getLogger().info("Config option \"" + Arrays.toString(parts) + "\" failed to load.");
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //set colors
        CyvClientColorHelper.checkColor(CyvClientConfig.getString("color1", "aqua"), CyvClientConfig.getString("color2", "white"));

        //set theme
        try {
            CyvForge.theme = ColorTheme.valueOf(CyvClientConfig.getString("theme", "CYVISPIRIA"));
        } catch (Exception e) {
            CyvForge.theme = ColorTheme.CYVISPIRIA;
        }

        for (DraggableHUDElement mod : HUDManager.registeredRenderers) {
            try {
                mod.readConfigFields();
            } catch (Exception e) {
                LogManager.getLogger().warn("HUD element " + mod.getClass().getSimpleName()
                        + " threw while reading its fields, using defaults for it", e);
            }
        }

        //decimal precision
        CyvForge.df.setMinimumIntegerDigits(1);
        if (CyvClientConfig.getBoolean("trimZeroes", true)) CyvForge.df.setMinimumFractionDigits(0);
        else CyvForge.df.setMinimumFractionDigits(CyvClientConfig.getInt("df",5));
        CyvForge.df.setMaximumFractionDigits(CyvClientConfig.getInt("df",5));
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setDecimalSeparator('.');
        CyvForge.df.setDecimalFormatSymbols(s);

    }

    public static void save(CyvClientConfig cfg, boolean isFinal) {
        for (DraggableHUDElement mod : HUDManager.registeredRenderers) {
            try {
                mod.saveConfigFields();
            } catch (Exception e) {
                LogManager.getLogger().warn("HUD element " + mod.getClass().getSimpleName()
                        + " threw while saving its fields, skipping it", e);
            }
        }

        StringBuilder sb = new StringBuilder();
        cfg.configFields.forEach((name, data) -> {
            String value = (data.value != null) ? data.value.toString() : "";
            sb.append(name).append('=').append(value).append('\n');
        });

        File tmp = new File(PATH, NAME + ".tmp");
        try (FileWriter writer = new FileWriter(tmp, false)) {
            writer.write(sb.toString());
        } catch (IOException e) {
            LogManager.getLogger().error("Failed to save the configuration to the temporary file – the old config.txt remains unchanged.", e);
            return;
        }

        try {
            java.nio.file.Files.move(tmp.toPath(), configFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            LogManager.getLogger().info("CyvForge config saved!");
        } catch (IOException e) {
            LogManager.getLogger().error("Failed to replace config.txt", e);
        }
    }
}
