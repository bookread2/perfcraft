package net.minecraft.server.integrated;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.Callable;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ThreadLanServerPing;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.crash.CrashReport;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.server.S2BPacketChangeGameState;
import net.minecraft.profiler.PlayerUsageSnooper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.CryptManager;
import net.minecraft.util.HttpUtil;
import net.minecraft.util.Util;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldManager;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.WorldSettings.GameType;
import net.minecraft.world.demo.DemoWorldServer;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.optifine.ClearWater;
import net.optifine.Config;
import net.optifine.reflect.Reflector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IntegratedServer extends MinecraftServer {
    private static final Logger logger = LogManager.getLogger();
    private final Minecraft mc;
    private final WorldSettings theWorldSettings;
    private boolean isGamePaused;
    private boolean isPublic;
    private ThreadLanServerPing lanServerPing;
    private long ticksSaveLast = 0L;
    public World difficultyUpdateWorld = null;
    public BlockPos difficultyUpdatePos = null;
    public DifficultyInstance difficultyLast = null;

    public IntegratedServer(Minecraft mcIn) {
        super(mcIn.getProxy(), new File(mcIn.mcDataDir, USER_CACHE_FILE.getName()));
        this.mc = mcIn;
        this.theWorldSettings = null;
    }

    public IntegratedServer(Minecraft mcIn, String folderName, String worldName, WorldSettings settings) {
        super(new File(mcIn.mcDataDir, "saves"), mcIn.getProxy(), new File(mcIn.mcDataDir, USER_CACHE_FILE.getName()));
        this.setServerOwner(mcIn.getSession().getUsername());
        this.setFolderName(folderName);
        this.setWorldName(worldName);
        this.setDemo(mcIn.isDemo());
        this.canCreateBonusChest(settings.isBonusChestEnabled());
        this.setBuildLimit(256);
        this.setConfigManager(new IntegratedPlayerList(this));
        this.mc = mcIn;
        this.theWorldSettings = this.isDemo() ? DemoWorldServer.demoWorldSettings : settings;
        ISaveHandler isavehandler = this.getActiveAnvilConverter().getSaveLoader(folderName, false);
        WorldInfo worldinfo = isavehandler.loadWorldInfo();
        if (worldinfo != null) {
            NBTTagCompound nbt = worldinfo.getPlayerNBTTagCompound();
            if (nbt != null && nbt.hasKey("Dimension")) {
                int dim = nbt.getInteger("Dimension");
                PacketThreadUtil.lastDimensionId = dim;
                this.mc.loadingScreen.setLoadingProgress(-1);
            }
        }
    }

    @Override
    protected ServerCommandManager createNewCommandManager() {
        return new IntegratedServerCommandManager();
    }

    @Override
    protected void loadAllWorlds(String saveName, String worldNameIn, long seed, WorldType type, String worldNameIn2) {
        this.convertMapIfNeeded(saveName);
        boolean forge = Reflector.DimensionManager.exists();
        if (!forge) {
            this.worldServers = new WorldServer[3];
            this.timeOfLastDimensionTick = new long[this.worldServers.length][100];
        }

        ISaveHandler isavehandler = this.getActiveAnvilConverter().getSaveLoader(saveName, true);
        this.setResourcePackFromWorld(this.getFolderName(), isavehandler);
        WorldInfo worldinfo = isavehandler.loadWorldInfo();
        if (worldinfo == null) {
            worldinfo = new WorldInfo(this.theWorldSettings, worldNameIn);
        } else {
            worldinfo.setWorldName(worldNameIn);
        }

        if (forge) {
            WorldServer overWorld = this.isDemo()
                ? (WorldServer)new DemoWorldServer(this, isavehandler, worldinfo, 0, this.theProfiler).init()
                : (WorldServer)new WorldServer(this, isavehandler, worldinfo, 0, this.theProfiler).init();
            overWorld.initialize(this.theWorldSettings);
            Integer[] dimIds = (Integer[])Reflector.call(Reflector.DimensionManager_getStaticDimensionIDs);
            Integer[] arr$ = dimIds;
            int len$ = dimIds.length;

            for (int i$ = 0; i$ < len$; i$++) {
                int dim = arr$[i$];
                WorldServer world = dim == 0 ? overWorld : (WorldServer)new WorldServerMulti(this, isavehandler, dim, overWorld, this.theProfiler).init();
                world.addWorldAccess(new WorldManager(this, world));
                if (!this.isSinglePlayer()) {
                    world.getWorldInfo().setGameType(this.getGameType());
                }

                if (Reflector.EventBus.exists()) {
                    Reflector.postForgeBusEvent(Reflector.WorldEvent_Load_Constructor, world);
                }
            }

            this.getConfigurationManager().setPlayerManager(new WorldServer[]{overWorld});
            if (overWorld.getWorldInfo().getDifficulty() == null) {
                this.setDifficultyForAllWorlds(this.mc.gameSettings.difficulty);
            }
        } else {
            for (int i = 0; i < this.worldServers.length; i++) {
                int j = 0;
                if (i == 1) {
                    j = -1;
                }

                if (i == 2) {
                    j = 1;
                }

                if (i == 0) {
                    if (this.isDemo()) {
                        this.worldServers[i] = (WorldServer)new DemoWorldServer(this, isavehandler, worldinfo, j, this.theProfiler).init();
                    } else {
                        this.worldServers[i] = (WorldServer)new WorldServer(this, isavehandler, worldinfo, j, this.theProfiler).init();
                    }

                    this.worldServers[i].initialize(this.theWorldSettings);
                } else {
                    this.worldServers[i] = (WorldServer)new WorldServerMulti(this, isavehandler, j, this.worldServers[0], this.theProfiler).init();
                }

                this.worldServers[i].addWorldAccess(new WorldManager(this, this.worldServers[i]));
            }

            this.getConfigurationManager().setPlayerManager(this.worldServers);
            if (this.worldServers[0].getWorldInfo().getDifficulty() == null) {
                this.setDifficultyForAllWorlds(this.mc.gameSettings.difficulty);
            }
        }

        this.initialWorldChunkLoad();
    }

    @Override
    protected boolean startServer() throws IOException {
        logger.info("Starting integrated minecraft server version 1.9");
        this.setOnlineMode(true);
        this.setCanSpawnAnimals(true);
        this.setCanSpawnNPCs(true);
        this.setAllowPvp(true);
        this.setAllowFlight(true);
        logger.info("Generating keypair");
        this.setKeyPair(CryptManager.generateKeyPair());
        if (Reflector.FMLCommonHandler_handleServerAboutToStart.exists()) {
            Object inst = Reflector.call(Reflector.FMLCommonHandler_instance);
            if (!Reflector.callBoolean(inst, Reflector.FMLCommonHandler_handleServerAboutToStart, this)) {
                return false;
            }
        }

        this.loadAllWorlds(
            this.getFolderName(),
            this.getWorldName(),
            this.theWorldSettings.getSeed(),
            this.theWorldSettings.getTerrainType(),
            this.theWorldSettings.getWorldName()
        );
        this.setMOTD(this.getServerOwner() + " - " + this.worldServers[0].getWorldInfo().getWorldName());
        if (Reflector.FMLCommonHandler_handleServerStarting.exists()) {
            Object inst = Reflector.call(Reflector.FMLCommonHandler_instance);
            if (Reflector.FMLCommonHandler_handleServerStarting.getReturnType() == boolean.class) {
                return Reflector.callBoolean(inst, Reflector.FMLCommonHandler_handleServerStarting, this);
            }

            Reflector.callVoid(inst, Reflector.FMLCommonHandler_handleServerStarting, this);
        }

        return true;
    }

    @Override
    public void tick() {
        this.onTick();
        boolean flag = this.isGamePaused;
        this.isGamePaused = Minecraft.getMinecraft().getNetHandler() != null && Minecraft.getMinecraft().isGamePaused();
        if (!flag && this.isGamePaused) {
            logger.info("Saving and pausing game...");
            this.getConfigurationManager().saveAllPlayerData();
            this.saveAllWorlds(false);
        }

        if (this.isGamePaused) {
            synchronized (this.futureTaskQueue) {
                while (!this.futureTaskQueue.isEmpty()) {
                    Util.runTask(this.futureTaskQueue.poll(), logger);
                }
            }
        } else {
            super.tick();
            if (this.mc.gameSettings.renderDistanceChunks != this.getConfigurationManager().getViewDistance()) {
                logger.info(
                    "Changing view distance to {}, from {}", this.mc.gameSettings.renderDistanceChunks, this.getConfigurationManager().getViewDistance()
                );
                this.getConfigurationManager().setViewDistance(this.mc.gameSettings.renderDistanceChunks);
            }

            if (this.mc.theWorld != null) {
                WorldInfo worldinfo1 = this.worldServers[0].getWorldInfo();
                WorldInfo worldinfo = this.mc.theWorld.getWorldInfo();
                if (!worldinfo1.isDifficultyLocked() && worldinfo.getDifficulty() != worldinfo1.getDifficulty()) {
                    logger.info("Changing difficulty to {}, from {}", worldinfo.getDifficulty(), worldinfo1.getDifficulty());
                    this.setDifficultyForAllWorlds(worldinfo.getDifficulty());
                } else if (worldinfo.isDifficultyLocked() && !worldinfo1.isDifficultyLocked()) {
                    logger.info("Locking difficulty to {}", worldinfo.getDifficulty());

                    for (WorldServer worldserver : this.worldServers) {
                        if (worldserver != null) {
                            worldserver.getWorldInfo().setDifficultyLocked(true);
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean canStructuresSpawn() {
        return false;
    }

    @Override
    public GameType getGameType() {
        return this.theWorldSettings.getGameType();
    }

    @Override
    public EnumDifficulty getDifficulty() {
        return this.mc.theWorld == null ? this.mc.gameSettings.difficulty : this.mc.theWorld.getWorldInfo().getDifficulty();
    }

    @Override
    public boolean isHardcore() {
        return this.theWorldSettings.getHardcoreEnabled();
    }

    @Override
    public boolean shouldBroadcastRconToOps() {
        return true;
    }

    @Override
    public boolean shouldBroadcastConsoleToOps() {
        return true;
    }

    @Override
    public void saveAllWorlds(boolean dontLog) {
        if (dontLog) {
            int ticks = this.getTickCounter();
            int ticksSaveInterval = this.mc.gameSettings.ofAutoSaveTicks;
            if (ticks < this.ticksSaveLast + ticksSaveInterval) {
                return;
            }

            this.ticksSaveLast = ticks;
        }

        super.saveAllWorlds(dontLog);
    }

    @Override
    public File getDataDirectory() {
        return this.mc.mcDataDir;
    }

    @Override
    public boolean isDedicatedServer() {
        return false;
    }

    @Override
    public boolean shouldUseNativeTransport() {
        return false;
    }

    @Override
    protected void finalTick(CrashReport report) {
        this.mc.crashed(report);
    }

    @Override
    public CrashReport addServerInfoToCrashReport(CrashReport report) {
        report = super.addServerInfoToCrashReport(report);
        report.getCategory().addCrashSectionCallable("Type", new Callable<String>() {
            public String call() throws Exception {
                return "Integrated Server (map_client.txt)";
            }
        });
        report.getCategory()
            .addCrashSectionCallable(
                "Is Modded",
                new Callable<String>() {
                    public String call() throws Exception {
                        String s = ClientBrandRetriever.getClientModName();
                        if (!s.equals("vanilla")) {
                            return "Definitely; Client brand changed to '" + s + "'";
                        } else {
                            s = IntegratedServer.this.getServerModName();
                            return !s.equals("vanilla")
                                ? "Definitely; Server brand changed to '" + s + "'"
                                : (
                                    Minecraft.class.getSigners() == null
                                        ? "Very likely; Jar signature invalidated"
                                        : "Probably not. Jar signature remains and both client + server brands are untouched."
                                );
                        }
                    }
                }
            );
        return report;
    }

    @Override
    public void setDifficultyForAllWorlds(EnumDifficulty difficulty) {
        super.setDifficultyForAllWorlds(difficulty);
        if (this.mc.theWorld != null) {
            this.mc.theWorld.getWorldInfo().setDifficulty(difficulty);
        }
    }

    @Override
    public void addServerStatsToSnooper(PlayerUsageSnooper playerSnooper) {
        super.addServerStatsToSnooper(playerSnooper);
        playerSnooper.addClientStat("snooper_partner", this.mc.getPlayerUsageSnooper().getUniqueID());
    }

    @Override
    public boolean isSnooperEnabled() {
        return Minecraft.getMinecraft().isSnooperEnabled();
    }

    @Override
    public String shareToLAN(GameType type, boolean allowCheats) {
        try {
            int i = -1;

            try {
                i = HttpUtil.getSuitableLanPort();
            } catch (IOException var5) {
            }

            if (i <= 0) {
                i = 25564;
            }

            this.getNetworkSystem().addLanEndpoint((InetAddress)null, i);
            logger.info("Started on " + i);
            this.isPublic = true;
            this.lanServerPing = new ThreadLanServerPing(this.getMOTD(), i + "");
            this.lanServerPing.start();
            this.getConfigurationManager().setGameType(type);
            this.getConfigurationManager().setCommandsAllowedForAll(allowCheats);
            return i + "";
        } catch (IOException var61) {
            return null;
        }
    }

    @Override
    public void stopServer() {
        super.stopServer();
        if (this.lanServerPing != null) {
            this.lanServerPing.interrupt();
            this.lanServerPing = null;
        }
    }

    @Override
    public void initiateShutdown() {
        if (!Reflector.MinecraftForge.exists() || this.isServerRunning()) {
            Futures.getUnchecked(this.addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    for (EntityPlayerMP entityplayermp : Lists.newArrayList(IntegratedServer.this.getConfigurationManager().getPlayerList())) {
                        IntegratedServer.this.getConfigurationManager().playerLoggedOut(entityplayermp);
                    }
                }
            }));
        }

        super.initiateShutdown();
        if (this.lanServerPing != null) {
            this.lanServerPing.interrupt();
            this.lanServerPing = null;
        }
    }

    public void setStaticInstance() {
        this.setInstance();
    }

    public boolean getPublic() {
        return this.isPublic;
    }

    @Override
    public void setGameType(GameType gameMode) {
        this.getConfigurationManager().setGameType(gameMode);
    }

    @Override
    public boolean isCommandBlockEnabled() {
        return true;
    }

    @Override
    public int getOpPermissionLevel() {
        return 4;
    }

    private void onTick() {
        for (WorldServer ws : Arrays.asList(this.worldServers)) {
            this.onTick(ws);
        }
    }

    public DifficultyInstance getDifficultyAsync(World world, BlockPos blockPos) {
        this.difficultyUpdateWorld = world;
        this.difficultyUpdatePos = blockPos;
        return this.difficultyLast;
    }

    private void onTick(WorldServer ws) {
        if (!Config.isTimeDefault()) {
            this.fixWorldTime(ws);
        }

        if (!Config.isWeatherEnabled()) {
            this.fixWorldWeather(ws);
        }

        if (Config.waterOpacityChanged) {
            Config.waterOpacityChanged = false;
            ClearWater.updateWaterOpacity(Config.getGameSettings(), ws);
        }

        if (this.difficultyUpdateWorld == ws && this.difficultyUpdatePos != null) {
            this.difficultyLast = ws.getDifficultyForLocation(this.difficultyUpdatePos);
            this.difficultyUpdateWorld = null;
            this.difficultyUpdatePos = null;
        }
    }

    private void fixWorldWeather(WorldServer ws) {
        WorldInfo worldInfo = ws.getWorldInfo();
        if (worldInfo.isRaining() || worldInfo.isThundering()) {
            worldInfo.setRainTime(0);
            worldInfo.setRaining(false);
            ws.setRainStrength(0.0F);
            worldInfo.setThunderTime(0);
            worldInfo.setThundering(false);
            ws.setThunderStrength(0.0F);
            this.getConfigurationManager().sendPacketToAllPlayers(new S2BPacketChangeGameState(2, 0.0F));
            this.getConfigurationManager().sendPacketToAllPlayers(new S2BPacketChangeGameState(7, 0.0F));
            this.getConfigurationManager().sendPacketToAllPlayers(new S2BPacketChangeGameState(8, 0.0F));
        }
    }

    private void fixWorldTime(WorldServer ws) {
        WorldInfo worldInfo = ws.getWorldInfo();
        if (worldInfo.getGameType().getID() == 1) {
            long time = ws.getWorldTime();
            long timeOfDay = time % 24000L;
            if (Config.isTimeDayOnly()) {
                if (timeOfDay <= 1000L) {
                    ws.setWorldTime(time - timeOfDay + 1001L);
                }

                if (timeOfDay >= 11000L) {
                    ws.setWorldTime(time - timeOfDay + 24001L);
                }
            }

            if (Config.isTimeNightOnly()) {
                if (timeOfDay <= 14000L) {
                    ws.setWorldTime(time - timeOfDay + 14001L);
                }

                if (timeOfDay >= 22000L) {
                    ws.setWorldTime(time - timeOfDay + 24000L + 14001L);
                }
            }
        }
    }
}
