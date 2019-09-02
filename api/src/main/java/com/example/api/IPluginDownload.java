package com.example.api;

public interface IPluginDownload {
    void onSuccess();

    void onFail();

    void onDownloading(float progress);
}
