package net.cyvforge.util.parkour;

import net.cyvforge.CyvForge;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;

public class LandingBlock {

    public BlockPos pos;
    public AxisAlignedBB[] bb; //bounding boxes
    public LandingMode mode;
    public LandingAxis axis;
    public boolean isBox;
    public boolean neoAndNormal;
    public boolean isLiquid = false;
    public double liquidBottomOffset = 0;
    public int targetTick = -1;

    public Double pb;
    public Double pbX;
    public Double pbZ;

    public Double lastOffsetX;
    public Double lastOffsetZ;
    public Double lastPb;

    public Double xMaxWall, xMinWall, zMaxWall, zMinWall;

    public double xMinCond, xMaxCond, zMinCond, zMaxCond; //detection boxes (for offsets)

    public LandingBlock(BlockPos pos, LandingMode mode, LandingAxis axis, boolean isBox) {
        this.pos = pos;
        this.mode = mode;
        this.axis = axis;
        this.isBox = isBox;

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
                    1-playerX/2 + pos.getX(), 1 + pos.getY(), 1-playerZ/2 + pos.getZ());
            this.bb = new AxisAlignedBB[] {box};

            return;
        }

        this.bb = CyvForge.getHitbox(pos, world).toArray(new AxisAlignedBB[0]);

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

    public void calculateWalls() {
        World world = Minecraft.getMinecraft().theWorld;
        AxisAlignedBB playerHitbox = Minecraft.getMinecraft().thePlayer.getEntityBoundingBox();
        BlockPos tempPos = pos; //new variable, because this will be lowered by one if the mode is currently "enter"
        if (this.mode == LandingMode.enter) tempPos = tempPos.down();

        xMinWall = null; xMaxWall = null; zMinWall = null; zMaxWall = null;

        for (AxisAlignedBB box : bb) {
            ArrayList<AxisAlignedBB> wallBoxes = new ArrayList<AxisAlignedBB>();
            BlockPos currentWallPos = null; //current x/z position of checked wall
            double offset, currentWall; //temporary variables

            //z back
            currentWallPos = tempPos.north();
            for (double i = 0; i < (playerHitbox.maxY - playerHitbox.minY); i++) {
                currentWallPos = currentWallPos.up();
                wallBoxes.addAll(CyvForge.getHitbox(currentWallPos, world));
            }
            for (AxisAlignedBB wall : wallBoxes) {
                if ((wall.maxX - wall.minX) < (box.maxX - box.minX)) continue; //skip if not wide enough
                currentWall = wall.maxZ;
                offset = box.minZ - currentWall - (playerHitbox.maxZ - playerHitbox.minZ);

                if (offset < 0) {
                    if (zMinWall == null || currentWall > zMinWall) zMinWall = currentWall;
                }
            }

            //z front
            wallBoxes.clear();
            currentWallPos = tempPos.south();
            for (double i = 0; i < (playerHitbox.maxY - playerHitbox.minY); i++) {
                currentWallPos = currentWallPos.up();
                wallBoxes.addAll(CyvForge.getHitbox(currentWallPos, world));
            }
            for (AxisAlignedBB wall : wallBoxes) {
                if ((wall.maxX - wall.minX) < (box.maxX - box.minX)) continue; //skip if not wide enough
                currentWall = wall.minZ;
                offset = currentWall - box.maxZ - (playerHitbox.maxZ - playerHitbox.minZ);

                if (offset < 0) {
                    if (zMaxWall == null || currentWall > zMaxWall) zMaxWall = currentWall;
                }
            }

            //x right
            wallBoxes.clear();
            currentWallPos = tempPos.west();
            for (double i = 0; i < (playerHitbox.maxY - playerHitbox.minY); i++) {
                currentWallPos = currentWallPos.up();
                wallBoxes.addAll(CyvForge.getHitbox(currentWallPos, world));
            }
            for (AxisAlignedBB wall : wallBoxes) {
                if ((wall.maxZ - wall.minZ) < (box.maxZ - box.minZ)) continue; //skip if not wide enough
                currentWall = wall.maxX;
                offset = box.minX - currentWall - (playerHitbox.maxX - playerHitbox.minX);

                if (offset < 0) {
                    if (xMinWall == null || currentWall > xMinWall) xMinWall = currentWall;
                }
            }

            //x left
            wallBoxes.clear();
            currentWallPos = tempPos.east();
            for (double i = 0; i < (playerHitbox.maxY - playerHitbox.minY); i++) {
                currentWallPos = currentWallPos.up();
                wallBoxes.addAll(CyvForge.getHitbox(currentWallPos, world));
            }
            for (AxisAlignedBB wall : wallBoxes) {
                if ((wall.maxZ - wall.minZ) < (box.maxZ - box.minZ)) continue; //skip if not wide enough
                currentWall = wall.minX;
                offset = currentWall - box.maxX - (playerHitbox.maxX - playerHitbox.minX);

                if (offset < 0) {
                    if (xMaxWall == null || currentWall > xMaxWall) xMaxWall = currentWall;
                }
            }

        }
    }

    public void resetWalls() {
        xMinWall = null; xMaxWall = null; zMinWall = null; zMaxWall = null;
    }
}
