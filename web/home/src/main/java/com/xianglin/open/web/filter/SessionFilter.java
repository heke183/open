package com.xianglin.open.web.filter;

import com.xianglin.open.util.AppSessionContext;
import com.xianglin.open.web.utils.WebUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * appSession过滤器，只针对需要session才能访问的业务
 */
@WebFilter(filterName = "sessionFilter", urlPatterns = {"/app/Recruit/*","/oauth/code"})
public class SessionFilter implements Filter {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String sessionCookieName = "XLSESSIONID";

    private String sessionCookieDomain = "xianglin.cn";

    private String sessionCookiePath = "/";

    @Resource(name = "redisSession")
    private RedisTemplate<String, String> redisSession;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        Thread.currentThread().setName(UUID.randomUUID().toString());
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        logger.info("local request:{}", request.getQueryString());
        String sessionId = WebUtil.getCookie(request, sessionCookieName);
        ininSession(sessionId);
        filterChain.doFilter(servletRequest, servletResponse);
        AppSessionContext.removeSession();
    }

    /**初始化session
     * @param sessionId
     * @return
     */
    private boolean ininSession(String sessionId) {
        try {
            Map<String, String> map = redisSession.<String, String>opsForHash().entries(AppSessionContext.SESSION_PRIFIX + sessionId);
            if (!CollectionUtils.isEmpty(map)) {
                map.put(AppSessionContext.SESSION_ID, sessionId);
                AppSessionContext.putSessionInfo(map);
                return true;
            }
        } catch (Exception e) {
            logger.warn("get session fail", e);
        }
        return false;
    }

    /**校验用户登陆
     * @return
     */
    private boolean checkLogin() {
        return AppSessionContext.ofPartyId(null) != null;
    }


    @Override
    public void destroy() {

    }
}
