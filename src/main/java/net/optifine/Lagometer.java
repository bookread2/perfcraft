package net.optifine;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.profiler.Profiler;
import net.optifine.util.MemoryMonitor;
import org.lwjgl.opengl.GL11;

public class Lagometer {
    private static Minecraft mc;
    private static GameSettings gameSettings;
    private static Profiler profiler;
    public static boolean active = false;
    public static Lagometer.TimerNano timerTick = new Lagometer.TimerNano();
    public static Lagometer.TimerNano timerScheduledExecutables = new Lagometer.TimerNano();
    public static Lagometer.TimerNano timerChunkUpload = new Lagometer.TimerNano();
    public static Lagometer.TimerNano timerChunkUpdate = new Lagometer.TimerNano();
    public static Lagometer.TimerNano timerVisibility = new Lagometer.TimerNano();
    public static Lagometer.TimerNano timerTerrain = new Lagometer.TimerNano();
    public static Lagometer.TimerNano timerServer = new Lagometer.TimerNano();
    private static long[] timesFrame = new long[512];
    private static long[] timesTick = new long[512];
    private static long[] timesScheduledExecutables = new long[512];
    private static long[] timesChunkUpload = new long[512];
    private static long[] timesChunkUpdate = new long[512];
    private static long[] timesVisibility = new long[512];
    private static long[] timesTerrain = new long[512];
    private static long[] timesServer = new long[512];
    private static boolean[] gcs = new boolean[512];
    private static int numRecordedFrameTimes = 0;
    private static long prevFrameTimeNano = -1L;
    private static long renderTimeNano = 0L;

    public static void updateLagometer() {
        if (mc == null) {
            mc = Minecraft.getMinecraft();
            gameSettings = mc.gameSettings;
            profiler = mc.mcProfiler;
        }

        if (gameSettings.showDebugInfo && (gameSettings.ofLagometer || gameSettings.showLagometer)) {
            active = true;
            long timeNowNano = System.nanoTime();
            if (prevFrameTimeNano == -1L) {
                prevFrameTimeNano = timeNowNano;
            } else {
                int frameIndex = numRecordedFrameTimes & timesFrame.length - 1;
                numRecordedFrameTimes++;
                boolean gc = MemoryMonitor.isGcEvent();
                timesFrame[frameIndex] = timeNowNano - prevFrameTimeNano - renderTimeNano;
                timesTick[frameIndex] = timerTick.timeNano;
                timesScheduledExecutables[frameIndex] = timerScheduledExecutables.timeNano;
                timesChunkUpload[frameIndex] = timerChunkUpload.timeNano;
                timesChunkUpdate[frameIndex] = timerChunkUpdate.timeNano;
                timesVisibility[frameIndex] = timerVisibility.timeNano;
                timesTerrain[frameIndex] = timerTerrain.timeNano;
                timesServer[frameIndex] = timerServer.timeNano;
                gcs[frameIndex] = gc;
                timerTick.reset();
                timerScheduledExecutables.reset();
                timerVisibility.reset();
                timerChunkUpdate.reset();
                timerChunkUpload.reset();
                timerTerrain.reset();
                timerServer.reset();
                prevFrameTimeNano = System.nanoTime();
            }
        } else {
            active = false;
            prevFrameTimeNano = -1L;
        }
    }

