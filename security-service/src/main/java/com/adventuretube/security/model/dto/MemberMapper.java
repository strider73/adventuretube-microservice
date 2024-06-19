package com.adventuretube.security.model.dto;


import com.adventuretube.common.domain.dto.auth.MemberDTO;
import com.adventuretube.security.model.MemberRegisterRequest;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.springframework.security.core.userdetails.UserDetails;

@Mapper
public interface MemberMapper {
    MemberMapper INSTANCE = Mappers.getMapper(MemberMapper.class);
    MemberDTO authRequestToMemberDTO(MemberRegisterRequest memberRegisterRequest);
    MemberDTO userDetailToMemberDTO(UserDetails userDetails);

}
