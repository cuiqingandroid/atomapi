package com.tencent.am

import android.util.Log
import com.tencent.cq.Helper
import com.tencent.cq.MethodCallback
import com.tencent.cq.At
import com.tencent.cq.MethodReplacement
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.util.*

/**
 * 封装访问各种find和hook的api
 */
object AtomApi {

    fun findClass(className: String, classLoader: ClassLoader?): Class<*> {
        return Helper.findClass(className, classLoader)
    }

    fun findMethodBestMatch(clazz: Class<*>, methodName: String, vararg parameters: Class<*>?): Method {
        return Helper.findMethodBestMatch(clazz, methodName, *parameters)
    }

    fun setObjectField(obj: Any?,fieldName: String?, value: Any?){
        Helper.setObjectField(obj, fieldName, value)
    }

    fun findClassIfExists(className: String, classLoader: ClassLoader?): Class<*>? {
        return Helper.findClassIfExists(className, classLoader)
    }

    fun findMethodExactIfExists(clz: Class<*>?, methodName: String, vararg objects: Any?): Method? {
        return Helper.findMethodExactIfExists(clz, methodName, *objects)
    }

    fun findConstructorExactIfExists(clz: Class<*>?, vararg objects: Any?): Constructor<*>? {
        return Helper.findConstructorExactIfExists(clz, *objects)
    }

    fun callStaticMethod(clazz: Class<*>?, methodName: String, vararg objects: Any?): Any? {
        return Helper.callStaticMethod(clazz, methodName, *objects)
    }

    fun callMethod(obj: Any?, methodName: String, vararg objects: Any?): Any? {
        return Helper.callMethod(obj, methodName, *objects)
    }

    fun findConstructorExact(className: String, classLoader: ClassLoader?, vararg objects: Any): Constructor<*> {
        return Helper.findConstructorExact(className, classLoader, *objects)
    }

    fun findConstructorExact(clazz: Class<*>, vararg objects: Any): Constructor<*> {
        return Helper.findConstructorExact(clazz, *objects)
    }

    fun getStaticObjectField(clazz: Class<*>, fieldName: String): Any? {
        return Helper.getStaticObjectField(clazz, fieldName)
    }

    fun newInstance(clazz: Class<*>, vararg objects: Any?): Any? {
        return Helper.newInstance(clazz, *objects)
    }

    fun getObjectField(obj: Any?, fieldName: String?): Any? {
        return Helper.getObjectField(obj, fieldName)
    }

    fun findField(clz: Class<*>?, fieldName: String?): Field? {
        return Helper.findField(clz, fieldName)
    }

    fun findMethodExact(clz: Class<*>?, methodName: String, vararg objects: Any?): Method {
        return Helper.findMethodExact(clz, methodName, *objects)
    }

    fun log(text: String) {
        At.log(text)
    }

    /**
     * hook 方法，轻易不要
     */
    fun hookMethod(hookMethod: Member?, callback: AtomCallback) {
        At.hookMethod(hookMethod, object : MethodCallback() {
            override fun bm(param: MethodHookParam?) {
                iB(param, callback)
            }

            override fun am(param: MethodHookParam?) {
                iA(param, callback)
            }
        })
    }

    /**
     * 查找并hook构造方法
     */
    fun findAndHookConstructor(clazz: Class<*>?, vararg objects: Any?) {
        val size =objects.size
        val callback = objects[size-1]
        if (callback is AtomCallback) {

            val cb = object : MethodCallback() {
                override fun bm(param: MethodHookParam?) {
                    iB(param, callback)
                }

                override fun am(param: MethodHookParam?) {
                    iA(param, callback)
                }
            }
            val params = arrayOfNulls<Any>(size)
            System.arraycopy(objects, 0, params, 0, size-1)
            params[size-1] = cb
            Log.d("robot","findAndHook params ${Arrays.toString(params)}")
            Helper.findAndHookConstructor(clazz, *params)
        }
    }

    /**
     * 查找并hook方法
     */
    fun findAndProcess(clazz: Class<*>?, methodName: String, vararg objects: Any?) {
        val size =objects.size
        val callback = objects[size-1]
        if (callback is AtomCallback) {
            val cb = object : MethodCallback() {
                override fun bm(param: MethodHookParam?) {
                    iB(param, callback)
                }

                override fun am(param: MethodHookParam?) {
                    iA(param, callback)
                }
            }
            val params = arrayOfNulls<Any>(size)
            System.arraycopy(objects, 0, params, 0, size-1)
            params[size-1] = cb
            Log.d("robot","findAndHook params ${Arrays.toString(params)}")
            Helper.findAndHookMethod(clazz, methodName, *params)
        } else if (callback is AtomReplaceCallback) {
            val cb = object : MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam?): Any? {
                    return iRp(param, callback)
                }

            }
            val params = arrayOfNulls<Any>(size)
            System.arraycopy(objects, 0, params, 0, size-1)
            params[size-1] = cb
            Log.d("robot","findAndHook params ${Arrays.toString(params)}")
            Helper.findAndHookMethod(clazz, methodName, *params)
        }
    }

    /**
     * 调用原始方法
     */
    fun invokeOriginal(member: Member, obj: Any?, args : Array<Any?>) : Any?{
        return At.invokeOriginalMethod(member, obj, args)
    }

    /**
     * 查找并hook方法
     */
    fun findAndProcess(className: String,classLoader: ClassLoader?, methodName: String, vararg objects: Any?) {
        val clz = findClass(className, classLoader)
        findAndProcess(clz, methodName, *objects)
    }

    fun iB(param: MethodCallback.MethodHookParam?, callback: AtomCallback) {
        val p = AtomMethodParam()
        if (param?.args != null) {
            p.args = param.args
        }
        p.method = param?.method
        p.thisObject = param?.thisObject
        p.result = param?.result
        p.throwable = param?.throwable
        callback.bm(p)
        if (p.result != null) {
            param?.result = p.result
        }
    }

    fun iA(param: MethodCallback.MethodHookParam?, callback: AtomCallback) {
        val p = AtomMethodParam()
        if (param?.args != null) {
            p.args = param.args
        }
        p.method = param?.method
        p.thisObject = param?.thisObject
        p.result = param?.result
        p.throwable = param?.throwable
        callback.am(p)
    }
    fun iRp(param: MethodCallback.MethodHookParam?, callback: AtomReplaceCallback): Any? {
        val p = AtomMethodParam()
        if (param?.args != null) {
            p.args = param.args
        }
        p.method = param?.method
        p.thisObject = param?.thisObject
        p.result = param?.result
        p.throwable = param?.throwable
        return callback.rm(p)
    }
}