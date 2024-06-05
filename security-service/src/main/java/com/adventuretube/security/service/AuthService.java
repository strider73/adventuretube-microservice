package com.adventuretube.security.service;

import com.adventuretube.security.exceptions.DuplicateException;
import com.adventuretube.security.model.AuthRequest;
import com.adventuretube.security.model.AuthResponse;
import com.adventuretube.common.domain.dto.UserDTO;
import com.fasterxml.jackson.databind.util.BeanUtil;
import lombok.AllArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@AllArgsConstructor
public class AuthService {

    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;


     public AuthResponse register(AuthRequest request){
         request.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));

         UserDTO userDTO = new UserDTO();
         BeanUtils.copyProperties(request,userDTO);


        String urlForEmailCheck = "http://MEMBER-SERVICE/member/emailDuplicationCheck";
        Boolean isUserAlreadyExist  = restTemplate.postForObject(urlForEmailCheck,userDTO.getEmail(),Boolean.class);

        if(isUserAlreadyExist){
            throw new DuplicateException(String.format("User with the email address '%s' already exists.", request.getEmail()));
        }

        String urlForRegister = "http://MEMBER-SERVICE/member/registerMember"; //with Eureka
        UserDTO registeredUser = restTemplate.postForObject(urlForRegister, userDTO, UserDTO.class);


        String accessToken = jwtUtil.generate(registeredUser.getId().toString(), registeredUser.getRole(), "ACCESS");
        String refreshToken = jwtUtil.generate(registeredUser.getId().toString(), registeredUser.getRole(), "REFRESH");

        return new AuthResponse(accessToken, refreshToken);
    }
}
