package com.xianglin.open.web.starter;

import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@EnableDubboConfiguration
@ComponentScan(basePackages={"com.xianglin.open"})
public class OpenWebHomeApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpenWebHomeApplication.class, args);
	}
}
