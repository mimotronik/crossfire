package com.winterfell.common.message;

/**
 * @author winterfell
 */
public interface Option {

    /**
     * 其他 占位使用
     */
    byte other = -1;

    /**
     * 标识一个tcp的包是建立连接
     */
    byte connect = 1;

    /**
     * 标识一个tcp的包是发送数据
     */
    byte send = 2;

}
