package com.example.api;

import android.content.Context;
import android.content.Intent;

public class PluginUtil {
    public static boolean isIntentFromPlugin(Intent intent) {
        if (intent == null) {
            return false;
        }
        return intent.getBooleanExtra(Constants.KEY_IS_PLUGIN, false);
    }

    public static void startPluginActivity(Context context, String apkName, String activityName) {
        if (context == null) return;
        Intent intent = new Intent();
        intent.setClassName(context, activityName);
        intent.putExtra(Constants.KEY_IS_PLUGIN, true);
        intent.putExtra(Constants.REAL_ACTIVITY_NAME, activityName);
        intent.putExtra(Constants.APK_NAME, apkName);
        context.startActivity(intent);
    }
}
