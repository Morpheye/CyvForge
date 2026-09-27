package net.cyvforge.gui;

import net.cyvforge.CyvForge;
import net.cyvforge.config.CyvClientColorHelper;
import net.cyvforge.config.CyvClientConfig;
import net.cyvforge.hud.HUDManager;
import net.cyvforge.hud.structure.DraggableHUDElement;
import net.cyvforge.hud.structure.IRenderer;
import net.cyvforge.hud.structure.ScreenPosition;
import net.cyvforge.util.defaults.CyvGui;
import net.cyvforge.util.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Predicate;

public class GuiHUDPositions extends CyvGui {
    protected final HashMap<DraggableHUDElement, ScreenPosition> renderers = new HashMap<DraggableHUDElement, ScreenPosition>();
    protected Optional<DraggableHUDElement> selectedRenderer = Optional.empty();
    protected int prevX;
    protected int prevY;
    protected final boolean fromLabels;
    protected boolean fromPresets = false;

    protected boolean isResizing = false;
    private final int handleSize = 6;

    public GuiHUDPositions(boolean fromLabels, boolean fromPresets) {
        super("HUD Position");
        Collection<DraggableHUDElement> registeredRenderers = HUDManager.registeredRenderers;
        this.fromLabels = fromLabels;
        this.fromPresets = fromPresets;

        for (DraggableHUDElement renderer : registeredRenderers) {
            if (!renderer.isEnabled) continue;

            ScreenPosition pos = renderer.load();
            if (pos == null) {
                pos = renderer.getDefaultPosition();
            }

            adjustBounds(renderer, pos);
            this.renderers.put(renderer, pos);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        final float zBackup = this.zLevel;
        this.zLevel = 200;

        int borderColor = (int) CyvClientColorHelper.color1.getDrawColor();
        GuiUtils.drawRectOutline(0, 0, this.width - 1, this.height - 1, borderColor); //GUI Border

        for (DraggableHUDElement renderer : renderers.keySet()) {
            if (renderer.getWidth() <= 0 || renderer.getHeight() <= 0) continue;

            ScreenPosition pos = renderers.get(renderer);
            if (!renderer.isDraggable) pos = renderer.getDefaultPosition();

            renderer.renderDummy(pos);

            int color = (int) CyvClientColorHelper.color1.getDrawColor();
            if (!renderer.isVisible) color = 0xFFAAAAAA;

            GuiUtils.drawRectOutline(pos.getAbsoluteX(), pos.getAbsoluteY(),
                    pos.getAbsoluteX()+renderer.getWidth(), pos.getAbsoluteY()+renderer.getHeight(), color);

            if (selectedRenderer.isPresent() && selectedRenderer.get() == renderer &&
                    (renderer.getName().equals("keystrokes") || renderer.getName().equals("InputHistory"))) {
                int handleX = pos.getAbsoluteX() + renderer.getWidth() - handleSize;
                int handleY = pos.getAbsoluteY() + renderer.getHeight() - handleSize;
                Gui.drawRect(handleX, handleY, handleX + handleSize, handleY + handleSize, 0xFFFFFFFF);
            }
        }

        this.zLevel = zBackup;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            renderers.entrySet().forEach((entry) -> {
                entry.getKey().save(entry.getValue());
            });

            if (fromPresets) {
                Minecraft.getMinecraft().displayGuiScreen(new GuiPresets());
            } else if (fromLabels) {
                Minecraft.getMinecraft().displayGuiScreen(new GuiMPK());
            } else {
                Minecraft.getMinecraft().displayGuiScreen(null);
            }
            return;
        } else if (keyCode == Keyboard.KEY_UP) {
            if (selectedRenderer.isPresent()) {
                if (selectedRenderer.get().isDraggable) {
                    moveSelectedRenderBy(0,-1);
                    return;
                }
            }
        } else if (keyCode == Keyboard.KEY_LEFT) {
            if (selectedRenderer.isPresent()) {
                if (selectedRenderer.get().isDraggable) {
                    moveSelectedRenderBy(-1,0);
                    return;
                }
            }
        } else if (keyCode == Keyboard.KEY_DOWN) {
            if (selectedRenderer.isPresent()) {
                if (selectedRenderer.get().isDraggable) {
                    moveSelectedRenderBy(0,1);
                    return;
                }
            }
        } else if (keyCode == Keyboard.KEY_RIGHT) {
            if (selectedRenderer.isPresent()) {
                if (selectedRenderer.get().isDraggable) {
                    moveSelectedRenderBy(1,0);
                    return;
                }
            }
        }
    }

