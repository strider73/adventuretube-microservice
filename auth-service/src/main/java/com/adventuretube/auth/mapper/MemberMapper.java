package com.adventuretube.auth.mapper;


import com.adventuretube.auth.model.MemberRegisterRequest;
import com.adventuretube.common.domain.dto.member.MemberDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.springframework.security.core.userdetails.UserDetails;

@Mapper
public interface MemberMapper {
    MemberMapper INSTANCE = Mappers.getMapper(MemberMapper.class);
    MemberDTO memberRegisterRequestToMemberDTO(MemberRegisterRequest memberRegisterRequest);
    MemberDTO userDetailToMemberDTO(UserDetails userDetails);

}
