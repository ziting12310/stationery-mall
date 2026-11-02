package com.stationerymall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 文具商城 MVP 启动类。
 * <p>
 * 根包 {@code com.stationerymall}，组件扫描范围覆盖 config / enums / exception / util，
 * 后续阶段的 entity / dto / vo / mapper / service / controller 也统一放在该根包下。
 * <p>
 * {@code @MapperScan} 指定 Mapper 接口包；本阶段尚未创建 mapper 包，启动时 MyBatis
 * 会打印 "No MyBatis mapper was found" 警告，属预期现象，后续阶段补齐即可。
 */
@SpringBootApplication
@MapperScan("com.stationerymall.mapper")
public class StationeryMallApplication {

    public static void main(String[] args) {
        SpringApplication.run(StationeryMallApplication.class, args);
    }
}
