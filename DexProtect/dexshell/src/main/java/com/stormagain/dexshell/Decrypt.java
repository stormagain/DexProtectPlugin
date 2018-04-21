package com.stormagain.dexshell;

/**
 * Created by liujian on 2018/4/12.
 */

public class Decrypt {

    public static byte[] decrypt(byte[] src) {
        for (int i = 0; i < src.length; i++) {
            src[i] = (byte) (src[i] ^ 3);
        }
        return src;
    }

}
