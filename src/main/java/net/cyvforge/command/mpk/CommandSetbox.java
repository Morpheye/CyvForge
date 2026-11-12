package net.cyvforge.command.mpk;

import net.cyvforge.CyvForge;
import net.cyvforge.event.events.ParkourTickListener;
import net.cyvforge.util.defaults.CyvCommand;
import net.cyvforge.util.parkour.LandingBlock;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.AxisAlignedBB;

public class CommandSetbox extends CyvCommand {
    public CommandSetbox() {
        super("setbox");
        this.usage = "<x1> <x2> <y1> <y2> <z1> <z2>";
        this.helpString = "Creates landing zone with set dimensions.";
    }

    @Override
    public void run(ICommandSender sender, String[] args) {
        try {
            double x1 = Double.parseDouble(args[0]);
            double x2 = Double.parseDouble(args[1]);
            double y1 = Double.parseDouble(args[2]);
            double y2 = Double.parseDouble(args[3]);
            double z1 = Double.parseDouble(args[4]);
            double z2 = Double.parseDouble(args[5]);

            double minX = Math.min(x1, x2);
            double maxX = Math.max(x1, x2);
            double minY = Math.min(y1, y2);
            double maxY = Math.max(y1, y2);
            double minZ = Math.min(z1, z2);
            double maxZ = Math.max(z1, z2);

            AxisAlignedBB mathBox = new AxisAlignedBB(minX + 0.3, minY, minZ + 0.3,
                    maxX - 0.3, maxY, maxZ - 0.3);

            LandingBlock lb;
            if (ParkourTickListener.landingBlock != null) {
                lb = ParkourTickListener.landingBlock;
                lb.bb = new AxisAlignedBB[]{mathBox};
                lb.isLiquid = false;
                CyvForge.sendChatMessage("Landing box changed.");
            } else {
                lb = new LandingBlock(mathBox);
                lb.isLiquid = false;
                ParkourTickListener.landingBlock = lb;
                CyvForge.sendChatMessage("Landing box set.");
            }

            lb.xMinCond = minX - 0.6;
            lb.xMaxCond = maxX + 0.6;
            lb.zMinCond = minZ - 0.6;
            lb.zMaxCond = maxZ + 0.6;

            lb.xMinWalls = new Double[]{minX - 0.3};
            lb.xMaxWalls = new Double[]{maxX + 0.3};
            lb.zMinWalls = new Double[]{minZ - 0.3};
            lb.zMaxWalls = new Double[]{maxZ + 0.3};

        } catch (Exception e) {
            e.printStackTrace();
            CyvForge.sendChatMessage("Invalid setbox syntax.");
        }
    }
}