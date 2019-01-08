package com.xianglin.open.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HeartBeatController {

    @RequestMapping(method = {RequestMethod.GET}, value = "/stat/alive")
    @ResponseBody
    public String heartBeat() {
        return "ok";
    }
}