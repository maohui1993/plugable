package com.example.api;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.util.Log;

import java.lang.reflect.Method;

public class PluginInstrumentation extends Instrumentation {
    private static final String LOG_TAG = PluginInstrumentation.class.getSimpleName();
    private Instrumentation mBaseInstrumentation;
    private Method execStartActivity;
    private PluginManager manager;

    public PluginInstrumentation(Instrumentation instrumentation, PluginManager manager) {
        mBaseInstrumentation = instrumentation;
        this.manager = manager;
    }

    @Override
    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Log.e(LOG_TAG, "hook");
        if (className.equals(PlaceholderActivity.class.getCanonicalName())) {
            String realClassName = intent.getStringExtra(Constants.REAL_ACTIVITY_NAME);
            String apkName = intent.getStringExtra(Constants.APK_NAME);
            String originPkgName = intent.getComponent().getPackageName();
            intent.setClassName(originPkgName, realClassName);
            ClassLoader pluginClassLoader = manager.load(apkName);
            try {
                Activity activity = super.newActivity(pluginClassLoader, realClassName, intent);
                Reflector.QuietReflector.with(activity).field("mResources").set(manager.getResource());
                return activity;
            } catch (Exception e) {
                intent.setComponent(new ComponentName(originPkgName, className));
                return super.newActivity(cl, className, intent);
            }
        }
        return super.newActivity(cl, className, intent);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        mBaseInstrumentation.callActivityOnCreate(activity, icicle);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle, PersistableBundle persistentState) {
        mBaseInstrumentation.callActivityOnCreate(activity, icicle, persistentState);
    }


    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options) {
        if (execStartActivity == null) {
            try {
                //这里通过反射找到原始Instrumentation类的execStartActivity方法
                execStartActivity = Instrumentation.class.getDeclaredMethod(
                        "execStartActivity",
                        Context.class, IBinder.class, IBinder.class, Activity.class,
                        Intent.class, int.class, Bundle.class);
                execStartActivity.setAccessible(true);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        if (PluginUtil.isIntentFromPlugin(intent)) {
            intent.setComponent(new ComponentName(who, PlaceholderActivity.class.getCanonicalName()));
            try {
                return (ActivityResult) execStartActivity.invoke(mBaseInstrumentation, who, contextThread, token, target, intent, requestCode, options);
            } catch (Exception e) {
                throw new RuntimeException();
            }
        }  else {
            try {
                return (ActivityResult) execStartActivity.invoke(mBaseInstrumentation, who, contextThread, token, target, intent, requestCode, options);
            } catch (Exception e) {
                try {
                    manager.handleIntent(intent);
                    return (ActivityResult) execStartActivity.invoke(mBaseInstrumentation, who, contextThread, token, target, intent, requestCode, options);
                } catch (Exception e1) {
                    e1.printStackTrace();
                    throw new RuntimeException();
                }
            }
        }
    }
}
