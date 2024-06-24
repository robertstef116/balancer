package com.robert

import org.slf4j.Logger
import org.slf4j.LoggerFactory


fun <R : Any> R.logger(): Lazy<Logger> {
    return lazy {LoggerFactory.getLogger(unwrapCompanionClass(this.javaClass).name) }
}

fun <T : Any> unwrapCompanionClass(ofClass: Class<T>): Class<*> {
    return if (ofClass.kotlin.isCompanion && ofClass.enclosingClass != null) {
        ofClass.enclosingClass
    } else {
        ofClass
    }
}

//fun <T : Any> unwrapCompanionClass(ofClass: Class<T>): Class<*> {
//    return if (ofClass.enclosingClass != null &&
//        ofClass.enclosingClass.kotlin.objectInstance?.javaClass == ofClass) {
//        ofClass.enclosingClass
//    } else {
//        ofClass
//    }
//}
