package net.cyvforge.util.parkour;

import net.cyvforge.CyvForge;
import net.cyvforge.config.CyvClientConfig;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class LandingBlock {

    public BlockPos pos;
    public AxisAlignedBB[] bb; //bounding boxes
    public LandingMode mode;
    public LandingAxis axis;
    public boolean isBox;
    public boolean neoAndNormal;
    public boolean ignoreWallHit = false;
    public boolean isLiquid = false;
    public double liquidBottomOffset = 0;
    public int targetTick = -1;
    public double lowBlockHeight = 0;
    private String supportName = "";

    public Double pb;
    public Double pbX;
    public Double pbZ;

    public Double lastOffsetX;
    public Double lastOffsetZ;
    public Double lastPb;

    public Double[] xMinWalls, xMaxWalls, zMinWalls, zMaxWalls;

    public double xMinCond, xMaxCond, zMinCond, zMaxCond; //detection boxes (for offsets)

    public LandingBlock(BlockPos pos, LandingMode mode, LandingAxis axis, boolean isBox) {
        this.pos = pos;
        this.mode = mode;
        this.axis = axis;
        this.isBox = isBox;
        this.ignoreWallHit = CyvClientConfig.getBoolean("ignoreWallHit", false);

        resolveBoxSupport();
        calculateBounds(); //calculate the hitbox
        calculateWalls();

        //create detection box
        this.xMinCond = this.smallestX() - 1;
        this.xMaxCond = this.largestX() + 1;
        this.zMinCond = this.smallestZ() - 1;
        this.zMaxCond = this.largestZ() + 1;
    }

    public LandingBlock(AxisAlignedBB bounds) {
        this.pos = new BlockPos(bounds.minX, bounds.minY, bounds.minZ);
        this.mode = LandingMode.landing;
        this.axis = LandingAxis.both;
        this.isBox = false;
        this.ignoreWallHit = CyvClientConfig.getBoolean("ignoreWallHit", false);

        this.bb = new AxisAlignedBB[] {bounds};

        this.xMinCond = this.smallestX() - 1;
        this.xMaxCond = this.largestX() + 1;
        this.zMinCond = this.smallestZ() - 1;
        this.zMaxCond = this.largestZ() + 1;
    }

    private void calculateBounds() {
        if (this.pos == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.theWorld;

        IBlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();

        if (block instanceof net.minecraft.block.BlockLiquid) {
            this.isLiquid = true;
            double shrink = 0.0;
            double bottomOffset = 0.0;

            if (block.getMaterial() == net.minecraft.block.material.Material.water) { //water
                shrink = 0.001;
                bottomOffset = 0.399;
            }
            else if (block.getMaterial() == net.minecraft.block.material.Material.lava) { //lava
                shrink = 0.1;
                bottomOffset = 0.4;
            }

            this.liquidBottomOffset = bottomOffset;

            this.bb = new AxisAlignedBB[] {
                    new AxisAlignedBB(
                            pos.getX() + shrink, pos.getY() - bottomOffset, pos.getZ() + shrink,
                            pos.getX() + 1 - shrink, pos.getY() + 1, pos.getZ() + 1 - shrink
                    )
            };
            return;
        }

        //THIS IS TEMPORARY. I will find a better solution in the future
        if (isBox && (block instanceof BlockLadder || block instanceof BlockVine || block instanceof BlockSlime || block instanceof BlockIce ||
                block instanceof net.minecraft.block.BlockPackedIce)) {
            AxisAlignedBB playerHitbox = mc.thePlayer.getEntityBoundingBox();

            double playerX = playerHitbox.maxX - playerHitbox.minX;
            double playerZ = playerHitbox.maxZ - playerHitbox.minZ;
            AxisAlignedBB box = new AxisAlignedBB(playerX/2 + pos.getX(), pos.getY() - 0.02, playerZ/2 + pos.getZ(),
                    1-playerX/2 + pos.getX(), 1 + pos.getY() + lowBlockHeight, 1-playerZ/2 + pos.getZ());
            this.bb = new AxisAlignedBB[] {box};

            return;
        }

        this.bb = CyvForge.getHitbox(pos, world).toArray(new AxisAlignedBB[0]);

    }

    private void resolveBoxSupport() {
        lowBlockHeight = 0;
        if (!isBox || mode == LandingMode.enter || pos == null) return;

        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return;

        BlockPos supportPos;
        if (isBoxSupport(world.getBlockState(pos).getBlock())) supportPos = pos; //the slime/ice itself
        else if (isBoxSupport(world.getBlockState(pos.down()).getBlock())) supportPos = pos.down(); //the thin block lying on it
        else return;

        double height = thinBlockHeight(world, supportPos);
        if (height <= 0) return;

        pos = supportPos;
        lowBlockHeight = height;
        supportName = supportName(world.getBlockState(supportPos).getBlock());
    }

    private static String supportName(Block block) {
        if (block instanceof BlockSlime) return "slime";
        if (block instanceof BlockPackedIce) return "packed ice";
        return "ice";
    }

    private static boolean isBoxSupport(Block block) {
        return block instanceof BlockSlime || block instanceof BlockIce || block instanceof BlockPackedIce;
    }

    private static double thinBlockHeight(World world, BlockPos supportPos) {
        BlockPos abovePos = supportPos.up();
        List<AxisAlignedBB> boxes = CyvForge.getHitbox(abovePos, world);
        if (boxes == null || boxes.isEmpty()) return 0;

        double base = supportPos.getY() + 1; //top of the slime/ice
        double top = base;
        for (AxisAlignedBB b : boxes) top = Math.max(top, b.maxY);

        double height = top - base;
        if (height <= 0 || height >= 1.0) return 0;

        double eps = 1.0E-4;
        for (AxisAlignedBB b : boxes) { //the highest box has to span the whole block
            if (b.maxY >= top - eps
                    && b.minX <= abovePos.getX() + eps && b.maxX >= abovePos.getX() + 1 - eps
                    && b.minZ <= abovePos.getZ() + eps && b.maxZ >= abovePos.getZ() + 1 - eps) return height;
        }
        return 0;
    }

    public boolean announceLowBlock(String blockName) {
        if (lowBlockHeight <= 0) return false;

        CyvForge.sendChatMessage("Successfully set a " + blockName + " on the " + supportName + " below");
        return true;
    }

    public double smallestX() {
        Double v = null;
        for (AxisAlignedBB bb : this.bb) {
            if (v == null || v > bb.minX) v = bb.minX;
        }
        return v;
    }

    public double smallestY() {
        Double v = null;
        for (AxisAlignedBB bb : this.bb) {
            if (v == null || v > bb.minY) v = bb.minY;
        }
        return v;
    }

    public double smallestZ() {
        Double v = null;
        for (AxisAlignedBB bb : this.bb) {
            if (v == null || v > bb.minZ) v = bb.minZ;
        }
        return v;
    }

    public double largestX() {
        Double v = null;
        for (AxisAlignedBB bb : this.bb) {
            if (v == null || v < bb.maxX) v = bb.maxX;
        }
        return v;
    }

    public double largestY() {
        Double v = null;
        for (AxisAlignedBB bb : this.bb) {
            if (v == null || v < bb.maxY) v = bb.maxY;
        }
        return v;
    }

    public double largestZ() {
        Double v = null;
        for (AxisAlignedBB bb : this.bb) {
            if (v == null || v < bb.maxZ) v = bb.maxZ;
        }
        return v;
    }

    public void adjustCond(double x1, double x2, double z1, double z2) {
        if (x1 > x2) {
            this.xMaxCond = x1;
            this.xMinCond = x2;

        } else {
            this.xMaxCond = x2;
            this.xMinCond = x1;

        }

        if (z1 > z2) {
            this.zMaxCond = z1;
            this.zMinCond = z2;
        } else {
            this.zMaxCond = z2;
            this.zMinCond = z1;
        }

    }

    public Double xMinWall(int i) { return wallAt(xMinWalls, i); }
    public Double xMaxWall(int i) { return wallAt(xMaxWalls, i); }
    public Double zMinWall(int i) { return wallAt(zMinWalls, i); }
    public Double zMaxWall(int i) { return wallAt(zMaxWalls, i); }

    private static Double wallAt(Double[] walls, int i) {
        return (walls != null && i >= 0 && i < walls.length) ? walls[i] : null;
    }

    public void calculateWalls() {
        World world = Minecraft.getMinecraft().theWorld;
        if (world == null || bb == null) return;
        AxisAlignedBB playerHitbox = Minecraft.getMinecraft().thePlayer.getEntityBoundingBox();
        BlockPos tempPos = pos;
        if (this.mode == LandingMode.enter) tempPos = tempPos.down();

        double playerHeight = playerHitbox.maxY - playerHitbox.minY;
        double playerWidthX = playerHitbox.maxX - playerHitbox.minX;
        double playerWidthZ = playerHitbox.maxZ - playerHitbox.minZ;

        Double[] newXMinWalls = new Double[bb.length];
        Double[] newXMaxWalls = new Double[bb.length];
        Double[] newZMinWalls = new Double[bb.length];
        Double[] newZMaxWalls = new Double[bb.length];

        for (int i = 0; i < bb.length; i++) {
            AxisAlignedBB box = bb[i];
            Double xMin = null, xMax = null, zMin = null, zMax = null; //walls of THIS box only
            ArrayList<AxisAlignedBB> wallBoxes = new ArrayList<AxisAlignedBB>();
            BlockPos currentWallPos = null;
            double offset, currentWall;

            // Z BACK (North: Z-)
            wallBoxes.clear();
            currentWallPos = tempPos.north();
            for (int dy = 0; dy <= 2; dy++) {
                List<AxisAlignedBB> h = CyvForge.getHitbox(currentWallPos.up(dy), world);
                if (h != null) wallBoxes.addAll(h);
            }

            for (int dy = 1; dy <= 2; dy++) {
                List<AxisAlignedBB> inBlock = CyvForge.getHitbox(tempPos.up(dy), world);
                if (inBlock != null) {
                    for (AxisAlignedBB bIn : inBlock) {
                        if (bIn.minZ <= box.minZ + 0.01 && bIn.maxZ < box.maxZ - 0.5) wallBoxes.add(bIn);
                    }
                }
            }

            for (AxisAlignedBB wall : wallBoxes) {
                if (wall.minX > box.minX + 0.01 || wall.maxX < box.maxX - 0.01) continue;
                if (wall.minX < box.minX - 0.1 && wall.maxX > box.maxX + 0.1 && (wall.maxZ - wall.minZ) < 0.5) continue;
                if (wall.maxY <= box.maxY) continue;
                if (wall.minY >= box.maxY + playerHeight) continue;

                currentWall = wall.maxZ;
                offset = box.minZ - currentWall - playerWidthZ;

                if (offset < 0) {
                    if (zMin == null || currentWall > zMin) zMin = currentWall;
                }
            }

            // Z FRONT (South: Z+)
            wallBoxes.clear();
            currentWallPos = tempPos.south();
            for (int dy = 0; dy <= 2; dy++) {
                List<AxisAlignedBB> h = CyvForge.getHitbox(currentWallPos.up(dy), world);
                if (h != null) wallBoxes.addAll(h);
            }

            for (int dy = 1; dy <= 2; dy++) {
                List<AxisAlignedBB> inBlock = CyvForge.getHitbox(tempPos.up(dy), world);
                if (inBlock != null) {
                    for (AxisAlignedBB bIn : inBlock) {
                        if (bIn.maxZ >= box.maxZ - 0.01 && bIn.minZ > box.minZ + 0.5) wallBoxes.add(bIn);
                    }
                }
            }

            for (AxisAlignedBB wall : wallBoxes) {
                if (wall.minX > box.minX + 0.01 || wall.maxX < box.maxX - 0.01) continue;
                if (wall.minX < box.minX - 0.1 && wall.maxX > box.maxX + 0.1 && (wall.maxZ - wall.minZ) < 0.5) continue;
                if (wall.maxY <= box.maxY) continue;
                if (wall.minY >= box.maxY + playerHeight) continue;

                currentWall = wall.minZ;
                offset = currentWall - box.maxZ - playerWidthZ;

                if (offset < 0) {
                    if (zMax == null || currentWall < zMax) zMax = currentWall;
                }
            }

            // X RIGHT (West: X-)
            wallBoxes.clear();
            currentWallPos = tempPos.west();
            for (int dy = 0; dy <= 2; dy++) {
                List<AxisAlignedBB> h = CyvForge.getHitbox(currentWallPos.up(dy), world);
                if (h != null) wallBoxes.addAll(h);
            }

            for (int dy = 1; dy <= 2; dy++) {
                List<AxisAlignedBB> inBlock = CyvForge.getHitbox(tempPos.up(dy), world);
                if (inBlock != null) {
                    for (AxisAlignedBB bIn : inBlock) {
                        if (bIn.minX <= box.minX + 0.01 && bIn.maxX < box.maxX - 0.5) wallBoxes.add(bIn);
                    }
                }
            }

            for (AxisAlignedBB wall : wallBoxes) {
                if (wall.minZ > box.minZ + 0.01 || wall.maxZ < box.maxZ - 0.01) continue;
                if (wall.minZ < box.minZ - 0.1 && wall.maxZ > box.maxZ + 0.1 && (wall.maxX - wall.minX) < 0.5) continue;
                if (wall.maxY <= box.maxY) continue;
                if (wall.minY >= box.maxY + playerHeight) continue;

                currentWall = wall.maxX;
                offset = box.minX - currentWall - playerWidthX;

                if (offset < 0) {
                    if (xMin == null || currentWall > xMin) xMin = currentWall;
                }
            }

            // X LEFT (East: X+)
            wallBoxes.clear();
            currentWallPos = tempPos.east();
            for (int dy = 0; dy <= 2; dy++) {
                List<AxisAlignedBB> h = CyvForge.getHitbox(currentWallPos.up(dy), world);
                if (h != null) wallBoxes.addAll(h);
            }

            for (int dy = 1; dy <= 2; dy++) {
                List<AxisAlignedBB> inBlock = CyvForge.getHitbox(tempPos.up(dy), world);
                if (inBlock != null) {
                    for (AxisAlignedBB bIn : inBlock) {
                        if (bIn.maxX >= box.maxX - 0.01 && bIn.minX > box.minX + 0.5) wallBoxes.add(bIn);
                    }
                }
            }

            for (AxisAlignedBB wall : wallBoxes) {
                if (wall.minZ > box.minZ + 0.01 || wall.maxZ < box.maxZ - 0.01) continue;
                if (wall.minZ < box.minZ - 0.1 && wall.maxZ > box.maxZ + 0.1 && (wall.maxX - wall.minX) < 0.5) continue;
                if (wall.maxY <= box.maxY) continue;
                if (wall.minY >= box.maxY + playerHeight) continue;

                currentWall = wall.minX;
                offset = currentWall - box.maxX - playerWidthX;

                if (offset < 0) {
                    if (xMax == null || currentWall < xMax) xMax = currentWall;
                }
            }

            newXMinWalls[i] = xMin;
            newXMaxWalls[i] = xMax;
            newZMinWalls[i] = zMin;
            newZMaxWalls[i] = zMax;
        }

        this.xMinWalls = newXMinWalls;
        this.xMaxWalls = newXMaxWalls;
        this.zMinWalls = newZMinWalls;
        this.zMaxWalls = newZMaxWalls;
    }

    public void resetWalls() {
        xMinWalls = null; xMaxWalls = null; zMinWalls = null; zMaxWalls = null;
    }
}