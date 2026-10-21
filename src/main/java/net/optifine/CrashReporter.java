package net.optifine;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.optifine.http.FileUploadThread;
import net.optifine.http.IFileUploadListener;
import net.optifine.shaders.Shaders;

public class CrashReporter {
    public static void onCrashReport(CrashReport crashReport, CrashReportCategory category) {
        try {
            Throwable cause = crashReport.getCrashCause();
            if (cause == null) {
                return;
            }

            if (cause.getClass().getName().contains(".fml.client.SplashProgress")) {
                return;
            }

            if (cause.getClass() == Throwable.class) {
                return;
            }

            extendCrashReport(category);
            GameSettings settings = Config.getGameSettings();
            if (settings == null) {
                return;
            }

            if (!settings.snooperEnabled) {
                return;
            }

            String url = "http://optifine.net/crashReport";
            String reportStr = makeReport(crashReport);
            byte[] content = reportStr.getBytes("ASCII");
            IFileUploadListener listener = new IFileUploadListener() {
                @Override
                public void fileUploadFinished(String url, byte[] contentx, Throwable exception) {
                }
            };
            Map headers = new HashMap();
            headers.put("OF-Version", Config.getVersion());
            headers.put("OF-Summary", makeSummary(crashReport));
            FileUploadThread fut = new FileUploadThread(url, headers, content, listener);
            fut.setPriority(10);
            fut.start();
            Thread.sleep(1000L);
        } catch (Exception var10) {
            Config.dbg(var10.getClass().getName() + ": " + var10.getMessage());
        }
    }

    private static String makeReport(CrashReport crashReport) {
        StringBuffer sb = new StringBuffer();
        sb.append("OptiFineVersion: " + Config.getVersion() + "\n");
        sb.append("Summary: " + makeSummary(crashReport) + "\n");
        sb.append("\n");
        sb.append(crashReport.getCompleteReport());
        sb.append("\n");
        return sb.toString();
    }

    private static String makeSummary(CrashReport crashReport) {
        Throwable t = crashReport.getCrashCause();
        if (t == null) {
            return "Unknown";
        } else {
            StackTraceElement[] traces = t.getStackTrace();
            String firstTrace = "unknown";
            if (traces.length > 0) {
                firstTrace = traces[0].toString().trim();
            }

            return t.getClass().getName() + ": " + t.getMessage() + " (" + crashReport.getDescription() + ")" + " [" + firstTrace + "]";
        }
    }

    public static void extendCrashReport(CrashReportCategory cat) {
        cat.addCrashSection("OptiFine Version", Config.getVersion());
        cat.addCrashSection("OptiFine Build", Config.getBuild());
        if (Config.getGameSettings() != null) {
            cat.addCrashSection("Render Distance Chunks", "" + Config.getChunkViewDistance());
            cat.addCrashSection("Mipmaps", "" + Config.getMipmapLevels());
            cat.addCrashSection("Anisotropic Filtering", "" + Config.getAnisotropicFilterLevel());
            cat.addCrashSection("Antialiasing", "" + Config.getAntialiasingLevel());
            cat.addCrashSection("Multitexture", "" + Config.isMultiTexture());
        }

        cat.addCrashSection("Shaders", "" + Shaders.getShaderPackName());
        cat.addCrashSection("OpenGlVersion", "" + Config.openGlVersion);
        cat.addCrashSection("OpenGlRenderer", "" + Config.openGlRenderer);
        cat.addCrashSection("OpenGlVendor", "" + Config.openGlVendor);
        cat.addCrashSection("CpuCount", "" + Config.getAvailableProcessors());
    }
}
