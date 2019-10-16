package com.tencent.am;

import android.content.pm.PackageInfo;
import android.util.Log;

import com.tencent.cq.At;
import com.tencent.cq.Helper;
import com.tencent.cq.callbacks.LoadPackage;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;

class Loader {
    private static final String TAG = "atom";
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

        ClassLoader mcl = new PathClassLoader(apk, At.BOOTCLASSLOADER);

        try {
            Class moduleClass = mcl.loadClass(moduleClassName);
            final Object moduleInstance = moduleClass.newInstance();
            Object lpparam = Helper.findConstructorExact(Helper.findClass("com.tencent.am.LoadParam", mcl),
                    LoadPackage.LoadPackageParam.class).newInstance(param);

//            if (!(moduleInstance instanceof LoaderInterface)) {
//                Log.e(TAG, "This plugin should contain class " +pluginPackageName+".xpapi.Loader who implement LoadInterface");
//                return;
//            }
//            ((LoaderInterface)moduleInstance).handleLoadPackage(lpparam);
//            Log.e(TAG, "  Loading class lpclass "+ lpparam.getClass());
//            Log.e(TAG, "  Loading class "+ moduleClassName+" methods " + Arrays.toString(moduleClass.getDeclaredMethods()));
            Method entryMethod = Helper.findMethodExactIfExists(moduleClass, "handleLoadPackage", lpparam.getClass());
            if (entryMethod == null) {
                Log.e(TAG, "This plugin should contain class " +pluginPackageName+".xpapi.Loader who implement LoadInterface");
//                return;
            }

//            Object moduleInstance = moduleClass.newInstance();
            Helper.callMethod(moduleInstance, "handleLoadPackage", lpparam);

        } catch (Throwable t) {
            Log.e(TAG, "    Failed to load class "+moduleClassName, t);
        }

    }
}
