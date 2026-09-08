package net.cyvforge.event.events;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSign;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class SignCommandListener {

    @SubscribeEvent
    public void onSignInteract(PlayerInteractEvent event) {
        if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            if (event.entityPlayer.isSneaking()) {
                return;
            }

            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null || mc.thePlayer == null) return;

            Block block = mc.theWorld.getBlockState(event.pos).getBlock();

            if (block instanceof BlockSign) {
                TileEntity te = mc.theWorld.getTileEntity(event.pos);

                if (te instanceof TileEntitySign) {
                    TileEntitySign sign = (TileEntitySign) te;

                    StringBuilder sb = new StringBuilder();
                    for (IChatComponent line : sign.signText) {
                        String lineText = line.getUnformattedText().trim();
                        if (!lineText.isEmpty()) {
                            if (sb.length() > 0) sb.append(" ");
                            sb.append(lineText);
                        }
                    }

                    String fullCommand = sb.toString().trim();

                    if (fullCommand.startsWith("/")) {
                        if (ClientCommandHandler.instance.executeCommand(mc.thePlayer, fullCommand) == 0) {
                            mc.thePlayer.sendChatMessage(fullCommand);
                        }

                        if (event.isCancelable()) {
                            event.setCanceled(true);
                        }
                    }
                }
            }
        }
    }
}