package net.cyvforge.command.mpk;

import net.cyvforge.CyvForge;
import net.cyvforge.event.events.ParkourTickListener;
import net.cyvforge.util.defaults.CyvCommand;
import net.cyvforge.util.parkour.LandingAxis;
import net.cyvforge.util.parkour.LandingBlock;
import net.cyvforge.util.parkour.LandingMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;

import java.util.List;

public class CommandSetlb extends CyvCommand {
    public CommandSetlb() {
        super("setlb");
        this.hasArgs = true;
        this.usage = "[arguments]";
        this.helpString = "Set landing block";
    }

    @Override
    public void run(ICommandSender sender, String[] args) {
        run(args);
    }

    public static void run(String[] args) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;

        new Thread(() -> {
            LandingMode mode = LandingMode.landing;
            LandingAxis axis = LandingAxis.both;
            boolean box = false;
            boolean target = false;
            int targetTick = -1;
            for (String s : args) {
                s = s.toLowerCase();
                if (s.equals("x")) axis = LandingAxis.x;
                else if (s.equals("z")) axis = LandingAxis.z;
                else if (s.equals("land") || s.equals("landing")) mode = LandingMode.landing;
                else if (s.equals("hit")) mode = LandingMode.hit;
                else if (s.equals("xneo") || s.equals("x-neo") || s.equals("neo-x") || s.equals("x_neo") || s.equals("neox")) mode = LandingMode.x_neo;
                else if (s.equals("zneo") || s.equals("z-neo") || s.equals("neo-z") || s.equals("neo") || s.equals("z_neo") || s.equals("neoz")) mode = LandingMode.z_neo;
                else if (s.equals("enter")) mode = LandingMode.enter;
                else if (s.equals("box")) box = true;
                else if (s.equals("target")) target = true;
                //shortcuts
                else if (s.equals("slime") || s.equals("ice") || s.equals("slime/ice")) { box = true; mode = LandingMode.hit; }
                else if (s.equals("ladder") || s.equals("vine") || s.equals("ladder/vine") ) { box = true; mode = LandingMode.enter; }

                if (s.startsWith("tick") || s.startsWith("tier")) {
                    try {
                        targetTick = Integer.parseInt(s.substring(4));
                    } catch (Exception e) {
                        CyvForge.sendChatMessage("Invalid tick format. Use e.g. tick5 or tier5");
                    }
                }
            }

            if (target) {
                net.minecraft.util.Vec3 eyePos = player.getPositionEyes(0);
                net.minecraft.util.Vec3 lookVec = player.getLook(0);
                net.minecraft.util.Vec3 endPos = eyePos.addVector(lookVec.xCoord * 100, lookVec.yCoord * 100, lookVec.zCoord * 100);

                MovingObjectPosition hit = mc.theWorld.rayTraceBlocks(eyePos, endPos, true, false, true);

                if (hit != null && hit.typeOfHit.equals(MovingObjectPosition.MovingObjectType.BLOCK)) {
                    try {
                        BlockPos pos = hit.getBlockPos();
                        List<AxisAlignedBB> list = CyvForge.getHitbox(pos, mc.theWorld);

                        net.minecraft.block.Block block = mc.theWorld.getBlockState(pos).getBlock();

                        boolean isLiquid = block instanceof net.minecraft.block.BlockLiquid;
                        boolean isPassable = block instanceof net.minecraft.block.BlockLadder || block instanceof net.minecraft.block.BlockVine;

                        if (list != null && list.isEmpty() && !isLiquid && !isPassable) {
                            CyvForge.sendChatMessage("Please look at a valid block.");
                            return;
                        } else {
                            ParkourTickListener.landingBlock = new LandingBlock(pos, mode, axis, box);
                            CyvForge.sendChatMessage("Successfully set landing block.");
                        }
                    } catch (Exception e) {
                        CyvForge.sendChatMessage("Please look at a valid block.");
                    }
                } else {
                    CyvForge.sendChatMessage("Please look at a valid block.");
                    return;
                }
            }
            else {
                if (player.onGround) {
                    BlockPos pos = new BlockPos(player.posX, player.posY - 0.05, player.posZ);
                    List<AxisAlignedBB> list = CyvForge.getHitbox(pos, mc.theWorld);

                    net.minecraft.block.Block block = mc.theWorld.getBlockState(pos).getBlock();
                    boolean isPassable = block instanceof net.minecraft.block.BlockLadder || block instanceof net.minecraft.block.BlockVine;

                    if (list != null && list.isEmpty() && !isPassable) {
                        pos = pos.down();
                        list = CyvForge.getHitbox(pos, mc.theWorld);
                    }

                    if (list != null && list.isEmpty() && !isPassable) {
                        CyvForge.sendChatMessage("Please stand on a valid block.");
                        return;
                    } else {
                        ParkourTickListener.landingBlock = new LandingBlock(pos, mode, axis, box);
                        CyvForge.sendChatMessage("Successfully set landing block.");
                    }

                } else {
                    CyvForge.sendChatMessage("Please stand on a valid block.");
                    return;
                }
            }
            ParkourTickListener.landingBlock.targetTick = targetTick;
            if (targetTick != -1) CyvForge.sendChatMessage("Target tick/tier set to: " + targetTick);
        }, "Set landing block").start();
    }

    @Override
    public List<String> getTabCompletions(String[] args) {
        return java.util.Arrays.asList("target", "box", "hit", "enter", "x", "z", "zneo", "xneo", "tick", "ladder/vine", "slime/ice");
    }
}