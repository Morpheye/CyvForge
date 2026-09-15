package net.cyvforge.event;

import net.cyvforge.command.CommandGui;
import net.cyvforge.command.CommandHelp;
import net.cyvforge.command.CommandInertia;
import net.cyvforge.command.CommandPositionChecker;
import net.cyvforge.command.calculations.*;
import net.cyvforge.command.checkpoint.CommandCheckpoint;
import net.cyvforge.command.checkpoint.CommandGiveCheckpointGenerator;
import net.cyvforge.command.config.*;
import net.cyvforge.command.mpk.*;
import net.cyvforge.util.defaults.CyvCommand;
import net.cyvforge.util.defaults.DefaultCommand;
import net.minecraftforge.client.ClientCommandHandler;

import java.util.ArrayList;
import java.util.Arrays;

public class CommandInitializer  {
    public static ArrayList<CyvCommand> cyvCommands = new ArrayList<CyvCommand>(); //all commands

    //add all commands in
    public static void register() {
        CyvCommand[] e = new CyvCommand[] {};

        cyvCommands.addAll(Arrays.asList(new CyvCommand[]{ //config commands
                new CommandHelp(), new CommandColor1(), new CommandColor2(), new CommandColors(), new CommandDf(),
                new CommandConfig(), new CommandGui(), new CommandInertia(), new CommandPositionChecker(),
                new CommandFullbright(), new CommandResetGui(), new CommandTogglesprint()
        }));

        cyvCommands.addAll(Arrays.asList(new CyvCommand[]{ //config commands
                new CommandSetlb(), new CommandClearlb(), new CommandSetmm(), new CommandClearmm(), new CommandClearpb(),
                new CommandSetbox(), new CommandSetcond(), new CommandLb(), new CommandMm()
        }));

        cyvCommands.addAll(Arrays.asList(new CyvCommand[] { //mm commands
                new CommandAirtime(), new CommandCalculate(), new CommandDistance(), new CommandHeight(),
                new CommandSimulate(), new CommandSetSensitivity(), new CommandOptimizeSensitivity(), new CommandCheckpoint(),
                new CommandGiveCheckpointGenerator()
        }));

        cyvCommands.add(new CommandMacro());

        ClientCommandHandler.instance.registerCommand(new DefaultCommand());
    }

}
