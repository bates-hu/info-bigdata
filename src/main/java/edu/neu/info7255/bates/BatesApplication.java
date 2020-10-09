package edu.neu.info7255.bates;

import javax.servlet.Filter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BatesApplication {

	public static void main(String[] args) {
		SpringApplication.run(BatesApplication.class, args);
	}

	@Bean
	public Filter filter(){
		ShallowEtagHeaderFilter filter = new ShallowEtagHeaderFilter();
		return filter;
	}

	@Bean
	public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
		FilterRegistrationBean<ShallowEtagHeaderFilter> filterRegistrationBean
				= new FilterRegistrationBean<>( new ShallowEtagHeaderFilter());
		filterRegistrationBean.addUrlPatterns("/*");
		filterRegistrationBean.setName("etagFilter");
		return filterRegistrationBean;
	}
}
