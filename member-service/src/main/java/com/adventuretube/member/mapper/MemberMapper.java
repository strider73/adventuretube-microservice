package com.adventuretube.member.mapper;


import com.adventuretube.common.domain.dto.member.Member;
import com.adventuretube.common.domain.dto.member.MemberDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MemberMapper {
    MemberMapper INSTANCE = Mappers.getMapper(MemberMapper.class);
    Member memberDTOtoMember(MemberDTO memberDTO);
    MemberDTO memberToMemberDTO(Member member);


}
