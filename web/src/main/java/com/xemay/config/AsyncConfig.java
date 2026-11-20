package com.xemay.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bật hai cơ chế đa luồng của Spring:
 * <ul>
 *   <li>{@code @EnableAsync}: phương thức gắn {@code @Async} chạy trên luồng nền, tên luồng {@code xemay-nen-N}.</li>
 *   <li>{@code @EnableScheduling}: phương thức gắn {@code @Scheduled} chạy định kỳ, tên luồng {@code xemay-dinh-ky-N}.</li>
 * </ul>
 * Số luồng và tên luồng được cấu hình trong {@code application.properties} (nhóm {@code spring.task.*}).
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
}
