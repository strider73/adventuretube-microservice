package com.adventuretube.auth;


import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WelcomeController {

    @GetMapping("/")
    public String welcome(WelcomeString welcomeString){
        welcomeString.serviceName = "index service!!!!";
        return welcomeString.getWelcomeString();
    }


    @GetMapping("/admin")
    public String welcomeForAdmin(WelcomeString welcomeString){
        welcomeString.serviceName = "admin";
        return welcomeString.getWelcomeString();
    }


    @Bean
    public WelcomeString welcomeString(){
        return new WelcomeString();
    }


    @Data
    class WelcomeString{
        String serviceName;

        public String getWelcomeString(){
           return  new String("Welcome to Spring "+serviceName+" service");
        }
    }
}
