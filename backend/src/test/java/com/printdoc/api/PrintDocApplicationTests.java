package com.printdoc.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties="spring.autoconfigure.exclude=org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration")
@org.springframework.test.context.ActiveProfiles("test")
class PrintDocApplicationTests {
    @Test void contextLoads() {}
}
