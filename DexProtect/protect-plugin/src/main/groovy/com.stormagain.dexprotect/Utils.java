package com.stormagain.dexprotect;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by liujian on 2018/4/20.
 */

public class Utils {

    public static void makeEncryptDex(String originDexPath, String targetDexPath) throws Exception {
        File originDex = new File(originDexPath);
        byte[] encrypt = encrypt(readFileBytes(originDex));
        File targetDex = new File(targetDexPath);
        if (targetDex.exists()) {
            targetDex.delete();
        }
        targetDex.createNewFile();

        FileOutputStream ops = new FileOutputStream(targetDex);
        ops.write(encrypt);
        ops.flush();
        ops.close();
    }

    public static void copyFile(File fromFile, File toFile) throws IOException {
        FileInputStream ins = new FileInputStream(fromFile);
        FileOutputStream out = new FileOutputStream(toFile);
        byte[] b = new byte[1024];
        int n = 0;
        while ((n = ins.read(b)) != -1) {
            out.write(b, 0, n);
        }
        ins.close();
        out.close();
    }

    public static byte[] encrypt(byte[] src) {
        for (int i = 0; i < src.length; i++) {
            src[i] = (byte) (src[i] ^ 3);
        }
        return src;
    }

    public static byte[] readFileBytes(File file) throws IOException {
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        FileInputStream fis = new FileInputStream(file);
        try {
            while (true) {
                int i = fis.read(buffer);
                if (i != -1) {
                    os.write(buffer, 0, i);
                } else {
                    return os.toByteArray();
                }
            }
        } catch (IOException e) {
            throw e;
        } finally {
            fis.close();
            os.close();
        }
    }
}
