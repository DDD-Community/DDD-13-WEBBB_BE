package com.ddd.webbb;

import com.ddd.webbb.config.TestRedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestRedisConfig.class)
class WebbbApplicationTests {

    @Test
    void contextLoads() {}
}
