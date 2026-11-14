package archive.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * 密码工具：随机盐 + MD5(盐 + 密码)。
 * 与 DeviceDemo 的 PasswordUtil 完全兼容（同样的算法、同样的十六进制输出）。
 */
public final class PasswordUtil {

    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String newSalt() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return hex(bytes);
    }

    public static String hash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update((salt == null ? "" : salt).getBytes(StandardCharsets.UTF_8));
            md.update((password == null ? "" : password).getBytes(StandardCharsets.UTF_8));
            return hex(md.digest());
        } catch (Exception e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }

    public static boolean matches(String rawPassword, String salt, String storedHash) {
        if (rawPassword == null || salt == null || storedHash == null) {
            return false;
        }
        byte[] expected = storedHash.toLowerCase().getBytes(StandardCharsets.UTF_8);
        byte[] actual = hash(rawPassword, salt).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    private static String hex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            out[i * 2] = HEX[b >>> 4];
            out[i * 2 + 1] = HEX[b & 0x0F];
        }
        return new String(out);
    }
}
