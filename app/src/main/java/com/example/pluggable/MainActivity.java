package com.example.pluggable;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.api.IPluginDownload;
import com.example.api.PlaceholderActivity;
import com.example.api.PluginManager;
import com.example.api.PluginUtil;

import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    private TextView nextTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nextTv = findViewById(R.id.tv_next);
        findViewById(R.id.tv_plugin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PluginManager.getSingleton().downloadApk("https://dev.tencent.com/u/Blue_Sky_Me/p/AdsTool/git/raw/master/plug-debug.apk", new IPluginDownload() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                nextTv.setEnabled(true);
                                Toast.makeText(MainActivity.this, "下载完成", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onFail() {

                    }

                    @Override
                    public void onDownloading(float progress) {

                    }
                });
            }
        });

        findViewById(R.id.tv_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PluginUtil.startPluginActivity(v.getContext(),"plug-debug.apk", "com.example.plug.MainActivity");
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        if (requestCode == 1) {
            PluginManager.getSingleton().downloadApk("https://coding.net/u/Blue_Sky_Me/p/AdsTool/git/raw/master/plug-debug.apk", new IPluginDownload() {
                @Override
                public void onSuccess() {
                    startActivity(new Intent(MainActivity.this, PlaceholderActivity.class));
                }

                @Override
                public void onFail() {

                }

                @Override
                public void onDownloading(float progress) {

                }
            });
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {

    }
}
