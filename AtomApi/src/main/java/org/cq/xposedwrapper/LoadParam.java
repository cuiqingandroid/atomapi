package org.cq.xposedwrapper;
import android.content.pm.ApplicationInfo;

import com.tencent.cq.callbacks.LoadPackage;

public class LoadParam {

    /** The name of the package being loaded.  */
    public String packageName;

    /** The process in which the package is executed.  */
    public String processName;

    /** The ClassLoader used for this package.  */
    public ClassLoader classLoader;

    /** More information about the application being loaded.  */
    public ApplicationInfo appInfo;

    /** Set to `true` if this is the first (and main) application for this process.  */
    public boolean isFirstApplication;

    public LoadParam(LoadPackage.LoadPackageParam param){
        this.packageName = param.packageName;
        this.processName = param.processName;
        this.classLoader = param.classLoader;
        this.appInfo = param.appInfo;
        this.isFirstApplication = param.isFirstApplication;
    }
}