    @Override
    public void mouseClickMove(int x, int y, int mouseButton, long time) {
        if (mouseButton == 0) { //left-clicked
            if (isResizing && selectedRenderer.isPresent()) {
                DraggableHUDElement renderer = selectedRenderer.get();
                ScreenPosition pos = renderers.get(renderer);

                if (renderer.getName().equals("keystrokes")) {
                    int newWidth = x - pos.getAbsoluteX() + (handleSize / 2);
                    int finalSize = Math.max(40, Math.min(250, newWidth));
                    if (finalSize != CyvClientConfig.getInt("keystrokesSize", 66)) {
                        CyvClientConfig.set("keystrokesSize", finalSize);
                    }
                }
                else if (renderer.getName().equals("InputHistory")) {
                    float currentScale = CyvClientConfig.getInt("ih_size", 100) / 100.0f;
                    float baseWidth = renderer.getWidth() / currentScale;

                    int newWidth = x - pos.getAbsoluteX();
                    int finalScale = (int)((newWidth / baseWidth) * 100);

                    finalScale = Math.max(50, Math.min(100, finalScale));

                    CyvClientConfig.set("ih_size", finalScale);
                }

            } else if (selectedRenderer.isPresent()) {
                if (!selectedRenderer.get().isDraggable) return;

                moveSelectedRenderBy(x - prevX, y - prevY);
            }
            this.prevX = x; this.prevY = y;
        }
    }

    @Override
    public void mouseClicked(int x, int y, int mouseButton) throws IOException {
        this.prevX = x;
        this.prevY = y;

        if (mouseButton == 0) { //left-clicked
            if (selectedRenderer.isPresent() &&
                    (selectedRenderer.get().getName().equals("keystrokes") || selectedRenderer.get().getName().equals("InputHistory"))) {

                ScreenPosition pos = renderers.get(selectedRenderer.get());
                int handleX = pos.getAbsoluteX() + selectedRenderer.get().getWidth() - handleSize;
                int handleY = pos.getAbsoluteY() + selectedRenderer.get().getHeight() - handleSize;

                if (x >= handleX && x <= handleX + handleSize && y >= handleY && y <= handleY + handleSize) {
                    this.isResizing = true;
                    return;
                }
            }
            this.isResizing = false;
            loadMouseOver(x, y);
        }

        if (mouseButton == 1) { //right-clicked
            loadMouseOver(x, y);
            if (!this.selectedRenderer.isPresent()) return;
            DraggableHUDElement modRender = this.selectedRenderer.get();
            modRender.isVisible = !modRender.isVisible;
        }
    }

    private void loadMouseOver(int x, int y) {
        this.selectedRenderer = renderers.keySet().stream().filter(new MouseOverFinder(x, y)).findFirst();
    }

    private void moveSelectedRenderBy(int offsetX, int offsetY) {
        if (!selectedRenderer.isPresent()) return;
        IRenderer renderer = selectedRenderer.get();
        ScreenPosition pos = renderers.get(renderer);

        pos.setAbsolute(pos.getAbsoluteX() + offsetX, pos.getAbsoluteY() + offsetY);
        adjustBounds(renderer, pos);
    }

    @Override
    public void onGuiClosed() {
        for (IRenderer renderer : renderers.keySet()) {
            renderer.save(renderers.get(renderer));
        }
        net.cyvforge.event.ConfigLoader.save(CyvForge.config, false);
    }

    private void adjustBounds(IRenderer renderer, ScreenPosition pos) {
        ScaledResolution res = new ScaledResolution(Minecraft.getMinecraft());

        int screenWidth = res.getScaledWidth();
        int screenHeight = res.getScaledHeight();

        int absoluteX = Math.max(0, Math.min(pos.getAbsoluteX(), Math.max(screenWidth - renderer.getWidth(), 0)));
        int absoluteY = Math.max(0, Math.min(pos.getAbsoluteY(), Math.max(screenHeight - renderer.getHeight(), 0)));

        pos.setAbsolute(absoluteX, absoluteY);
    }

    private class MouseOverFinder implements Predicate<IRenderer> {
        private int mouseX, mouseY;

        public MouseOverFinder(int x, int y) {
            this.mouseX = x; this.mouseY = y;
        }

        @Override
        public boolean test(IRenderer renderer) {
            ScreenPosition pos = renderers.get(renderer);
            int absoluteX = pos.getAbsoluteX();
            int absoluteY = pos.getAbsoluteY();

            if (mouseX >= absoluteX && mouseX <= absoluteX + renderer.getWidth()) {
                if (mouseY >= absoluteY && mouseY <= absoluteY + renderer.getHeight()) {
                    return true;
                }
            }

            return false;
        }
    }
}