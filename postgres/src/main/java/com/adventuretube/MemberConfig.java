package com.adventuretube;

import com.adventuretube.model.Member;
import com.adventuretube.repo.MemberRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class MemberConfig {


    @Bean
    CommandLineRunner commandLineRunner(MemberRepository repository){
       return args -> {
         Member chris = Member.builder()
                 .name("Yegun Lee")
                 .channeld("https://www.youtube.com/channel/UCMg4QJXtDH-VeoJvlEpfEYg")
                 .email("monekey@gmail.com")
                 .build();

           Member bella = Member.builder()
                           .name("Sunhee Mub")
                           .channeld("https://www.youtube.com/channel/UCMg4QJXtDH-VeoJvlEpfEYg")
                           .email("monekey@gmail.com")
                           .build();

           repository.saveAll(List.of(chris,bella));

       };

    }
}
