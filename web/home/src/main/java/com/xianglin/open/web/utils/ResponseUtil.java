package com.xianglin.open.web.utils;

import com.xianglin.open.shared.exception.OpenException;
import com.xianglin.open.shared.exception.ResultEnum;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponseUtil {

    private final static Logger logger = LoggerFactory.getLogger(ResponseUtil.class);

    /**
     * 业务执行返回
     *
     * @param exec
     * @param <T>
     * @return
     */
    public static <T> JsonResult<T> executeResponse(BusiResponse<T> exec) {
        JsonResult<T> resp = new JsonResult<>();
        try {
            T t = exec.execute();
            if (t instanceof JsonResult) {
                resp = (JsonResult<T>) t;
            } else {
                resp.setContent(t);
            }
        } catch (OpenException e) {
            logger.warn("OpenException", e);
            resp.setResult(e.getCode(),e.getMessage());
        } catch (Exception e) {
            logger.warn("", e);
            resp.setResult(ResultEnum.SUCCESS);
        }
        logger.info("resp:{}",StringUtils.substring(ToStringBuilder.reflectionToString(resp),0,500));
        return resp;
    }

    /**
     * 业务执行方法
     *
     * @param <T>
     */
    @FunctionalInterface
    public interface BusiResponse<T> {

        T execute() throws Exception;
    }
}



