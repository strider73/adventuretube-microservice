package com.adventuretube.security.service;

import com.adventuretube.security.exceptions.DuplicateException;
import com.adventuretube.security.model.AuthRequest;
import com.adventuretube.security.model.AuthResponse;
import com.adventuretube.common.domain.dto.UserDTO;
import lombok.AllArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;


import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;

@Service
@Transactional(readOnly = true)
@AllArgsConstructor
public class AuthService {

    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;


     @Transactional
     public AuthResponse register(AuthRequest request) throws Exception {
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
        if(registeredUser.getErrorMessage() ==null&& registeredUser.getE()== null) {

            String accessToken = jwtUtil.generate(registeredUser.getEmail().toString(), registeredUser.getRole(), "ACCESS");
            String refreshToken = jwtUtil.generate(registeredUser.getEmail().toString(), registeredUser.getRole(), "REFRESH");

            return new AuthResponse(accessToken, refreshToken);
        }else{
            String errorMessage = registeredUser.getErrorMessage();
            throw registeredUser.getE();
        }
    }


    public  AuthResponse getToken(UserDetails userDetails){

        String accessToken = jwtUtil.generate(userDetails.getUsername(),userDetails.getAuthorities().toString(), "ACCESS");
        String refreshToken = jwtUtil.generate(userDetails.getUsername(),userDetails.getAuthorities().toString(), "REFRESH");


        return new AuthResponse(accessToken, refreshToken);
     }
}
