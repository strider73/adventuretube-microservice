package com.adventuretube.security.model.dto;


import com.adventuretube.common.domain.dto.auth.AuthDTO;
import com.adventuretube.security.model.AuthRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.security.core.userdetails.UserDetails;

@Mapper(componentModel = "spring")
public interface MemberMapper {
    MemberMapper INSTANCE = Mappers.getMapper(MemberMapper.class);
    AuthDTO   AuthRequestToAuthDTO(AuthRequest authRequest);
    AuthDTO   UserDetailToAuthDTO(UserDetails userDetails);

}
