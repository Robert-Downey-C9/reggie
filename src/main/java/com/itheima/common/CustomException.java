package com.itheima.common;

/**
 * 自定义用户异常类
 */
public class CustomException extends RuntimeException{
    public CustomException(String message){
        super(message);
    }
}
