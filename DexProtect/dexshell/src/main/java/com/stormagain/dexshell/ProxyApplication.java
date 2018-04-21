package com.stormagain.dexshell;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

/**
 * Created by liujian on 2018/4/19.
 */

public class ProxyApplication extends Application {
    private String realApplication;
    private final String encryptFileName = "data.sec";
    private final String KEY = "REAL_APP";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo
                    (getPackageName(), PackageManager.GET_META_DATA);
            realApplication = applicationInfo.metaData.getString(KEY);

            new ClassLoaderDelegate(this).loadDex(base.getAssets().open(encryptFileName));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onCreate() {
        super.onCreate();
        if (!TextUtils.isEmpty(realApplication)) {
            Application app = ClassLoaderDelegate.changeTopApplication(realApplication);
            if (app != null) {
                app.onCreate();
            }
        }
    }


}
