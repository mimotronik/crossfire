package com.winterfell.common.utils;

import java.util.Random;

/**
 * @author winterfell
 */
public class RandomUtils {

    private static final Random r = new Random();

    /**
     * 获取随机的int
     *
     * @return
     */
    public static int getRandomInt() {
        boolean zf = r.nextBoolean();
        int randomInt = r.nextInt(Integer.MAX_VALUE - 1);
        return zf ? randomInt : -randomInt;
    }


    /**
     * 获取一个正数
     *
     * @return
     */
    public static int getPositiveInt() {
        return r.nextInt(Integer.MAX_VALUE);
    }

    /**
     * 获取一个负数
     */
    public static int getNegativeInt() {
        return -r.nextInt(Integer.MAX_VALUE);
    }

}
