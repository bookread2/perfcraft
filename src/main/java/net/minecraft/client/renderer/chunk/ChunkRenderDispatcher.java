package net.minecraft.client.renderer.chunk;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RegionRenderCacheBuilder;
import net.minecraft.client.renderer.VertexBufferUploader;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.util.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

public class ChunkRenderDispatcher {
    private static final Logger logger = LogManager.getLogger();
    private static final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("Chunk Batcher %d").setDaemon(true).build();
    private final List<ChunkRenderWorker> listThreadedWorkers = Lists.newArrayList();
    private final BlockingQueue<ChunkCompileTaskGenerator> queueChunkUpdates = Queues.newArrayBlockingQueue(100);
    private final BlockingQueue<RegionRenderCacheBuilder> queueFreeRenderBuilders;
    private final WorldVertexBufferUploader worldVertexUploader = new WorldVertexBufferUploader();
    private final VertexBufferUploader vertexUploader = new VertexBufferUploader();
    private final Queue<ListenableFutureTask<?>> queueChunkUploads = Queues.newArrayDeque();
    private final ChunkRenderWorker renderWorker;
    private final int countRenderBuilders;
    private List<RegionRenderCacheBuilder> listPausedBuilders = new ArrayList<>();

    public ChunkRenderDispatcher() {
        this(-1);
    }

    public ChunkRenderDispatcher(int countRenderBuilders) {
        int m = Math.max(1, (int)(Runtime.getRuntime().maxMemory() * 0.3) / 10485760);
        int c = Math.max(1, MathHelper.clamp_int(Runtime.getRuntime().availableProcessors() - 2, 1, m / 5));
        if (countRenderBuilders < 0) {
            this.countRenderBuilders = MathHelper.clamp_int(c * 8, 1, m);
        } else {
            this.countRenderBuilders = countRenderBuilders;
        }

        for (int i = 0; i < c; i++) {
            ChunkRenderWorker chunkrenderworker = new ChunkRenderWorker(this);
            Thread thread = threadFactory.newThread(chunkrenderworker);
            thread.start();
            this.listThreadedWorkers.add(chunkrenderworker);
        }

        this.queueFreeRenderBuilders = Queues.newArrayBlockingQueue(this.countRenderBuilders);

        for (int j = 0; j < this.countRenderBuilders; j++) {
            this.queueFreeRenderBuilders.add(new RegionRenderCacheBuilder());
        }

        this.renderWorker = new ChunkRenderWorker(this, new RegionRenderCacheBuilder());
    }

    public String getDebugInfo() {
        return String.format("pC: %03d, pU: %1d, aB: %1d", this.queueChunkUpdates.size(), this.queueChunkUploads.size(), this.queueFreeRenderBuilders.size());
    }

    public boolean runChunkUploads(long p_178516_1_) {
        boolean flag = false;

        long i;
        do {
            boolean flag1 = false;
            ListenableFutureTask pendingUpload = null;
            synchronized (this.queueChunkUploads) {
                pendingUpload = this.queueChunkUploads.poll();
            }

            if (pendingUpload != null) {
                pendingUpload.run();
                flag1 = true;
                flag = true;
            }

            if (p_178516_1_ == 0L || !flag1) {
                break;
            }

            i = p_178516_1_ - System.nanoTime();
        } while (i >= 0L);

        return flag;
    }

    public boolean updateChunkLater(RenderChunk chunkRenderer) {
        chunkRenderer.getLockCompileTask().lock();

        boolean flag1;
        try {
            final ChunkCompileTaskGenerator chunkcompiletaskgenerator = chunkRenderer.makeCompileTaskChunk();
            chunkcompiletaskgenerator.addFinishRunnable(new Runnable() {
                @Override
                public void run() {
                    ChunkRenderDispatcher.this.queueChunkUpdates.remove(chunkcompiletaskgenerator);
                }
            });
            boolean flag = this.queueChunkUpdates.offer(chunkcompiletaskgenerator);
            if (!flag) {
                chunkcompiletaskgenerator.finish();
            }

            flag1 = flag;
        } finally {
            chunkRenderer.getLockCompileTask().unlock();
        }

        return flag1;
    }

    public boolean updateChunkNow(RenderChunk chunkRenderer) {
        chunkRenderer.getLockCompileTask().lock();

        boolean flag;
        try {
            ChunkCompileTaskGenerator chunkcompiletaskgenerator = chunkRenderer.makeCompileTaskChunk();

            try {
                this.renderWorker.processTask(chunkcompiletaskgenerator);
            } catch (InterruptedException var8) {
            }

            flag = true;
        } finally {
            chunkRenderer.getLockCompileTask().unlock();
        }

        return flag;
    }

