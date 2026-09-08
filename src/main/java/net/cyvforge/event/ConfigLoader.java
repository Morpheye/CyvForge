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
        try {
            BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile)));
            String s = "";
            while ((s = bufferedreader.readLine()) != null) {
                String[] parts = s.split("=");
                try {
                    if (cfg.configFields.containsKey(parts[0])) {
                        cfg.configFields.get(parts[0]).set(parts[1]);
                    } else {
                        //key doesn't exist
                    }
                } catch (Exception e) {
                    LogManager.getLogger().info("Config option \"" + Arrays.toString(parts) + "\" failed to load.");
                }

            }

            bufferedreader.close();

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
            mod.readConfigFields();
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
            mod.saveConfigFields();
        }

        try {
            FileWriter writer = new FileWriter(configFile, false);

            cfg.configFields.forEach((name, data) -> {
                try {
                    writer.write(name + "=" + data.value.toString() + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            LogManager.getLogger().info("CyvForge config saved!");
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
