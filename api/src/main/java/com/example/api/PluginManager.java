package com.example.api;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android.app.ActivityThread;
import dalvik.system.DexClassLoader;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PluginManager {
    private static final String LOG_TAG = PluginManager.class.getSimpleName();
    private static String APK_DIR;
    private static Context mContext;
    private static volatile PluginManager instance;
    private Map<String, Resources> resourcesMap = new ConcurrentHashMap<>();
    private Map<String, DexClassLoader> pluginClassLoaderMap = new ConcurrentHashMap<>();
    private OkHttpClient okHttpClient;

    private PluginManager(Context context) {
        mContext = context.getApplicationContext();
        okHttpClient = new OkHttpClient();
        boolean externalStorageAvailable = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (externalStorageAvailable) {
            APK_DIR = context.getExternalFilesDir(null).getPath() + File.separator + "plugin/";
        } else {
            APK_DIR = context.getFilesDir().getPath() + File.separator + "plugin/";
        }
    }

    public static PluginManager init(Context context) {
        mContext = context;
        getSingleton().hookInstrumentation();
        return getSingleton();
    }

    public void preLoad() {
        File file = new File(APK_DIR);
        if (!file.exists()) return;
        File[] files = file.listFiles();
        if (files == null) return;
        for (File f : files){
            if (f.getName().endsWith(".apk")) {
                load(f.getName());
            }
        }
    }

    private void hookInstrumentation() {
        try {
//            Class activityThread = Class.forName("android.android.app.ActivityThread");
//            Method currentActivityThread = activityThread.getDeclaredMethod("currentActivityThread");
//            Field mInstrumentation = activityThread.getDeclaredField("mInstrumentation");
//            //获取主线程对象
//            Object activityThreadObject = currentActivityThread.invoke(null);
//            // 获取mInstrumentation对象
//            mInstrumentation.setAccessible(true);
//            Instrumentation instrumentation = (Instrumentation) mInstrumentation.get(activityThreadObject);
//            PluginInstrumentation pluginInstrumentation = new PluginInstrumentation(instrumentation, this);
//            mInstrumentation.set(activityThreadObject, pluginInstrumentation);
//            Log.d(LOG_TAG, "hook成功");

            ActivityThread activityThread = ActivityThread.currentActivityThread();
            Field mInstrumentation = activityThread.getClass().getDeclaredField("mInstrumentation");
            // 获取mInstrumentation对象
            mInstrumentation.setAccessible(true);
            Instrumentation instrumentation = (Instrumentation) mInstrumentation.get(activityThread);
            PluginInstrumentation pluginInstrumentation = new PluginInstrumentation(instrumentation, this);
            mInstrumentation.set(activityThread, pluginInstrumentation);
            Log.d(LOG_TAG, "hook成功");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static PluginManager getSingleton() {
        if (mContext == null) throw new RuntimeException("请先调用init()");
        if (instance == null) {
            synchronized (PluginManager.class) {
                if (instance == null) {
                    instance = new PluginManager(mContext);
                }
            }
        }
        return instance;
    }

    public DexClassLoader load(String apkName) {
        if (pluginClassLoaderMap.containsKey(apkName)) {
            return pluginClassLoaderMap.get(apkName);
        }
        DexClassLoader classLoader = new DexClassLoader(APK_DIR + File.separator + apkName, APK_DIR, null, mContext.getClassLoader());
        pluginClassLoaderMap.put(apkName, classLoader);
        loadResource(mContext, APK_DIR + File.separator + apkName);
        return classLoader;
    }

    public void loadResource(Context context, String apkPath) {
        try {
            AssetManager am = AssetManager.class.newInstance();
            Reflector.with(am).method("addAssetPath", String.class).call(apkPath);
            Resources hostResources = context.getResources();
            Resources r = new Resources(am, hostResources.getDisplayMetrics(), hostResources.getConfiguration());
            // 解析插件中的manifest
            PackageInfo packageInfo = mContext.getPackageManager().getPackageArchiveInfo(apkPath, 0);
            resourcesMap.put(apkPath, r);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (Reflector.ReflectedException e) {
            e.printStackTrace();
        }
    }

    public void downloadApk(final String url, final IPluginDownload listener) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        okHttpClient.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        InputStream is = null;
                        byte[] buf = new byte[2048];
                        int len;
                        FileOutputStream fos = null;
                        // 储存下载文件的目录
                        String savePath = APK_DIR;
                        try {
                            is = response.body().byteStream();
                            long total = response.body().contentLength();
                            File dir = new File(savePath);
                            if (!dir.exists()) {
                                dir.mkdirs();
                            }
                            File file = new File(dir, getNameFromUrl(url));
                            fos = new FileOutputStream(file);
                            Log.e("pluginManager", file.getAbsolutePath());
                            long sum = 0;
                            while ((len = is.read(buf)) != -1) {
                                fos.write(buf, 0, len);
                                sum += len;
                                int progress = (int) (sum * 1.0f / total * 100);
                                // 下载中
                                listener.onDownloading(progress);
                            }
                            fos.flush();
                            // 下载完成
                            listener.onSuccess();
                        } catch (Exception e) {
                            listener.onFail();
                        } finally {
                            try {
                                if (is != null)
                                    is.close();
                            } catch (IOException e) {
                            }
                            try {
                                if (fos != null)
                                    fos.close();
                            } catch (IOException e) {
                            }
                        }
                    }
                });
    }

    /**
     * @param url
     * @return 从下载连接中解析出文件名
     */
    @NonNull
    private String getNameFromUrl(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }

    public Resources getResource() {
        String key = resourcesMap.keySet().iterator().next();
        return resourcesMap.get(key);
    }

    public String getPluginPkg(ClassLoader classLoader) {
        Set<String> keySet = pluginClassLoaderMap.keySet();
        for (String key : keySet) {
            if (pluginClassLoaderMap.get(key) == classLoader) {
                return key;
            }
        }
        return null;
    }

    public void handleIntent(Intent intent) {
        Set<String> keySet = pluginClassLoaderMap.keySet();
        for (String apkName : keySet) {
            ClassLoader classLoader = pluginClassLoaderMap.get(apkName);
            try {
                String realClassName = intent.getComponent().getClassName();
                classLoader.loadClass(intent.getComponent().getClassName());
                intent.setClassName(mContext, PlaceholderActivity.class.getCanonicalName());
                intent.putExtra(Constants.KEY_IS_PLUGIN, true);
                intent.putExtra(Constants.REAL_ACTIVITY_NAME, realClassName);
                intent.putExtra(Constants.APK_NAME, apkName);
                return;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                continue;
            }
        }
    }
}
