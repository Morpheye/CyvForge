package net.cyvforge.util.parkour;

import net.cyvforge.CyvForge;
import net.cyvforge.config.CyvClientConfig;
import net.cyvforge.event.events.ParkourTickListener;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;

import java.text.DecimalFormat;

public class LandingBlockOffset {
    private static Double currentPb;
    private static Double currentXOffset;
    private static Double currentZOffset;
    private static boolean sendChatOffset = true;

    public static void refreshPb() { //called at the start of each tick before checks happen
        currentPb = null;
        currentXOffset = null;
        currentZOffset = null;
        sendChatOffset = false;
    }

    public static void check(double x, double y, double z, double lastx, double lasty, double lastz, double nextX, double nextY, double nextZ, LandingBlock b, int i) {
        Double xOffset = null, zOffset = null;
        Double eventX = null, eventZ = null;

        boolean isForcedTick = (b.targetTick != -1);

        if (!b.isLiquid && !isForcedTick && y < b.bb[i].minY) return; //below bottom y
        else if (!b.isLiquid && !isForcedTick && y < b.bb[i].maxY && b.mode.equals(LandingMode.enter)) { //inside block on y
            xOffset = checkX(x, b, i);
            zOffset = checkZ(z, b, i);
            eventX = x;
            eventZ = z;
        } else if (b.mode.equals(LandingMode.hit) && (b.isLiquid || isForcedTick || (y <= b.bb[i].maxY && lasty >= b.bb[i].maxY))) { //hit tick
            xOffset = checkX(x, b, i);
            zOffset = checkZ(z, b, i);
            eventX = x;
            eventZ = z;
        } else if (b.isLiquid || isForcedTick || (y >= b.bb[i].maxY && (b.mode.equals(LandingMode.z_neo) || b.mode.equals(LandingMode.x_neo) || b.mode.equals(LandingMode.landing)))) {
            xOffset = checkX(x, b, i);
            eventX = x;
            zOffset = checkZ(z, b, i);
            eventZ = z;

            if (b.mode.equals(LandingMode.z_neo)) {
                zOffset = checkZ(lastz, b, i);
                eventZ = lastz;

                if (b.neoAndNormal) {
                    double temp = checkZ(z, b, i);
                    zOffset = temp < zOffset ? temp : zOffset;
                    eventZ = z;
                }
            }
            else if (b.mode.equals(LandingMode.x_neo)) {
                xOffset = checkX(nextX, b, i);
                eventX = nextX;
            }

        }

        if (!isForcedTick) {
            if (eventX == null || eventZ == null || !(eventX <= b.xMaxCond && eventX >= b.xMinCond &&
                    eventZ <= b.zMaxCond && eventZ >= b.zMinCond)) return;
        }

        //finalize x and z offset
        AxisAlignedBB playerBB = Minecraft.getMinecraft().thePlayer.getEntityBoundingBox();
        double playerLengthX = playerBB.maxX - playerBB.minX;
        double playerLengthZ = playerBB.maxZ - playerBB.minZ;

        double pb = calculatePb(xOffset, zOffset, b);
        if (currentPb == null) {
            currentPb = pb;
            currentXOffset = xOffset;
            currentZOffset = zOffset;
            if (((CyvClientConfig.getBoolean("sendLbChatOffset", false) && b == ParkourTickListener.landingBlock) ||
                    (CyvClientConfig.getBoolean("sendMmChatOffset", false) && b == ParkourTickListener.momentumBlock ))
                    && (eventX <= b.xMaxCond + playerLengthX/2 && eventX >= b.xMinCond - playerLengthX/2 &&
                    eventZ <= b.zMaxCond + playerLengthZ/2 && eventZ >= b.zMinCond - playerLengthZ/2)) {
                sendChatOffset = true;
            } else sendChatOffset = false;
        } else if (pb > currentPb) {
            currentPb = pb;
            currentXOffset = xOffset;
            currentZOffset = zOffset;
            if (((CyvClientConfig.getBoolean("sendLbChatOffset", false) && b == ParkourTickListener.landingBlock) ||
                    (CyvClientConfig.getBoolean("sendMmChatOffset", false) && b == ParkourTickListener.momentumBlock ))
                    && (eventX <= b.xMaxCond + playerLengthX/2 && eventX >= b.xMinCond - playerLengthX/2 &&
                    eventZ <= b.zMaxCond + playerLengthZ/2 && eventZ >= b.zMinCond - playerLengthZ/2)) {
                sendChatOffset = true;
            } else sendChatOffset = false;
        }

    }

