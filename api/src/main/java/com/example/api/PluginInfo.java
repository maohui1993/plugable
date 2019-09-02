package com.example.api;

public class PluginInfo {
    private String apkName;
    private String pkgName;

    public PluginInfo(String apkName, String pkgName) {
        this.apkName = apkName;
        this.pkgName = pkgName;
    }

    public String getApkName() {
        return apkName;
    }

    public void setApkName(String apkName) {
        this.apkName = apkName;
    }

    public String getPkgName() {
        return pkgName;
    }

    public void setPkgName(String pkgName) {
        this.pkgName = pkgName;
    }
}
