package com.adventuretube.auth.model.mapper;


import com.adventuretube.auth.model.request.MemberRegisterRequest;
import com.adventuretube.auth.model.dto.member.MemberDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.springframework.security.core.userdetails.UserDetails;
@Mapper(componentModel = "spring")
public interface MemberMapper {
    MemberDTO memberRegisterRequestToMemberDTO(MemberRegisterRequest memberRegisterRequest);
    MemberDTO userDetailToMemberDTO(UserDetails userDetails);

}
