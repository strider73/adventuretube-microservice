package com.adventuretube.web;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebServiceConfig {

    @Bean
    // after create eureka server without @LoadBalanced  annotation  restTemplate call
    // will get the error of unknown host !!!!
    //end if you have more than one instance to call the request will be loadbalanced
    //and able to check from the log
    @LoadBalanced

    public RestTemplate restTemplate(){
        return  new RestTemplate();
    }
}