    public static Double checkX(double x, LandingBlock b, int i) {
        Minecraft mc = Minecraft.getMinecraft();
        AxisAlignedBB playerBB = mc.thePlayer.getEntityBoundingBox();
        double halfPlayerSize = (playerBB.maxX - playerBB.minX) / 2;
        double fullPlayerSize = halfPlayerSize * 2;

        double rightWallOffset = (b.xMinWall == null) ? 0 : (b.bb[i].minX - b.xMinWall) - fullPlayerSize;
        double leftWallOffset = (b.xMaxWall == null) ? 0 : (b.xMaxWall - b.bb[i].maxX) - fullPlayerSize;

        if (leftWallOffset == 0) {
            AxisAlignedBB checkBB = new AxisAlignedBB(b.bb[i].maxX, b.bb[i].maxY, b.bb[i].minZ, b.bb[i].maxX + 0.1, b.bb[i].maxY + 0.5, b.bb[i].maxZ);
            if (!mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, checkBB).isEmpty()) {
                leftWallOffset = -fullPlayerSize;
            }
        }
        if (rightWallOffset == 0) {
            AxisAlignedBB checkBB = new AxisAlignedBB(b.bb[i].minX - 0.1, b.bb[i].maxY, b.bb[i].minZ, b.bb[i].minX, b.bb[i].maxY + 0.5, b.bb[i].maxZ);
            if (!mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, checkBB).isEmpty()) {
                rightWallOffset = -fullPlayerSize;
            }
        }

        if (rightWallOffset > 0) rightWallOffset = 0;
        if (leftWallOffset > 0) leftWallOffset = 0;

        double left = (b.bb[i].maxX + leftWallOffset) - (x) + halfPlayerSize;
        double right = (x) - (b.bb[i].minX - rightWallOffset) + halfPlayerSize;

        if (left > Math.abs(right)) return right;
        if (right > Math.abs(left)) return left;

        return Math.max(left, right);
    }

    public static Double checkZ(double z, LandingBlock b, int i) {
        Minecraft mc = Minecraft.getMinecraft();
        AxisAlignedBB playerBB = mc.thePlayer.getEntityBoundingBox();
        double halfPlayerSize = (playerBB.maxZ - playerBB.minZ) / 2;
        double fullPlayerSize = halfPlayerSize * 2;

        double backWallOffset = (b.zMinWall == null) ? 0 : (b.bb[i].minZ - b.zMinWall) - fullPlayerSize;
        double frontWallOffset = (b.zMaxWall == null) ? 0 : (b.zMaxWall - b.bb[i].maxZ) - fullPlayerSize;

        if (frontWallOffset == 0) {
            AxisAlignedBB checkBB = new AxisAlignedBB(b.bb[i].minX, b.bb[i].maxY, b.bb[i].maxZ, b.bb[i].maxX, b.bb[i].maxY + 0.5, b.bb[i].maxZ + 0.1);
            if (!mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, checkBB).isEmpty()) {
                frontWallOffset = -fullPlayerSize;
            }
        }
        if (backWallOffset == 0) {
            AxisAlignedBB checkBB = new AxisAlignedBB(b.bb[i].minX, b.bb[i].maxY, b.bb[i].minZ - 0.1, b.bb[i].maxX, b.bb[i].maxY + 0.5, b.bb[i].minZ);
            if (!mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, checkBB).isEmpty()) {
                backWallOffset = -fullPlayerSize;
            }
        }

        if (backWallOffset > 0) backWallOffset = 0;
        if (frontWallOffset > 0) frontWallOffset = 0;

        double front = (b.bb[i].maxZ + frontWallOffset) - (z) + halfPlayerSize;
        double back = (z) - (b.bb[i].minZ - backWallOffset) + halfPlayerSize;

        if (front > Math.abs(back)) return back;
        if (back > Math.abs(front)) return front;

        return Math.max(front, back);
    }

    public static Double calculatePb(Double x, Double z, LandingBlock b) {
        if (x == null) return z;
        else if (z == null) return x;
        else if (b.axis.equals(LandingAxis.x)) return x;
        else if (b.axis.equals(LandingAxis.z)) return z;
        else if (x < 0 && z < 0) return -Math.hypot(x, z);
        else if (x < 0) return x;
        else if (z < 0) return z;
        else return Math.hypot(x, z);
    }

    public static void finalizePb(LandingBlock b) {
        if (currentPb == null) return;
        Double tempLastPb = b.lastPb;

        boolean momentumCancel = CyvClientConfig.getBoolean("momentumPbCancelling", false) &&
                (ParkourTickListener.momentumBlock != null && ParkourTickListener.momentumBlock.lastPb != null
                        && ParkourTickListener.momentumBlock.lastPb < 0);

        b.lastPb = currentPb;
        if (b.lastPb != null) {
            if (currentXOffset != null) {
                b.lastOffsetX = currentXOffset;
            }
            if (currentZOffset != null) {
                b.lastOffsetZ = currentZOffset;
            }

            /*
            if (currentPb.doubleValue() >= 0 && !momentumCancel) {
                if (!(tempLastPb != null && tempLastPb > 0
                        && b.targetBlock instanceof BlockSlime
                        && System.currentTimeMillis() - b.lastLandingTime <
                        ModManager.getMod(ModAntiCP.class).delay*1000)) { //[CyvClient] fix slime issue
                    b.lastLandingTime = System.currentTimeMillis();
                    b.lastLandingHotbarSlot = Minecraft.getMinecraft().thePlayer.inventory.currentItem;

                }
                b.sentMessage = false;
            }

             */

            if ((b.pb == null || b.lastPb > b.pb) && b == ParkourTickListener.landingBlock && !momentumCancel) { //must be landing block
                b.pb = b.lastPb;
                b.pbX = currentXOffset;
                b.pbZ = currentZOffset;
                DecimalFormat df = CyvForge.df;
                CyvForge.sendChatMessage("New pb! " + df.format(b.pb.doubleValue()));

                if (b.pb >= 0) {
                    net.cyvforge.event.events.ParkourTickListener.triggerAntiCP();
                }
            }
        }

        if (sendChatOffset && !momentumCancel) {
            if (currentXOffset != null && b.axis != LandingAxis.z)
                CyvForge.sendChatMessage("X Offset: " + CyvForge.df.format(currentXOffset.doubleValue()));
            if (currentZOffset != null && b.axis != LandingAxis.x)
                CyvForge.sendChatMessage("Z Offset: " + CyvForge.df.format(currentZOffset.doubleValue()));
        }

    }

}
