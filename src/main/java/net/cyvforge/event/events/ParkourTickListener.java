package net.cyvforge.event.events;

import net.cyvforge.CyvForge;
import net.cyvforge.config.CyvClientConfig;
import net.cyvforge.util.RenderUtils;
import net.cyvforge.util.parkour.CheckpointTeleport;
import net.cyvforge.util.parkour.LandingBlock;
import net.cyvforge.util.parkour.LandingBlockOffset;
import net.cyvforge.util.parkour.LandingMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.command.CommandBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.text.DecimalFormat;

public class ParkourTickListener {
    public static long lastPbTimestamp = 0;
    public static int airtime = 0;
    public static PosTick lastTick = new PosTick(0, 0, 0, 0, new boolean[] {false, false, false, false, false, false, false});
    public static PosTick secondLastTick = new PosTick(0, 0, 0, 0, new boolean[] {false, false, false, false, false, false, false});
    public static PosTick thirdLastTick = new PosTick(0, 0, 0, 0, new boolean[] {false, false, false, false, false, false, false});
    public static boolean pendingWaterCheck = false;
    public static int waterDetectionLockout = 0;
    private static long jumpTimestamp = 0;
    private static long moveTimestamp = 0;
    private static long sprintTimestamp = 0;
    private static long releaseTimestamp = 0;
    private static int stopCounter = 0;
    private static int waitCounter = 0;
    public static int runCounter = 0;
    public static boolean hasInputtedSinceReset = false;

    public static int lastAirtime;
    public static double x = 0, y = 0, z = 0; //coords
    public static double vx = 0, vy = 0, vz = 0; //velocities

    public static float f = 0, p = 0; //yaw and pitch
    public static float vf = 0, vp = 0; //last turnings

    public static double lx = 0, ly = 0, lz = 0; //landings
    public static double hx = 0, hy = 0, hz = 0; //hits
    public static double jx = 0, jy = 0, jz = 0; //jump
    public static float hf = 0; //hit facing
    public static double hvx = 0, hvz = 0; //hit velocities

    public static float jf = 0, jp = 0; //jump angles
    public static float pf = 0, pp = 0; //preturn angles

    // turning angles
    public static float[] turningAngles = new float[12];

    //inertia
    public static double stored_v = 0;
    public static float stored_slip = 1;

    //landing block & other labels
    public static LandingBlock landingBlock = null;
    public static LandingBlock momentumBlock = null;

    public static String lastTiming = "";
    public static int blips = 0;
    public static boolean positionCheckedThisJump = false;
    public static boolean inertiaCheckedThisJump = false;

    public static double lastBlipHeight = 0;

    public static int grinds = 0;
    private static boolean grindStarted = false;

    public static float last45 = 0;
    public static double lastTurning = 0;

    public static int sidestep = 0; //0 = wad 1 = wdwa
    public static int sidestepTime = -1;

    //Timings
    private static int lastJumpTime = -1;
    private static int lastGroundMoveTime = -1;
    private static int lastMoveTime = -1;
    private static int lastSprintTime = -1;
    private static int lastSneakTime = -2;
    public static int lastStopTime = 0;
    public static int lastWaitTime = 0;
    public static int lastRunTime = 0;

    private static long earliestMoveTimestamp;
    private static boolean locked = false;
    private static boolean hasActed = false;
    private static boolean hasCollided = false;


    @SubscribeEvent
    public void onKeyInput(net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.currentScreen != null) return;

        int keyCode = org.lwjgl.input.Keyboard.getEventKey();
        long eventTime = org.lwjgl.input.Keyboard.getEventNanoseconds();

