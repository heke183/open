package com.xianglin.open.web.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.xianglin.appserv.common.service.facade.app.PersonalService;
import com.xianglin.appserv.common.service.facade.app.RecruitService;
import com.xianglin.appserv.common.service.facade.app.SystemParaService;
import com.xianglin.appserv.common.service.facade.model.Response;
import com.xianglin.appserv.common.service.facade.model.vo.DistrictVo;
import com.xianglin.appserv.common.service.facade.model.vo.RecruitJobVo;
import com.xianglin.appserv.common.service.facade.model.vo.SysParaVo;
import com.xianglin.appserv.common.service.facade.model.vo.req.PageReq;
import com.xianglin.open.shared.exception.OpenException;
import com.xianglin.open.web.utils.JsonResult;
import com.xianglin.open.web.utils.ResponseUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Describe :
 * Created by xingyali on 2018/6/1 14:35.
 * Update reason :
 */
@RestController
@RequestMapping("/app/currency")
public class CurrencyController {
    
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private SystemParaService appSysParaService;
    
    @Reference
    private PersonalService personalService;


    /**
     * 系统参数查询
     * @param
     */
    @RequestMapping("/queryPara")
    public JsonResult<String> queryPara(String paraName) {
        return ResponseUtil.executeResponse(()->{
            logger.info("queryPara res:{}",paraName);
            Response<SysParaVo> resp = appSysParaService.queryPara(paraName);
            logger.info("queryPara resp:{}",StringUtils.substring(resp.toString(),0,200));
            if(!resp.isSuccess()){
                throw new OpenException(resp.getCode()+"",resp.getTips());
            }
            return resp.getResult().getValue();
        });
    }

    /**
     * 级联区域查询
     * @param
     */
    @RequestMapping("/queryDistrictList")
    public JsonResult<List<DistrictVo>> queryDistrictList(String paraCode) {
        return ResponseUtil.executeResponse(()->{
            logger.info("queryDistrictList paraName:{}", paraCode);
            Response<List<DistrictVo>> listResponse = personalService.queryDistrictList(paraCode);
            logger.info("queryDistrictList resp:{}",StringUtils.substring(listResponse.toString(),0,200));
            if(!listResponse.isSuccess()){
              throw new OpenException(listResponse.getCode()+"",listResponse.getTips());  
            }
            return listResponse.getResult();
        });
    }
    
    
    
}
