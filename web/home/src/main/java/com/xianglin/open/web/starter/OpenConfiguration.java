package com.xianglin.open.web.starter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.xianglin.common.filter.MDCFilter;

@Configuration
@ServletComponentScan(basePackages = {"com.xianglin.open.web.filter"})
public class OpenConfiguration {

	@Bean
	public FilterRegistrationBean<MDCFilter> mdcFilterFilterRegistrationBean() {

		FilterRegistrationBean<MDCFilter> registrationBean = new FilterRegistrationBean<>();
		registrationBean.setFilter(new MDCFilter());
		registrationBean.addUrlPatterns("/*");
		registrationBean.setName("mdcFilter");
		registrationBean.setOrder(1);
		return registrationBean;
	}

}