    public void stopChunkUpdates() {
        this.clearChunkUpdates();

        while (this.runChunkUploads(0L)) {
        }

        List<RegionRenderCacheBuilder> list = Lists.newArrayList();

        while (list.size() != this.countRenderBuilders) {
            try {
                list.add(this.allocateRenderBuilder());
            } catch (InterruptedException var3) {
            }
        }

        this.queueFreeRenderBuilders.addAll(list);
    }

    public void freeRenderBuilder(RegionRenderCacheBuilder p_178512_1_) {
        this.queueFreeRenderBuilders.add(p_178512_1_);
    }

    public RegionRenderCacheBuilder allocateRenderBuilder() throws InterruptedException {
        return this.queueFreeRenderBuilders.take();
    }

    public ChunkCompileTaskGenerator getNextChunkUpdate() throws InterruptedException {
        return this.queueChunkUpdates.take();
    }

    public boolean updateTransparencyLater(RenderChunk chunkRenderer) {
        chunkRenderer.getLockCompileTask().lock();

        boolean var4;
        try {
            final ChunkCompileTaskGenerator chunkcompiletaskgenerator = chunkRenderer.makeCompileTaskTransparency();
            if (chunkcompiletaskgenerator != null) {
                chunkcompiletaskgenerator.addFinishRunnable(new Runnable() {
                    @Override
                    public void run() {
                        ChunkRenderDispatcher.this.queueChunkUpdates.remove(chunkcompiletaskgenerator);
                    }
                });
                return this.queueChunkUpdates.offer(chunkcompiletaskgenerator);
            }

            boolean flag = true;
            var4 = flag;
        } finally {
            chunkRenderer.getLockCompileTask().unlock();
        }

        return var4;
    }

    public ListenableFuture<Object> uploadChunk(EnumWorldBlockLayer player, WorldRenderer p_178503_2_, RenderChunk chunkRenderer, CompiledChunk compiledChunkIn) {
        if (Minecraft.getMinecraft().isCallingFromMinecraftThread()) {
            if (OpenGlHelper.useVbo()) {
                this.uploadVertexBuffer(p_178503_2_, chunkRenderer.getVertexBufferByLayer(player.ordinal()));
            } else {
                this.uploadDisplayList(p_178503_2_, ((ListedRenderChunk)chunkRenderer).getDisplayList(player, compiledChunkIn), chunkRenderer);
            }

            p_178503_2_.setTranslation(0.0, 0.0, 0.0);
            return Futures.immediateFuture((Object)null);
        } else {
            ListenableFutureTask<Object> listenablefuturetask = ListenableFutureTask.create(new Runnable() {
                @Override
                public void run() {
                    ChunkRenderDispatcher.this.uploadChunk(player, p_178503_2_, chunkRenderer, compiledChunkIn);
                }
            }, (Object)null);
            synchronized (this.queueChunkUploads) {
                this.queueChunkUploads.add(listenablefuturetask);
                return listenablefuturetask;
            }
        }
    }

    private void uploadDisplayList(WorldRenderer p_178510_1_, int p_178510_2_, RenderChunk chunkRenderer) {
        GL11.glNewList(p_178510_2_, 4864);
        GlStateManager.pushMatrix();
        chunkRenderer.multModelviewMatrix();
        this.worldVertexUploader.draw(p_178510_1_);
        GlStateManager.popMatrix();
        GL11.glEndList();
    }

    private void uploadVertexBuffer(WorldRenderer p_178506_1_, VertexBuffer vertexBufferIn) {
        this.vertexUploader.setVertexBuffer(vertexBufferIn);
        this.vertexUploader.draw(p_178506_1_);
    }

    public void clearChunkUpdates() {
        while (!this.queueChunkUpdates.isEmpty()) {
            ChunkCompileTaskGenerator chunkcompiletaskgenerator = this.queueChunkUpdates.poll();
            if (chunkcompiletaskgenerator != null) {
                chunkcompiletaskgenerator.finish();
            }
        }
    }

    public boolean hasChunkUpdates() {
        return this.queueChunkUpdates.isEmpty() && this.queueChunkUploads.isEmpty();
    }

    public void pauseChunkUpdates() {
        while (this.listPausedBuilders.size() != this.countRenderBuilders) {
            try {
                this.runChunkUploads(Long.MAX_VALUE);
                RegionRenderCacheBuilder builder = this.queueFreeRenderBuilders.poll(100L, TimeUnit.MILLISECONDS);
                if (builder != null) {
                    this.listPausedBuilders.add(builder);
                }
            } catch (InterruptedException var2) {
            }
        }
    }

    public void resumeChunkUpdates() {
        this.queueFreeRenderBuilders.addAll(this.listPausedBuilders);
        this.listPausedBuilders.clear();
    }
}