    public static void showLagometer(ScaledResolution scaledResolution) {
        if (gameSettings != null) {
            if (gameSettings.ofLagometer || gameSettings.showLagometer) {
                long timeRenderStartNano = System.nanoTime();
                GlStateManager.clear(256);
                GlStateManager.matrixMode(5889);
                GlStateManager.pushMatrix();
                GlStateManager.enableColorMaterial();
                GlStateManager.loadIdentity();
                GlStateManager.ortho(0.0, mc.displayWidth, mc.displayHeight, 0.0, 1000.0, 3000.0);
                GlStateManager.matrixMode(5888);
                GlStateManager.pushMatrix();
                GlStateManager.loadIdentity();
                GlStateManager.translate(0.0F, 0.0F, -2000.0F);
                GL11.glLineWidth(1.0F);
                GlStateManager.disableTexture2D();
                Tessellator tess = Tessellator.getInstance();
                WorldRenderer tessellator = tess.getWorldRenderer();
                tessellator.begin(1, DefaultVertexFormats.POSITION_COLOR);

                for (int frameNum = 0; frameNum < timesFrame.length; frameNum++) {
                    int lum = (frameNum - numRecordedFrameTimes & timesFrame.length - 1) * 100 / timesFrame.length;
                    lum += 155;
                    float baseHeight = mc.displayHeight;
                    long heightFrame = 0L;
                    if (gcs[frameNum]) {
                        heightFrame = renderTime(frameNum, timesFrame[frameNum], lum, lum / 2, 0, baseHeight, tessellator);
                    } else {
                        heightFrame = renderTime(frameNum, timesFrame[frameNum], lum, lum, lum, baseHeight, tessellator);
                        baseHeight -= (float)renderTime(frameNum, timesServer[frameNum], lum / 2, lum / 2, lum / 2, baseHeight, tessellator);
                        baseHeight -= (float)renderTime(frameNum, timesTerrain[frameNum], 0, lum, 0, baseHeight, tessellator);
                        baseHeight -= (float)renderTime(frameNum, timesVisibility[frameNum], lum, lum, 0, baseHeight, tessellator);
                        baseHeight -= (float)renderTime(frameNum, timesChunkUpdate[frameNum], lum, 0, 0, baseHeight, tessellator);
                        baseHeight -= (float)renderTime(frameNum, timesChunkUpload[frameNum], lum, 0, lum, baseHeight, tessellator);
                        baseHeight -= (float)renderTime(frameNum, timesScheduledExecutables[frameNum], 0, 0, lum, baseHeight, tessellator);
                        baseHeight -= (float)renderTime(frameNum, timesTick[frameNum], 0, lum, lum, baseHeight, tessellator);
                    }
                }

                renderTimeDivider(0, timesFrame.length, 33333333L, 196, 196, 196, mc.displayHeight, tessellator);
                renderTimeDivider(0, timesFrame.length, 16666666L, 196, 196, 196, mc.displayHeight, tessellator);
                tess.draw();
                GlStateManager.enableTexture2D();
                int y60 = mc.displayHeight - 80;
                int y30 = mc.displayHeight - 160;
                mc.fontRendererObj.drawString("30", 2, y30 + 1, -8947849);
                mc.fontRendererObj.drawString("30", 1, y30, -3881788);
                mc.fontRendererObj.drawString("60", 2, y60 + 1, -8947849);
                mc.fontRendererObj.drawString("60", 1, y60, -3881788);
                GlStateManager.matrixMode(5889);
                GlStateManager.popMatrix();
                GlStateManager.matrixMode(5888);
                GlStateManager.popMatrix();
                GlStateManager.enableTexture2D();
                float lumMem = 1.0F - (float)((System.currentTimeMillis() - MemoryMonitor.getStartTimeMs()) / 1000.0);
                lumMem = Config.limit(lumMem, 0.0F, 1.0F);
                int memColR = (int)(170.0F + lumMem * 85.0F);
                int memColG = (int)(100.0F + lumMem * 55.0F);
                int memColB = (int)(10.0F + lumMem * 10.0F);
                int colMem = memColR << 16 | memColG << 8 | memColB;
                int posX = 512 / scaledResolution.getScaleFactor() + 2;
                int posY = mc.displayHeight / scaledResolution.getScaleFactor() - 8;
                GuiIngame.drawRect(posX - 1, posY - 1, posX + 50, posY + 10, -1605349296);
                mc.fontRendererObj.drawString(" " + MemoryMonitor.getAllocationRateMb() + " MB/s", posX, posY, colMem);
                renderTimeNano = System.nanoTime() - timeRenderStartNano;
            }
        }
    }

    private static long renderTime(int frameNum, long time, int r, int g, int b, float baseHeight, WorldRenderer tessellator) {
        long heightTime = time / 200000L;
        if (heightTime < 3L) {
            return 0L;
        } else {
            tessellator.pos(frameNum + 0.5F, baseHeight - (float)heightTime + 0.5F, 0.0).color(r, g, b, 255).endVertex();
            tessellator.pos(frameNum + 0.5F, baseHeight + 0.5F, 0.0).color(r, g, b, 255).endVertex();
            return heightTime;
        }
    }

    private static long renderTimeDivider(int frameStart, int frameEnd, long time, int r, int g, int b, float baseHeight, WorldRenderer tessellator) {
        long heightTime = time / 200000L;
        if (heightTime < 3L) {
            return 0L;
        } else {
            tessellator.pos(frameStart + 0.5F, baseHeight - (float)heightTime + 0.5F, 0.0).color(r, g, b, 255).endVertex();
            tessellator.pos(frameEnd + 0.5F, baseHeight - (float)heightTime + 0.5F, 0.0).color(r, g, b, 255).endVertex();
            return heightTime;
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static class TimerNano {
        public long timeStartNano = 0L;
        public long timeNano = 0L;

        public void start() {
            if (Lagometer.active) {
                if (this.timeStartNano == 0L) {
                    this.timeStartNano = System.nanoTime();
                }
            }
        }

        public void end() {
            if (Lagometer.active) {
                if (this.timeStartNano != 0L) {
                    this.timeNano = this.timeNano + (System.nanoTime() - this.timeStartNano);
                    this.timeStartNano = 0L;
                }
            }
        }

        private void reset() {
            this.timeNano = 0L;
            this.timeStartNano = 0L;
        }
    }
}
