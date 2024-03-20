package com.itheima.common;

/**
 * 基于ThreadLocal类封装工具类,用户保存和获取当前用户id
 * ThreadLocal不是一个Thread，而是Thread的局部变量。
 * ThreadLocal为每一个线程提供单独一份存储空间，具有线程隔离的效果，只有在线程内才能获取对应的值，线程外则不能访问。
 */
public class BaseContext {
    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id){
        threadLocal.set(id);
    }

    public static Long getCurrentId(){
        return threadLocal.get();
    }

}
