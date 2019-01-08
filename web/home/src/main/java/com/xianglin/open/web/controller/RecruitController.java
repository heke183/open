package com.xianglin.open.web.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xianglin.appserv.common.service.facade.app.PersonalService;
import com.xianglin.appserv.common.service.facade.app.RecruitService;
import com.xianglin.appserv.common.service.facade.app.UserGenealogyService;
import com.xianglin.appserv.common.service.facade.model.Response;
import com.xianglin.appserv.common.service.facade.model.vo.RecruitJobResumeVo;
import com.xianglin.appserv.common.service.facade.model.vo.RecruitJobVo;
import com.xianglin.appserv.common.service.facade.model.vo.RecruitResumeVo;
import com.xianglin.appserv.common.service.facade.model.vo.UserVo;
import com.xianglin.appserv.common.service.facade.model.vo.req.PageReq;
import com.xianglin.appserv.common.service.facade.req.RecruitJobReq;
import com.xianglin.open.shared.AppgwService;
import com.xianglin.open.shared.exception.OpenException;
import com.xianglin.open.shared.exception.ResultEnum;
import com.xianglin.open.util.AppSessionContext;
import com.xianglin.open.web.utils.JsonResult;
import com.xianglin.open.web.utils.ResponseUtil;
import io.lettuce.core.RedisClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.ShardedJedis;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Describe :
 * Created by xingyali on 2018/6/1 9:37.
 * Update reason :
 */
