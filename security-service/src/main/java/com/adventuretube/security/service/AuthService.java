package com.adventuretube.security.service;

import com.adventuretube.common.domain.requestmodel.AuthRequest;
import com.adventuretube.common.domain.requestmodel.AuthResponse;
import com.adventuretube.common.domain.dto.UserDTO;
import lombok.AllArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@AllArgsConstructor
public class AuthService {

    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;


     public AuthResponse register(AuthRequest request){
        request.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));

//        String urlForEmailCheck = "http://MEMBER-SERVICE/member/emailCheck";
//        UserDTO emailCheckedUser = restTemplate.postForObject(urlForEmailCheck, request, UserDTO.class);


        String urlForRegister = "http://MEMBER-SERVICE/member/registerMember"; //with Eureka
        UserDTO registeredUser = restTemplate.postForObject(urlForRegister, request, UserDTO.class);


        String accessToken = jwtUtil.generate(registeredUser.getId().toString(), registeredUser.getRole(), "ACCESS");
        String refreshToken = jwtUtil.generate(registeredUser.getId().toString(), registeredUser.getRole(), "REFRESH");

        return new AuthResponse(accessToken, refreshToken);
    }
}
