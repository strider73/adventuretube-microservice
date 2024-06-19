package com.adventuretube.security.model.dto;


import com.adventuretube.common.domain.dto.auth.AuthDTO;
import com.adventuretube.security.model.AuthRequest;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.springframework.security.core.userdetails.UserDetails;

@Mapper
public interface MemberMapper {
    MemberMapper INSTANCE = Mappers.getMapper(MemberMapper.class);
    AuthDTO authRequestToAuthDTO(AuthRequest authRequest);
    AuthDTO userDetailToAuthDTO(UserDetails userDetails);

}