@RestController
@RequestMapping("/app/Recruit")
public class RecruitController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private RecruitService recruitService;

    @Autowired
    @Resource(name = "redisCache")
    private RedisTemplate<String,String> redisCache;

    @Autowired
    private AppgwService appgwService;

    /**
     * 招工列表
     */
    private final String REC = "RecruitJob";
    
    private final String REC_CACHE = "RecruitJobCache";

    private static int sysMinite = 0;

    /**
     * 查招工详情
     * @param 
     */
    @RequestMapping("/queryRecruitJobById")
    public JsonResult<RecruitJobVo> queryRecruitJobById(Long id) {
        return ResponseUtil.executeResponse(() -> {
//            Long partyId = AppSessionContext.ofPartyId(null);
//            if(partyId == null){
//                throw new OpenException("403","用户未登录");
//            }
            String type="RECRUIT";
            logger.info("queryRecruitJobById res:{}",id);
            //Response<RecruitJobVo> recruitJobVoResponse = recruitService.queryRecruitJobById(type, id);
            RecruitJobVo recruitJobVo = appgwService.service(RecruitService.class, "queryRecruitJobById", RecruitJobVo.class, type, id);
            logger.info("queryRecruitJobById resp:{}", StringUtils.substring(ToStringBuilder.reflectionToString(recruitJobVo),0,200));
             //throw new OpenException(ResultEnum.SUCCESS)
            return recruitJobVo;
        });
    }

    /**
     * 查招聘官方列表
     * @param
     */
    @RequestMapping("/queryOfficialRecruitJob")
    public JsonResult<List<RecruitJobVo>> queryOfficialRecruitJob(HttpServletResponse response,final long startPage,final long pageSize, final long lastId) {
        return ResponseUtil.executeResponse(() ->{
            logger.info("queryOfficialRecruitJob  rep:{},{},{}:",startPage,pageSize,lastId);
//            Long partyId = AppSessionContext.ofPartyId(null);
//            if(partyId == null){
//                throw new OpenException("403","用户未登录");
//            }
            if(redisCache.opsForList().size(REC)==0 || LocalDateTime.now().getMinute() != sysMinite){
                //redisCache.delete(REC);
                logger.info("queryOfficialRecruitJob old.lLen:"+redisCache.opsForList().size(REC));
                sysMinite = LocalDateTime.now().getMinute();
                RecruitJobReq recruitJobReq = new RecruitJobReq();
                recruitJobReq.setStartPage(1);
                List list = appgwService.serviceList(RecruitService.class, "queryRecruitJobList", List.class, recruitJobReq);
                //Response<List<RecruitJobVo>> listResponse = recruitService.queryRecruitJobList(recruitJobReq);
                if(list.size()>0){
                    for (int i=0;i<list.size();i++){
                        JSONObject jSONObject = (JSONObject) list.get(i);
                        if(jSONObject.get("officialSign").toString().equals("1")){
                            redisCache.opsForList().rightPush(REC_CACHE,JSON.toJSONString(jSONObject));   
                        }
                    }
                    /*listResponse.getResult().stream().forEach((recruitJobVo)->{
                        if(recruitJobVo.getOfficialSign()==1){
                            redisCache.opsForList().rightPush(REC_CACHE,JSON.toJSONString(recruitJobVo));
                        }
                    });*/
                }
                redisCache.rename(REC_CACHE,REC);
            }
            logger.info("queryOfficialRecruitJob new.lLen:"+redisCache.opsForList().size(REC));
            sysMinite = LocalDateTime.now().getMinute();
            long start=(startPage-1)*pageSize;
            long page=start+pageSize-1;
            return  redisCache.opsForList().range(REC, start, page).stream().map((vo)->
                //logger.info("vovovo:"+vo);
                 JSON.parseObject(vo,RecruitJobVo.class)
            ).filter((vo) ->(vo.getId() < lastId) || lastId == 0).collect(Collectors.toList());
        });
    }

    /**
     *查询个人简历
     * @param
     */
    @RequestMapping("/queryRecruitResume")
    public JsonResult<RecruitResumeVo> queryRecruitResume(HttpServletResponse response) {
        return ResponseUtil.executeResponse(() -> {
            Long partyId = AppSessionContext.ofPartyId(null);
            logger.info("partyId res:{}",partyId);
            if(partyId == null){
                throw new OpenException("403","用户未登录");
            }
            PageReq req= new PageReq();
            req.setPageSize(1);
            req.setStartPage(1);
            logger.info("queryRecruitResume res:{}");
            List list = appgwService.service(RecruitService.class, "queryRecruitResumeListByType", List.class, "PSERSONAL", req);
            logger.info("queryRecruitResume resp:{}", StringUtils.substring(ToStringBuilder.reflectionToString(list),0,200));
            RecruitResumeVo recruitResumeVo =null;
            if(list.size()>0){
                recruitResumeVo = JSONObject.parseObject(list.get(0).toString().getBytes(), RecruitResumeVo.class);
            }
            return recruitResumeVo;
        });
    }

    /**
     * 投递简历
     * @param
     */
    @RequestMapping("/publishRecruitJobResume")
    public JsonResult<Boolean> publishRecruitJobResume(HttpServletResponse response,Long jobId, Long resumeId) {
        return ResponseUtil.executeResponse(()->{
            Long partyId = AppSessionContext.ofPartyId(null);
            if(partyId == null){
                throw new OpenException("403","用户未登录");
            }
            logger.info("publishRecruitJobResume res:{} {} ",jobId,resumeId);
             Boolean flag = appgwService.service(RecruitService.class, "publishRecruitJobResume", Boolean.class, jobId, resumeId);

            logger.info("publishRecruitJobResume resp:{}", StringUtils.substring(ToStringBuilder.reflectionToString(flag),0,200));
            return flag;
        });
    }

    /**
     * 发布个人简历（支持修改简历）
     * @param
     */
    @RequestMapping("/publishRecruitResume")
    public JsonResult<RecruitResumeVo> publishRecruitResume(HttpServletResponse response,RecruitResumeVo recruitResumeVo) {
        return ResponseUtil.executeResponse(()->{
            Long partyId = AppSessionContext.ofPartyId(null);
            if(partyId == null){
                throw new OpenException("403","用户未登录");
            }
            logger.info("publishRecruitResume res:{} {} ",recruitResumeVo.toString());
            recruitResumeVo.setType("PSERSONAL");
            RecruitResumeVo recruitResume = appgwService.service(RecruitService.class, "publishRecruitResume", RecruitResumeVo.class, recruitResumeVo);
            logger.info("publishRecruitResume resp:{}", StringUtils.substring(ToStringBuilder.reflectionToString(recruitResume),0,200));
            return recruitResume;
        });
    }
}
