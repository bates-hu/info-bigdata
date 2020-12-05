package edu.neu.info7255.bates;

import javax.servlet.Filter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import edu.neu.info7255.bates.service.DocConsumer;

@SpringBootApplication
public class BatesApplication {

	public static void main(String[] args) {

		SpringApplication.run(BatesApplication.class, args);
		Thread consumer = new DocConsumer();
		consumer.start();
	}

}
