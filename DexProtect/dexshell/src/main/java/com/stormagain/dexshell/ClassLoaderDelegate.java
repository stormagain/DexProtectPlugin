package com.stormagain.dexshell;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;

import dalvik.system.DexClassLoader;

/**
 * Created by liujian on 2018/4/12.
 */

public class ClassLoaderDelegate {

    private Context context = null;
    private Class<?> loadedApkClass = null;
    private WeakReference<?> loadedApkRef = null;

    public ClassLoaderDelegate(Context ctx) {
        this.context = ctx;
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
            Object activityThread = currentActivityThreadMethod.invoke(null);

            Field mPackagesField = activityThreadClass.getDeclaredField("mPackages");
            mPackagesField.setAccessible(true); //取消默认 Java 语言访问控制检查的能力（暴力反射）
            Map mPackages = (Map) mPackagesField.get(activityThread);
            loadedApkRef = (WeakReference) mPackages.get(ctx.getPackageName());

            loadedApkClass = Class.forName("android.app.LoadedApk");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ClassLoader getAppClassLoader() {
        try {
            Field mClassLoaderField = loadedApkClass.getDeclaredField("mClassLoader");
            mClassLoaderField.setAccessible(true);
            return (ClassLoader) mClassLoaderField.get(loadedApkRef.get());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean setAppClassLoader(ClassLoader newClassLoader) {
        try {
            Field mClassLoaderField = loadedApkClass.getDeclaredField("mClassLoader");
            mClassLoaderField.setAccessible(true);
            mClassLoaderField.set(loadedApkRef.get(), newClassLoader);
            return true;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return false;
    }

    public ClassLoader loadDex(InputStream inputStream) {
        try {
            ClassLoader appClassLoader = getAppClassLoader();
            ClassLoader newClassLoader = null;
            File decryptFile = new File(context.getCacheDir(), "classes.dex");
            String libPath = context.getApplicationInfo().nativeLibraryDir;
            File odexDir = context.getFilesDir();
            dex2file(inputStream, decryptFile);
            if (appClassLoader != null) {
                newClassLoader = new DexClassLoader(decryptFile.getAbsolutePath(), odexDir.getAbsolutePath(), libPath, appClassLoader);
            } else {
                newClassLoader = new DexClassLoader(decryptFile.getAbsolutePath(), odexDir.getAbsolutePath(), libPath, context.getClassLoader());
            }
            setAppClassLoader(newClassLoader);
            return newClassLoader;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static boolean dex2file(InputStream in, File outFile) {
        File outDir = outFile.getParentFile();
        if (!outDir.exists() && outDir.isDirectory()) {
            outDir.mkdirs();
        }

        FileOutputStream out = null;
        try {
            if (outFile.exists()) {
                outFile.delete();
            }

            out = new FileOutputStream(outFile);
            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();

            byte[] buff = new byte[1024];
            int len;
            while ((len = in.read(buff)) != -1) {
                byteOutput.write(buff, 0, len);
            }

            byte[] dexBytes = byteOutput.toByteArray();
            byte[] decryptBuff = Decrypt.decrypt(dexBytes);
            out.write(decryptBuff, 0, decryptBuff.length);
            out.flush();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static Application changeTopApplication(String appClassName) {
        Object currentActivityThread = Reflect.invokeMethod("android.app.ActivityThread", null, "currentActivityThread", new Object[]{}, new Class[]{});
        Object mBoundApplication = Reflect.getFieldValue(
                "android.app.ActivityThread", currentActivityThread,
                "mBoundApplication");
        Object loadedApkInfo = Reflect.getFieldValue(
                "android.app.ActivityThread$AppBindData",
                mBoundApplication, "info");
        Reflect.setFieldValue("android.app.LoadedApk", loadedApkInfo, "mApplication", null);
        Object oldApplication = Reflect.getFieldValue(
                "android.app.ActivityThread", currentActivityThread,
                "mInitialApplication");
        ArrayList<Application> mAllApplications = (ArrayList<Application>) Reflect
                .getFieldValue("android.app.ActivityThread",
                        currentActivityThread, "mAllApplications");
        mAllApplications.remove(oldApplication);

        ApplicationInfo loadedApk = (ApplicationInfo) Reflect
                .getFieldValue("android.app.LoadedApk", loadedApkInfo,
                        "mApplicationInfo");
        ApplicationInfo appBindData = (ApplicationInfo) Reflect
                .getFieldValue("android.app.ActivityThread$AppBindData",
                        mBoundApplication, "appInfo");

        loadedApk.className = appClassName;
        appBindData.className = appClassName;

        Application app = (Application) Reflect.invokeMethod(
                "android.app.LoadedApk", loadedApkInfo, "makeApplication",
                new Object[]{false, null},
                boolean.class, Instrumentation.class);

        Reflect.setFieldValue("android.app.ActivityThread", currentActivityThread, "mInitialApplication", app);
        return app;
    }
}
