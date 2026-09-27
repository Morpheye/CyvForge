package net.cyvforge.event.events;

import net.cyvforge.config.CyvClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;
import java.util.ArrayList;

public class InputHistoryManager {
    public static ArrayList<HistorySection> history = new ArrayList<>();

    private boolean lastW, lastA, lastS, lastD, lastJmp, lastSpr, lastSnk;
    private float lastYaw;
    private boolean lastOnGround;

    private boolean skipUntilGround = false;

    public static int absoluteTick = 0;

    public static class HistorySection {
        public int ticks = 1;
        public int startTick;
        public int endTick;
        public int age = 0;
        public boolean w, a, s, d, jmp, spr, snk, isAir;
        public float yaw;
        public float turnAmount;

        public HistorySection(boolean w, boolean a, boolean s, boolean d, boolean jmp, boolean spr, boolean snk, boolean isAir, float yaw, float turnAmount) {
            this.w = w; this.a = a; this.s = s; this.d = d;
            this.jmp = jmp; this.spr = spr; this.snk = snk;
            this.isAir = isAir; this.yaw = yaw; this.turnAmount = turnAmount;

            this.startTick = absoluteTick + 1;
            this.endTick = absoluteTick + 1;
        }

        public String getInputsString() {
            StringBuilder sb = new StringBuilder();
            if (w) sb.append("W ");
            if (a) sb.append("A ");
            if (s) sb.append("S ");
            if (d) sb.append("D ");
            if (jmp) sb.append("Jmp ");
            if (spr) sb.append("Spr ");
            if (snk) sb.append("Snk ");
            return sb.toString().trim();
        }

        public boolean isSameInput(boolean w2, boolean s2, boolean a2, boolean d2, boolean jmp2, boolean spr2, boolean snk2, boolean air2) {
            return w == w2 && s == s2 && a == a2 && d == d2 && jmp == jmp2 && spr == spr2 && snk == snk2 && isAir == air2;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        // Reset
        if (CyvClientConfig.getBoolean("ih_resetRMB", true) && Mouse.isButtonDown(1)) {
            history.clear();
            absoluteTick = 0;
            skipUntilGround = true;
            return;
        }

        GameSettings gs = mc.gameSettings;
        boolean w = gs.keyBindForward.isKeyDown();
        boolean a = gs.keyBindLeft.isKeyDown();
        boolean s = gs.keyBindBack.isKeyDown();
        boolean d = gs.keyBindRight.isKeyDown();
        boolean jmp = gs.keyBindJump.isKeyDown();
        boolean spr = gs.keyBindSprint.isKeyDown();
        boolean snk = gs.keyBindSneak.isKeyDown();

        float currentYaw = mc.thePlayer.rotationYaw;
        boolean onGround = mc.thePlayer.onGround;

        if (skipUntilGround) {
            if (!onGround) {
                lastW = w; lastA = a; lastS = s; lastD = d; lastSnk = snk;
                lastYaw = currentYaw; lastOnGround = onGround;
                lastSpr = false; lastJmp = false;
                return;
            } else {
                skipUntilGround = false;
                lastJmp = false; lastSpr = false;
                lastOnGround = true;
            }
        }

        boolean hasActionInput = w || a || s || d || jmp || snk;
        if (CyvClientConfig.getBoolean("ih_ignoreSprUntilInput", false) && history.isEmpty()) {
            if (!hasActionInput) {
                lastW = w; lastA = a; lastS = s; lastD = d; lastSnk = snk;
                lastYaw = currentYaw; lastOnGround = onGround;
                lastSpr = false; lastJmp = false;
                return;
            }
        }

        boolean inputRelease = CyvClientConfig.getBoolean("ih_inputRelease", true);
        boolean groundJumpReset = CyvClientConfig.getBoolean("ih_groundJumpReset", true);
        boolean treatLandingAsAir = CyvClientConfig.getBoolean("ih_treatLandingAsAir", true);
        boolean jumpOnlyNoBreak = CyvClientConfig.getBoolean("ih_jumpNoBreak", false);
        boolean noYawBreak = CyvClientConfig.getBoolean("ih_noYawBreak", false);
        boolean justJumped = groundJumpReset && !onGround && lastOnGround;
        boolean justLanded = groundJumpReset && onGround && !lastOnGround;

        boolean effectiveAirState = !onGround;
        if (groundJumpReset && treatLandingAsAir && justLanded) {
            effectiveAirState = true;
        }

        boolean effJmp = inputRelease ? (jmp && (!lastJmp || justJumped)) : jmp;
        boolean effSpr = inputRelease ? (spr && !lastSpr) : spr;

        boolean trackFacing = CyvClientConfig.getBoolean("ih_trackFacing", false);

        boolean isNewSection = false;

        if (history.isEmpty()) {
            isNewSection = true;
        } else {
            HistorySection lastSec = history.get(history.size() - 1);

            boolean movementChanged = (lastSec.w != w || lastSec.a != a || lastSec.s != s || lastSec.d != d || lastSec.snk != snk);

            boolean stateChanged = (lastSec.isAir != effectiveAirState);

            boolean forceBreak = justJumped || (justLanded && !treatLandingAsAir);

            boolean yawChanged = trackFacing && Math.abs(currentYaw - lastYaw) > 0.001;

            if (movementChanged || stateChanged || forceBreak || (yawChanged && !noYawBreak)) {
                isNewSection = true;
            } else {
                boolean sprintPressed = spr && !lastSpr;

                if (sprintPressed) {
                    isNewSection = true;
                } else if (!jumpOnlyNoBreak) {
                    if (lastSec.jmp != effJmp || lastSec.spr != effSpr) {
                        isNewSection = true;
                    }
                }
            }
        }

        if (isNewSection) {
            boolean landingStart = !history.isEmpty() && history.get(history.size()-1).isAir && !effectiveAirState;
            if (justJumped || landingStart) {
                absoluteTick = 0;
            }

            HistorySection newSec = new HistorySection(w, a, s, d, effJmp, effSpr, snk, effectiveAirState, currentYaw, currentYaw - lastYaw);
            history.add(newSec);

            int maxLength = CyvClientConfig.getInt("ih_historyLength", 20);
            while (history.size() > maxLength) history.remove(0);
        } else {
            HistorySection last = history.get(history.size() - 1);
            last.ticks++;
            last.endTick = absoluteTick + 1;

            if (noYawBreak) {
                last.turnAmount = currentYaw - lastYaw;
                last.yaw = currentYaw;
            }

            if (jumpOnlyNoBreak) {
                if (effJmp) last.jmp = true;
                if (effSpr) last.spr = true;
            }
        }

        absoluteTick++;

        boolean alwaysVisible = CyvClientConfig.getBoolean("ih_alwaysVisible", true);
        int visLength = CyvClientConfig.getInt("ih_visLength", 60);

        for (int i = history.size() - 1; i >= 0; i--) {
            history.get(i).age++;
            if (!alwaysVisible && history.get(i).age > visLength) {
                history.remove(i);
            }
        }
        updateLastState(w, a, s, d, jmp, spr, snk, currentYaw, onGround);
    }

    private void updateLastState(boolean w, boolean a, boolean s, boolean d, boolean jmp, boolean spr, boolean snk, float yaw, boolean og) {
        lastW = w; lastA = a; lastS = s; lastD = d;
        lastJmp = jmp; lastSpr = spr; lastSnk = snk;
        lastYaw = yaw; lastOnGround = og;
    }
}