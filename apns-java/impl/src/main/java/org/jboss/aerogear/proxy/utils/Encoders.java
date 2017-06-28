package org.jboss.aerogear.proxy.utils;

public class Encoders {

    private static final char base[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    public static String encodeHex(final byte[] bytes) {
        final char[] chars = new char[bytes.length * 2];

        for (int i = 0; i < bytes.length; ++i) {
            final int b = (bytes[i]) & 0xFF;
            chars[2 * i] = base[b >>> 4];
            chars[2 * i + 1] = base[b & 0xF];
        }

        return new String(chars);
    }
}
