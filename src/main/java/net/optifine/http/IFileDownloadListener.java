package net.optifine.http;

public interface IFileDownloadListener {
    void fileDownloadFinished(String string, byte[] bs, Throwable throwable);
}
