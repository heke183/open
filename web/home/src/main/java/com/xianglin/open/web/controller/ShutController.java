package com.xianglin.open.web.controller;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
public class ShutController implements ApplicationContextAware {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private ApplicationContext ctx;

    @Value("${shutdown.user.name}")
    private String user;

    @Value("${shutdown.user.password}")
    private String password;

    @Value("${shutdown.user.server}")
    private String server;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
    }

    @RequestMapping("/shutdownContext")
    public String shutdownContext(String u, String p,HttpServletRequest request) {
        logger.info("begin to shutdown server ================================== {},{},{},{},{},{}", user, password,server,u, p,request.getServerName());
        if (StringUtils.equals(user, u) && StringUtils.equals(password, p) && StringUtils.equals(server,request.getServerName())) {
            ((ConfigurableApplicationContext) ctx).close();
            logger.info("end to shutdown server");
            return "context shutdown over";
        } else {
            return "context shutdown error";

        }
    }

}
