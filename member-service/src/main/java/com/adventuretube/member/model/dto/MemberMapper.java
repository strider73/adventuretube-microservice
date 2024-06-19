package com.adventuretube.member.model.dto;


import com.adventuretube.common.domain.dto.auth.MemberDTO;
import com.adventuretube.member.model.Member;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MemberMapper {
    MemberMapper INSTANCE = Mappers.getMapper(MemberMapper.class);
    Member memberDTOtoMember(MemberDTO memberDTO);


}
