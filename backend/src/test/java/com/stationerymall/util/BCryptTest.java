package com.stationerymall.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * BCrypt 加密调试测试（仅本地调试使用，不参与业务逻辑）。
 * <p>
 * 目的：直接调用项目加密工具 {@link BCryptUtil}，把明文与密文打印到控制台，
 * 便于对照 {@code db/init-data.sql} 中的预置账号密码、排查登录失败问题。
 * <p>
 * 运行方式：
 * <pre>
 *   cd backend
 *   mvn -Dtest=BCryptTest test
 * </pre>
 * 说明：BCrypt 每次使用随机盐，同一明文多次加密结果不同，属正常现象；
 * 密文固定为 60 位（$2a$10$...），与 at.favre.lib:bcrypt strength=10 对应。
 */
class BCryptTest {

    /** 分隔线，便于控制台阅读 */
    private static final String LINE = "==================================================";

    /**
     * 打印单个明文的加密结果与回验结果。
     *
     * @param plain 明文密码
     */
    private void printEncryptResult(String plain) {
        String cipher = BCryptUtil.encode(plain);

        System.out.println(LINE);
        System.out.println("明文      : " + plain);
        System.out.println("密文      : " + cipher);
        System.out.println("密文长度  : " + cipher.length());
        System.out.println("回验(正确): " + BCryptUtil.matches(plain, cipher));
        System.out.println("回验(错误): " + BCryptUtil.matches(plain + "_wrong", cipher));
        System.out.println(LINE);
    }

    @Test
    @DisplayName("普通用户密码 12345678 加密并打印")
    void encryptNormalUserPassword() {
        printEncryptResult("12345678");
    }

    @Test
    @DisplayName("管理员密码 admin123 加密并打印")
    void encryptAdminPassword() {
        printEncryptResult("admin123");
    }

    @Test
    @DisplayName("同一明文两次加密结果不同（随机盐），但均可回验")
    void encryptTwiceWithRandomSalt() {
        String first = BCryptUtil.encode("123456");
        String second = BCryptUtil.encode("123456");

        System.out.println(LINE);
        System.out.println("第一次密文: " + first);
        System.out.println("第二次密文: " + second);
        System.out.println("是否相同  : " + (first.equals(second) ? "相同" : "不同（正常，盐随机）"));
        System.out.println("两次均可回验: "
                + (BCryptUtil.matches("123456", first) && BCryptUtil.matches("123456", second)));
        System.out.println(LINE);
    }

    @Test
    @DisplayName("校验 init-data.sql 中预置的密文是否可用")
    void verifyPresetHashFromInitSql() {
        // 与 db/init-data.sql 中 alice / bob 的密文一致
        String presetHash = "$2a$10$o.sqWzKrClKdZmiU/.etvumtBeCY.m0mn7T1HImy/0W73UNYwWOZi";

        System.out.println(LINE);
        System.out.println("预置密文      : " + presetHash);
        System.out.println("123456 是否匹配: " + BCryptUtil.matches("123456", presetHash));
        System.out.println(LINE);
    }
}