        if (org.lwjgl.input.Keyboard.getEventKeyState()) {
            if (keyCode == mc.gameSettings.keyBindForward.getKeyCode() ||
                    keyCode == mc.gameSettings.keyBindLeft.getKeyCode() ||
                    keyCode == mc.gameSettings.keyBindRight.getKeyCode() ||
                    keyCode == mc.gameSettings.keyBindBack.getKeyCode()) {
                moveTimestamp = eventTime;
            }
            if (keyCode == mc.gameSettings.keyBindJump.getKeyCode()) {
                jumpTimestamp = eventTime;
            }
            if (keyCode == mc.gameSettings.keyBindSprint.getKeyCode()) {
                sprintTimestamp = eventTime;
            }
        } else {
            if (keyCode == mc.gameSettings.keyBindForward.getKeyCode() ||
                    keyCode == mc.gameSettings.keyBindLeft.getKeyCode() ||
                    keyCode == mc.gameSettings.keyBindRight.getKeyCode() ||
                    keyCode == mc.gameSettings.keyBindBack.getKeyCode()) {
                releaseTimestamp = eventTime;
            }
        }
    }

    //end of tick
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP mcPlayer = mc.thePlayer;
        GameSettings gameSettings = mc.gameSettings;

        if (mcPlayer == null || mc.theWorld == null || mc.isGamePaused()) return;

        calculateLastTiming();
        doCheckpoints();

        boolean W = gameSettings.keyBindForward.isKeyDown();
        boolean S = gameSettings.keyBindBack.isKeyDown();
        boolean A = gameSettings.keyBindLeft.isKeyDown();
        boolean D = gameSettings.keyBindRight.isKeyDown();
        boolean Space = gameSettings.keyBindJump.isKeyDown();
        boolean Sneak = gameSettings.keyBindSneak.isKeyDown();

        boolean hasEffectiveMovement = (W != S) || (A != D);
        boolean isAnyInput = W || S || A || D || Space || Sneak;

        if (isAnyInput) hasInputtedSinceReset = true;

        if (hasInputtedSinceReset) {
            // runtime
            if (lastTick != null && !lastTick.onGround && mcPlayer.onGround) {
                if (CyvClientConfig.getBoolean("resetRunOnLand", false)) {
                    lastRunTime = 0;
                    runCounter = 0;
                }
            }

            if (lastTick != null && lastTick.onGround && !mcPlayer.onGround) {
                if (runCounter > 0) {
                    lastRunTime = runCounter;
                }
                runCounter = 0;
            }

            if (mcPlayer.onGround) {
                if (hasEffectiveMovement && !Space) {
                    if (lastTick != null && lastTick.onGround) {
                        runCounter++;
                        lastRunTime = runCounter;
                    }
                } else {
                    runCounter = 0;
                }
            }

            // stoptime
            if (!hasEffectiveMovement) {
                stopCounter++;
            } else {
                if (stopCounter > 0) {
                    lastStopTime = stopCounter;
                    stopCounter = 0;
                }
            }

            // waittime
            if (mcPlayer.onGround && !hasEffectiveMovement && !Space && !Sneak && lastTick != null && lastTick.onGround) {
                waitCounter++;
            } else {
                if (waitCounter > 0) lastWaitTime = waitCounter;
                waitCounter = 0;
                if (!mcPlayer.onGround) waitCounter = 0;
            }
        }

        if (lastTick.hasCollidedHorizontally && !hasCollided) {
            hasCollided = true;
        }

        if (waterDetectionLockout > 0) waterDetectionLockout--;

        if (lastTick != null) {
            if ((!lastTick.onGround || !mcPlayer.onGround) && !mcPlayer.capabilities.isFlying) airtime++;

            x = mcPlayer.posX;
            y = mcPlayer.posY;
            z = mcPlayer.posZ;
            f = mcPlayer.rotationYaw; //note: actual yaw and pitch are delayed by a tick
            p = mcPlayer.rotationPitch;

            vx = x - lastTick.x;
            vy = y - lastTick.y;
            vz = z - lastTick.z;
            vf = f - lastTick.f;
            vp = p - lastTick.p;

            checkInertia();
            checkPosition();

        }

        if (airtime == 1) { //jump tick
            if (mcPlayer.movementInput.jump && vy >= 0) {
                jx = x;
                jy = y;
                jz = z;

                jf = f;
                jp = f;

                if (lastTick != null && secondLastTick != null) {
                    pf = lastTick.f - secondLastTick.f;
                    pp = lastTick.p - secondLastTick.p;
                }

                //grinds
                if (y == lastTick.y && vy == 0) {
                    if (!grindStarted) {
                        grindStarted = true;
                        grinds = 0;
                    }
                    grinds++;

                } else {
                    if (!grindStarted) grinds = 0;
                }

                //blips
                if (lastTick.onGround && !secondLastTick.onGround && lastTick.vy == 0 && (lastTick.y % 0.015625 != 0) && lastTick.airtime > 1) {
                    blips++;

                    lastBlipHeight = lastTick.y;

                } else {
                    blips = 0;
                }
            }

        }

        //last 45
        if (lastTick.keys[0] && ((lastTick.keys[1] && lastTick.keys[3]) || (!lastTick.keys[1] && !lastTick.keys[3]))
                && mcPlayer.movementInput.moveStrafe != 0 && mcPlayer.movementInput.moveForward != 0 && !mcPlayer.onGround) {
            last45 = f - lastTick.f;
        }

        //last turning
        if (f != lastTick.f) lastTurning = f - lastTick.f;

        // update turning angles
        if (airtime >= 1 && airtime <= 12) {
            turningAngles[airtime - 1] = f - lastTick.f;
        }

        //hit tick
        if (lastTick != null && mcPlayer.onGround && !lastTick.onGround && vy < 0) {
            lx = lastTick.x;
            ly = lastTick.y;
            lz = lastTick.z;

            hx = x;
            hy = y;
            hz = z;
            hf = f;
            hvx = vx;
            hvz = vz;

        }

        if (landingBlock != null) { //must be lower than the landing to check it

            if (landingBlock.targetTick != -1) {
                if (airtime == landingBlock.targetTick && !positionCheckedThisJump) {
                    LandingBlockOffset.refreshPb();

                    LandingBlockOffset.check(x, y, z, x + vx, y + vy, z + vz, lastTick.x, lastTick.y, lastTick.z, landingBlock, 0);

                    LandingBlockOffset.finalizePb(landingBlock);
                    positionCheckedThisJump = true;
                }
            } else if (landingBlock.isLiquid) {
                double d = 0.7;
                double bottomOffset = landingBlock.liquidBottomOffset;
                boolean inZone = (x >= landingBlock.pos.getX() - d && x <= landingBlock.pos.getX() + 1 + d) &&
                        (y >= landingBlock.pos.getY() - d - bottomOffset && y <= landingBlock.pos.getY() + 1 + d) &&
                        (z >= landingBlock.pos.getZ() - d && z <= landingBlock.pos.getZ() + 1 + d);

                if (inZone && !mcPlayer.onGround && !positionCheckedThisJump && waterDetectionLockout == 0) {
                    LandingBlockOffset.refreshPb();
                    LandingBlockOffset.check(x, y, z, x + vx, y + vy, z + vz, lastTick.x, lastTick.y, lastTick.z, landingBlock, 0);

                        LandingBlockOffset.finalizePb(landingBlock);
                        if (landingBlock.lastPb != null && landingBlock.lastPb >= 0) {
                            positionCheckedThisJump = true;
                        }
                }
            } else {
                LandingBlockOffset.refreshPb();

                for (int i=0; i<landingBlock.bb.length; i++) {
                    if ((landingBlock.mode.equals(LandingMode.enter) && y <= landingBlock.bb[i].maxY && (y > landingBlock.bb[i].minY)) ||
                            !landingBlock.mode.equals(LandingMode.enter) && y <= landingBlock.bb[i].maxY && (lastTick.y > landingBlock.bb[i].maxY)) {
                        //check the previous ticks

                        if (vy < 0 && airtime > 1) {
                            if (landingBlock.mode.equals(LandingMode.hit) || landingBlock.mode.equals(LandingMode.enter)) {
                                LandingBlockOffset.check(x, y, z, lastTick.x,
                                        lastTick.y, lastTick.z, x, y, z, landingBlock, i);
                            } else {
                                LandingBlockOffset.check(lastTick.x, lastTick.y, lastTick.z, secondLastTick.x,
                                        secondLastTick.y, secondLastTick.z, x, y, z, landingBlock, i);
                            }
                        }
                    }
                }

                LandingBlockOffset.finalizePb(landingBlock);
            }
        }

        if (momentumBlock != null) { //must be lower than the landing to check it

            if (momentumBlock.targetTick != -1) {
                if (airtime == momentumBlock.targetTick && !positionCheckedThisJump) {
                    LandingBlockOffset.refreshPb();

                    LandingBlockOffset.check(x, y, z, x + vx, y + vy, z + vz, lastTick.x, lastTick.y, lastTick.z, momentumBlock, 0);

                    LandingBlockOffset.finalizePb(momentumBlock);
                    positionCheckedThisJump = true;
                }
            } else if (momentumBlock.isLiquid) {
                double d = 0.7;
                double bottomOffset = momentumBlock.liquidBottomOffset;
                boolean inZone = (x >= momentumBlock.pos.getX() - d && x <= momentumBlock.pos.getX() + 1 + d) &&
                        (y >= momentumBlock.pos.getY() - d - bottomOffset  && y <= momentumBlock.pos.getY() + 1 + d) &&
                        (z >= momentumBlock.pos.getZ() - d && z <= momentumBlock.pos.getZ() + 1 + d);

                if (inZone && !mcPlayer.onGround  && !positionCheckedThisJump && waterDetectionLockout == 0) {
                    LandingBlockOffset.refreshPb();
                    LandingBlockOffset.check(x, y, z, x + vx, y + vy, z + vz, lastTick.x, lastTick.y, lastTick.z, momentumBlock, 0);

                        LandingBlockOffset.finalizePb(momentumBlock);
                        if (momentumBlock.lastPb != null && momentumBlock.lastPb >= 0) {
                            positionCheckedThisJump = true;
                        }
                }
            } else {
                LandingBlockOffset.refreshPb();

                for (int i=0; i<momentumBlock.bb.length; i++) {
                    if ((momentumBlock.mode.equals(LandingMode.enter) && y <= momentumBlock.bb[i].maxY && (y > momentumBlock.bb[i].minY)) ||
                            !momentumBlock.mode.equals(LandingMode.enter) && y <= momentumBlock.bb[i].maxY && (lastTick.y > momentumBlock.bb[i].maxY)) {
                        //check the previous ticks

                        if (vy < 0 && airtime > 1) {
                            if (momentumBlock.mode.equals(LandingMode.hit) || momentumBlock.mode.equals(LandingMode.enter)) {
                                LandingBlockOffset.check(x, y, z, lastTick.x,
                                        lastTick.y, lastTick.z, x, y, z, momentumBlock, i);
                            } else {
                                LandingBlockOffset.check(lastTick.x, lastTick.y, lastTick.z, secondLastTick.x,
                                        secondLastTick.y, secondLastTick.z, x, y, z, momentumBlock, i);
                            }
                        }
                    }
                }

                LandingBlockOffset.finalizePb(momentumBlock);
            }
        }

        boolean[] keys = new boolean[] {gameSettings.keyBindForward.isKeyDown(),
                gameSettings.keyBindLeft.isKeyDown(),
                gameSettings.keyBindBack.isKeyDown(), gameSettings.keyBindRight.isKeyDown(),
                gameSettings.keyBindJump.isKeyDown(), gameSettings.keyBindSprint.isKeyDown(),
                gameSettings.keyBindSneak.isKeyDown()};

        thirdLastTick = secondLastTick;
        secondLastTick = lastTick;
        lastTick = new PosTick(mcPlayer, vx, vy, vz, airtime, keys);
        lastTick.true_vx = mcPlayer.motionX;
        lastTick.true_vy = mcPlayer.motionY;
        lastTick.true_vz = mcPlayer.motionZ;
        lastTick.hasCollidedHorizontally = mcPlayer.isCollidedHorizontally;
        if (lastTick.onGround) {
            if (airtime != 0) lastAirtime = airtime;
            airtime = 0;

            positionCheckedThisJump = false;
            inertiaCheckedThisJump = false;
            pendingWaterCheck = false;
        }
        else lastAirtime = airtime;

    }

    private static void checkInertia() {
        if (!CyvClientConfig.getBoolean("inertiaEnabled", false)) return;
        int inertiaTick = CyvClientConfig.getInt("inertiaTick", 4);
        char inertiaAxis = CyvClientConfig.getChar("inertiaAxis", 'x');
        String inertiaGroundType = CyvClientConfig.getString("inertiaGroundType", "normal");
        double inertiaMin = CyvClientConfig.getDouble("inertiaMin", -0.02);
        double inertiaMax = CyvClientConfig.getDouble("inertiaMax", 0.02);

        //check inertia
        if (airtime == inertiaTick) {
            if (inertiaAxis == 'x') stored_v=vx; else stored_v=vz;
            if (airtime > 1) stored_slip = 1f;
            else if (inertiaGroundType.equals("ice")) stored_slip = 0.98f;
            else if (inertiaGroundType.equals("slime")) stored_slip = 0.8f;
            else stored_slip = 0.6f;

        } else if (airtime == inertiaTick + 1 && !inertiaCheckedThisJump) {

            int tick = inertiaTick;
            double min = inertiaMin;
            double max = inertiaMax;

            int d = Integer.valueOf(CyvForge.config.configFields.get("df").value.toString());
            DecimalFormat df = new DecimalFormat("#");
            df.setMaximumFractionDigits(d);

            if ((stored_v>=min && stored_v<=max) || (stored_v<=min && stored_v>=max)) {

                if (Math.abs(stored_v)*0.91F*stored_slip < 0.005) {
                    CyvForge.sendChatMessage("Hit inertia at tick " + (airtime-1) + ", previous v = " + df.format(stored_v));
                } else {
                    CyvForge.sendChatMessage("Missed inertia at tick " + (airtime-1) + ", previous v = " + df.format(stored_v));
                }

                inertiaCheckedThisJump = true;
            }

        }//end checking inertia
    }

    private void checkPosition() {
        boolean toggled = CyvClientConfig.getBoolean("positionCheckerEnabled", false);
        int checkTick = CyvClientConfig.getInt("positionCheckerTick", 7);
        boolean zNeo = CyvClientConfig.getBoolean("positionCheckerZNeo", false);

        double xMin = CyvClientConfig.getDouble("positionCheckerMinX", -10000.0);
        double xMax = CyvClientConfig.getDouble("positionCheckerMaxX", 10000.0);
        double zMin = CyvClientConfig.getDouble("positionCheckerMinZ", -10000.0);
        double zMax = CyvClientConfig.getDouble("positionCheckerMaxZ", 10000.0);

        if (!toggled) return;
        if (airtime == checkTick && !zNeo && !positionCheckedThisJump) {
            if (!((xMin <= x && x <= xMax) || (xMax <= x && x <= xMin))) return;
            if (!((zMin <= z && z <= zMax) || (zMax <= z && z <= zMin))) return;

            DecimalFormat df = CyvForge.df;

            CyvForge.sendChatMessage("X: " + df.format(x) + ", Z: " + df.format(z));

            positionCheckedThisJump = true;

        } else if (airtime == checkTick && zNeo && !positionCheckedThisJump) {
            if (!((xMin <= x && x <= xMax) || (xMax <= x && x <= xMin))) return;
            if (!((zMin <= lastTick.z && lastTick.z <= zMax) || (zMax <= lastTick.z && lastTick.z <= zMin))) return;

            DecimalFormat df = CyvForge.df;

            CyvForge.sendChatMessage("X: " + df.format(x) + ", Z: " + df.format(lastTick.z));

            positionCheckedThisJump = true;
        }

    }

    private static void calculateLastTiming() {
        boolean showMS = CyvClientConfig.getBoolean("showMilliseconds", true);
        GameSettings gameSettings = Minecraft.getMinecraft().gameSettings;

        if (gameSettings.keyBindForward.isKeyDown() || //ANYTHING IS PRESSED
                gameSettings.keyBindBack.isKeyDown() ||
                gameSettings.keyBindLeft.isKeyDown() ||
                gameSettings.keyBindRight.isKeyDown()) {
            lastMoveTime++;
            lastGroundMoveTime++;
            hasActed = true;

            //already jumped, started moving
            if (lastJumpTime > -1 && lastMoveTime == 0 && airtime != 0 && !(vy == 0 && lastTick.onGround)
                    && (lastTiming.contains("Pessi") || !locked)) {
                if ((lastJumpTime+1) == 1) lastTiming = "Max Pessi";
                else lastTiming = "Pessi " + (lastJumpTime+1) + " ticks";
                long diff = Math.abs(moveTimestamp - jumpTimestamp) / 1000000;

                if (showMS && diff < 500) {
                    lastTiming += " (" + diff + "ms)";
                }
                locked = true;
            }

            if (lastTick.onGround && !secondLastTick.onGround) { //landed
                lastGroundMoveTime = 0;
            }

        } else { //nothing is pressed
            lastMoveTime = -1;
            lastGroundMoveTime = -1;
        }

        //jumping
        if (gameSettings.keyBindJump.isKeyDown() && airtime == 0) {
            lastJumpTime = 0;
            hasActed = true;

            //already jumped, started moving
            if ((lastGroundMoveTime == 0 || lastMoveTime == 0) && !locked) {
                lastTiming = "Jam";

                long diff = Math.abs(jumpTimestamp - moveTimestamp) / 1000000;

                if (showMS && diff < 1000) {
                    lastTiming += " (" + diff + "ms)";
                }

                if (gameSettings.keyBindSprint.isKeyDown() || !gameSettings.keyBindForward.isKeyDown()) {
                    locked = true;
                }
            }
            else if (lastGroundMoveTime > -1 && !locked && lastJumpTime == 0) {
                if (lastSneakTime == -1) lastTiming = "Burst " + (lastGroundMoveTime - 1) + " ticks";
                else if (lastSneakTime > -1) lastTiming = "Burstjam " + (lastGroundMoveTime) + " ticks";
                else lastTiming = "HH " + (lastGroundMoveTime) + " ticks";

                long diff = (jumpTimestamp - moveTimestamp) / 1000000;
                if (showMS && Math.abs(diff) < 1000) {
                    lastTiming += " (" + diff + "ms)";
                }
                locked = true;
            }

            //midair after jumping
        } else if (!lastTick.onGround && lastJumpTime > -1) {
            lastJumpTime++;
            //not midair not jumping
        } else {
            lastJumpTime = -1;
        }

        //sneaking
        if (gameSettings.keyBindSneak.isKeyDown()) {
            if (lastSneakTime == -2) lastSneakTime = 0;
            else lastSneakTime++;
        }
        else {
            if (lastSneakTime == -1 || lastSneakTime == -2) lastSneakTime = -2;
            else lastSneakTime = -1;
        }

        if ((gameSettings.keyBindSprint.isKeyDown() || lastSprintTime != -1)
                && !lastTick.onGround ) {
            lastSprintTime++;
            if (lastTiming.startsWith("Jam") && lastSprintTime == 0 && !locked && lastTick.keys[0]) {
                if (lastJumpTime < 1) {
                } else {
                    if (lastJumpTime == 1) lastTiming = "Max FMM";
                    else lastTiming = "FMM " + (lastJumpTime) + " ticks";

                    long diff = (sprintTimestamp - jumpTimestamp) / 1000000;
                    if (showMS && diff >= 0 && diff < 1000) lastTiming += " (" + diff + "ms)";

                    locked = true;
                }
            }

        } else {
            lastSprintTime = -1;
        }

        //mark
        if (airtime > 0) {
            if (gameSettings.keyBindForward.isKeyDown() && !lastTick.keys[0] && (lastTick.keys[1] || lastTick.keys[3])) {
                long diff = (moveTimestamp - jumpTimestamp) / 1000000;
                String msTxt = (showMS && diff >= 0 && diff < 1000) ? " (" + diff + "ms)" : "";

                if (CyvClientConfig.getString("markDisplay", "Sidestep").equals("Sidestep")) {
                    sidestep = 2;
                    sidestepTime = airtime;
                } else {
                    lastTiming = "Mark " + airtime + "t" + msTxt;
                }
            }
        }

        //strafejam
        if (CyvClientConfig.getBoolean("detectStrafejam", false) && airtime > 0) {
            if (gameSettings.keyBindForward.isKeyDown() && !(gameSettings.keyBindLeft.isKeyDown() || gameSettings.keyBindRight.isKeyDown())
                    && lastTick.keys[0] && (lastTick.keys[1] || lastTick.keys[3])) {

                long diff = (releaseTimestamp - jumpTimestamp) / 1000000;
                String msTxt = (showMS && diff >= 0 && diff < 1000) ? " (" + diff + "ms)" : "";
                String sjSuffix = "Strafejam " + (airtime) + "t" + msTxt;

                if (!CyvClientConfig.getBoolean("strafejamJamOnly", true)) {
                    if (lastTiming.isEmpty() || lastTiming.equals("-")) {
                        lastTiming = sjSuffix;
                    } else if (!lastTiming.contains("Strafejam")) {
                        lastTiming = lastTiming + " | " + sjSuffix;
                        locked = true;
                    }
                } else if (lastTiming.startsWith("Jam") || lastTiming.startsWith("Mark")) {
                    lastTiming = sjSuffix;
                    locked = true;
                }
            }
        }

        //wobble
        if (CyvClientConfig.getBoolean("detectWobble", false)) {
            if (secondLastTick != null && lastTick != null && secondLastTick.keys != null && lastTick.keys != null) {
                String wobbledKey = "";

                // 0=W, 1=A, 2=S, 3=D
                if (gameSettings.keyBindForward.isKeyDown() && !lastTick.keys[0] && secondLastTick.keys[0]) wobbledKey += "W";
                if (gameSettings.keyBindLeft.isKeyDown() && !lastTick.keys[1] && secondLastTick.keys[1]) wobbledKey += "A";
                if (gameSettings.keyBindBack.isKeyDown() && !lastTick.keys[2] && secondLastTick.keys[2]) wobbledKey += "S";
                if (gameSettings.keyBindRight.isKeyDown() && !lastTick.keys[3] && secondLastTick.keys[3]) wobbledKey += "D";

                if (!wobbledKey.isEmpty()) {
                    long diff = (moveTimestamp - releaseTimestamp) / 1000000;
                    String msTxt = (showMS && diff >= 0 && diff < 1000) ? " " + diff + "ms" : "";
                    lastTiming = "Wobble (" + wobbledKey + msTxt + ")";
                    // locked = true;
                }
            }
        }

        //reset
        if (!(gameSettings.keyBindForward.isKeyDown() || //ANYTHING IS PRESSED
                gameSettings.keyBindBack.isKeyDown() ||
                gameSettings.keyBindLeft.isKeyDown() ||
                gameSettings.keyBindRight.isKeyDown()) &&
                Minecraft.getMinecraft().thePlayer.onGround) {
            resetLastTiming();
        }

        //sidestep
        if (gameSettings.keyBindJump.isKeyDown() && airtime == 0) {
            if (((lastTick.strafe() != 0) && Minecraft.getMinecraft().thePlayer.movementInput.moveStrafe == 0)) {
                sidestepTime = 1;
                sidestep = 0;
            } else if (Minecraft.getMinecraft().thePlayer.movementInput.moveStrafe != 0) {
                sidestepTime = 1;
                sidestep = 1;
            } else {
                sidestep = -1;
                sidestepTime = 0;
            }

        } else if (airtime > 0) {
            if (sidestep == -1 && Minecraft.getMinecraft().thePlayer.movementInput.moveStrafe != 0) {
                sidestep = 0;
                sidestepTime = airtime;
            }

            if (sidestepTime == airtime && Minecraft.getMinecraft().thePlayer.movementInput.moveStrafe == 0.0f
                    && sidestep == 0) {
                sidestepTime++;
            }

        }

        //overflow prevention
        if (lastJumpTime > 999) lastJumpTime = 999;
        if (lastGroundMoveTime > 999) lastGroundMoveTime = 999;
        if (lastMoveTime > 999) lastMoveTime = 999;
        if (lastSprintTime > 999) lastSprintTime = 999;
    }

    public static void resetLastTiming() {
        locked = false;
        hasActed = false;
        grindStarted = false;
        hasCollided = false;
    }

    public static class PosTick {

        public PosTick(EntityPlayerSP player, double vx, double vy, double vz, int airtime, boolean[] keys) {

            this.x = player.posX;
            this.y = player.posY;
            this.z = player.posZ;
            this.f = player.rotationYaw;
            this.p = player.rotationPitch;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.onGround = player.onGround;
            this.airtime = airtime;
            this.keys = keys;
        }

        public PosTick(double x, double y, double z, int airtime, boolean[] keys) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.f = 0;
            this.p = 0;
            this.vx = 0;
            this.vy = 0;
            this.vz = 0;
            this.onGround = true;
            this.airtime = airtime;
            this.keys = keys;
        }

        public PosTick(double x, double y, double z, float yaw, float pitch, double motionX, double motionY,
                       double motionZ, boolean onGround, int airtime, boolean[] keys) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.f = yaw;
            this.p = pitch;
            this.vx = motionX;
            this.vy = motionY;
            this.vz = motionZ;
            this.onGround = onGround;
            this.airtime = airtime;
            this.keys = keys;
        }


        public boolean[] keys;
        public double x;
        public double y;
        public double z;

        public float f;
        public float p;

        public double vx;
        public double vy;
        public double vz;

        public double true_vx, true_vy, true_vz;

        public boolean onGround;
        public int airtime;

        boolean hasCollidedHorizontally;

        int strafe() {
            int i = 0;
            if (keys[1] == true) i--;
            if (keys[3] == true) i++;
            return i;
        }

        int forward() {
            int i = 0;
            if (keys[0] == true) i++;
            if (keys[3] == true) i--;
            return i;
        }

    }

    public static float formatYaw(float yaw) {
        float facing = yaw % 360;
        if (facing > 180) facing -= 360;
        else if (facing < -180) facing += 360;
        return facing;
    }

    private void doCheckpoints() {
        if (!CyvClientConfig.getBoolean("singleplayerCheckpointsEnabled", true)) return;
        if (MinecraftServer.getServer() == null || !MinecraftServer.getServer().isSinglePlayer()) return;
        ItemStack item = Minecraft.getMinecraft().thePlayer.getHeldItem();

        try {
            if (item.getTagCompound().getBoolean("isCP")) {
                //set fall dmg to 0
                EntityPlayerMP player = CommandBase.getPlayer(Minecraft.getMinecraft().thePlayer, Minecraft.getMinecraft().thePlayer.getName());
                player.fallDistance = 0;
                player.getFoodStats().setFoodLevel(20);
                player.setAir(300);

                player.addPotionEffect(new PotionEffect(12, 8, 0, true, false)); //fire resistance
                player.addPotionEffect(new PotionEffect(11, 8, 4, true, false)); //resistance

                WorldInfo worldinfo = MinecraftServer.getServer().worldServers[0].getWorldInfo();
                worldinfo.setRainTime(0); //weather shit
                worldinfo.setThunderTime(0);
                worldinfo.setRaining(false);
                worldinfo.setThundering(false);

                if (Minecraft.getMinecraft().gameSettings.keyBindUseItem.isKeyDown()) {
                    item.getTagCompound().getBoolean("isCP");
                    double x = item.getTagCompound().getDouble("coordX");
                    double y = item.getTagCompound().getDouble("coordY");
                    double z = item.getTagCompound().getDouble("coordZ");
                    float yaw = item.getTagCompound().getFloat("coordYaw");
                    float pitch = item.getTagCompound().getFloat("coordPitch");

                    String[] coords = new String[] {Minecraft.getMinecraft().thePlayer.getName(), String.valueOf(x), String.valueOf(y), String.valueOf(z), String.valueOf(yaw), String.valueOf(pitch)};
                    CheckpointTeleport.tp(Minecraft.getMinecraft().thePlayer, coords);
                    Minecraft.getMinecraft().thePlayer.setVelocity(0, 0, 0);

                }
            }

        } catch (Exception ignored) {}

    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent e) {
        Entity p = Minecraft.getMinecraft().getRenderViewEntity();
        double pX = p.lastTickPosX + (e.partialTicks * (p.posX - p.lastTickPosX));
        double pY = p.lastTickPosY + (e.partialTicks * (p.posY - p.lastTickPosY));
        double pZ = p.lastTickPosZ + (e.partialTicks * (p.posZ - p.lastTickPosZ));

        GL11.glLineWidth(2);
        GlStateManager.disableDepth();

        if (CyvClientConfig.getBoolean("highlightLandingCond", false)) {
            if (landingBlock != null) {
                AxisAlignedBB bb = new AxisAlignedBB(landingBlock.xMinCond - pX + 0.3, landingBlock.smallestY() - pY,
                        landingBlock.zMinCond - pZ + 0.3, landingBlock.xMaxCond - pX - 0.3, landingBlock.largestY() - pY,
                        landingBlock.zMaxCond - pZ - 0.3);

                RenderUtils.drawFilledBox(bb.expand(0.001, 0.001, 0.001), 0, 192, 255, 25);
                RenderUtils.drawBoxOutline(bb.expand(0.001, 0.001, 0.001), 0, 192, 255, 75);
            }

            if (momentumBlock != null) {
                AxisAlignedBB bb = new AxisAlignedBB(momentumBlock.xMinCond - pX + 0.3, momentumBlock.smallestY() - pY,
                        momentumBlock.zMinCond - pZ + 0.3, momentumBlock.xMaxCond - pX - 0.3, momentumBlock.largestY() - pY,
                        momentumBlock.zMaxCond - pZ - 0.3);

                RenderUtils.drawFilledBox(bb.expand(0.001, 0.001, 0.001), 255, 0, 0, 25);
                RenderUtils.drawBoxOutline(bb.expand(0.001, 0.001, 0.001), 255, 0, 0, 75);
            }
        }

        if (CyvClientConfig.getBoolean("highlightLanding", false)) {
            if (landingBlock != null) {
                for (AxisAlignedBB bb : landingBlock.bb) {
                    AxisAlignedBB renderBB = bb.offset(-pX, -pY, -pZ);
                    RenderUtils.drawFilledBox(renderBB.expand(0.001, 0.001, 0.001), 0, 192, 255, 100);
                    RenderUtils.drawBoxOutline(renderBB.expand(0.001, 0.001, 0.001), 0, 192, 255, 200);
                }
            }

            if (momentumBlock != null) {
                for (AxisAlignedBB bb : momentumBlock.bb) {
                    AxisAlignedBB renderBB = bb.offset(-pX, -pY, -pZ);
                    RenderUtils.drawFilledBox(renderBB.expand(0.001, 0.001, 0.001), 255, 0, 0, 100);
                    RenderUtils.drawBoxOutline(renderBB.expand(0.001, 0.001, 0.001), 255, 0, 0, 200);
                }
            }
        }

        GlStateManager.enableDepth();

    }

    public static void triggerAntiCP() {
        if (CyvClientConfig.getBoolean("antiCP", false)) {
            lastPbTimestamp = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (event.button == 1 && event.buttonstate) {

            positionCheckedThisJump = false;
            pendingWaterCheck = false;
            inertiaCheckedThisJump = false;

            waterDetectionLockout = 10;

            stopCounter = 0;
            waitCounter = 0;
            runCounter = 0;
            lastStopTime = 0;
            lastWaitTime = 0;
            lastRunTime = 0;
            hasInputtedSinceReset = false;

            if (CyvClientConfig.getBoolean("antiCP", false)) {

                long delayMs = CyvClientConfig.getInt("antiCPDelay", 1) * 1000L;
                long timePassed = System.currentTimeMillis() - lastPbTimestamp;

                if (timePassed < delayMs) {
                    event.setCanceled(true);

                    CyvForge.sendChatMessage("§cYou have AntiCP enabled, right-click cancelled.");
                }
            }
        }
    }
}
