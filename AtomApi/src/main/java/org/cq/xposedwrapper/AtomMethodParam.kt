package org.cq.xposedwrapper

import java.lang.reflect.Member

/**
 * Created by shixiangyu on 2019-07-03.
 */
class AtomMethodParam {

    /** The hooked method/constructor.  */
    var method: Member? = null

    /** The `this` reference for an instance method, or `null` for static methods.  */
    var thisObject: Any? = null

    /** Arguments to the method call.  */
    var args: Array<Any?>? = null

    var result: Any? = null
    var throwable: Throwable? = null
}