package com.tencent.am;

import android.content.pm.PackageInfo;
import android.util.Log;

import com.tencent.cq.At;
import com.tencent.cq.Helper;
import com.tencent.cq.callbacks.LoadPackage;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;

class Loader {
    private static final String TAG = "Atom";
    private static final String INSTANT_RUN_CLASS = "com.android.tools.fd.runtime.BootstrapApplication";

    public static void handleLoadPackage(LoadPackage.LoadPackageParam lpparam, String pluginPackageName) {
        loadModule(pluginPackageName, lpparam.classLoader, lpparam);
    }

    private static void closeSilently(DexFile dexFile) {
        if (dexFile != null) {
            try {
                dexFile.close();
            } catch (IOException ignored) {
            }

        }
    }

    /**
     * Load a module from an APK by calling the init(String) method for all classes defined
     * in `assets/xposed_init`.
     */
    private static void loadModule(String pluginPackageName, ClassLoader classLoader, LoadPackage.LoadPackageParam param) {

        Class classActivityThread = Helper.findClass("android.app.ActivityThread", classLoader);
        Object ipm = Helper.callStaticMethod(classActivityThread, "getPackageManager");
        PackageInfo packageInfo = (PackageInfo) Helper.callMethod(ipm, "getPackageInfo", pluginPackageName, 0, 0);
        String apk = packageInfo.applicationInfo.sourceDir;
        //loadModule(packageInfo.applicationInfo.sourceDir,ClassLoader.getSystemClassLoader())

        ClassLoader topClassLoader = ClassLoader.getSystemClassLoader();
        String moduleClassName = pluginPackageName+".xpapi.Loader";
        Log.i(TAG, "Loading modules from "+ apk);

        if (!new File(apk).exists()) {
            Log.e(TAG, "  File does not exist");
            return;
        }

        DexFile dexFile = null;
        try {
            dexFile = new DexFile(apk);
        } catch (IOException e) {
            Log.e(TAG, "  Cannot load module", e);
            return;
        }

        if (dexFile.loadClass(INSTANT_RUN_CLASS, topClassLoader) != null) {
            Log.e(TAG, "  Cannot load module, please disable \"Instant Run\" in Android Studio.");
            closeSilently(dexFile);
            return;
        }

        if (dexFile.loadClass(At.class.getName(), topClassLoader) != null) {
            Log.e(TAG, "  Cannot load module:");
            Log.e(TAG, "  The Xposed API classes are compiled into the module's APK.");
            Log.e(TAG, "  This may cause strange issues and must be fixed by the module developer.");
            Log.e(TAG, "  For details, see: http://api.xposed.info/using.html");
            closeSilently(dexFile);
            return;
        }

        closeSilently(dexFile);


        ClassLoader pluginClassloader = new PathClassLoader(apk, At.BOOTCLASSLOADER);

        try {
            Class<?> moduleClass = pluginClassloader.loadClass(moduleClassName);
            final Object moduleInstance = moduleClass.newInstance();
            Class<?> clzParam = pluginClassloader.loadClass("com.tencent.am.LoadParam");
            Object lpparam = clzParam.getConstructor(LoadPackage.LoadPackageParam.class).newInstance(param);

            Log.i(TAG, "  Loading plugin class entry "+ moduleClass.getName());
            try {
                Method method = moduleClass.getDeclaredMethod("handleLoadPackage", clzParam);
                method.setAccessible(true);
                method.invoke(moduleInstance, lpparam);
            } catch (Helper.ClassNotFoundError | NoSuchMethodError e) {
                Log.e(TAG, "This plugin should contain class " +pluginPackageName+".xpapi.Loader who implement LoadInterface");
            }

        } catch (Throwable t) {
            Log.e(TAG, "    Failed to load class "+moduleClassName, t);
        }

    }


}
