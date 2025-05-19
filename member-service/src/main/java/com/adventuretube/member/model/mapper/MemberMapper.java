package com.adventuretube.member.model.mapper;


import com.adventuretube.member.model.entity.Member;
import com.adventuretube.member.model.dto.member.MemberDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MemberMapper {
    MemberMapper INSTANCE = Mappers.getMapper(MemberMapper.class);
    Member memberDTOtoMember(MemberDTO memberDTO);
    MemberDTO memberToMemberDTO(Member member);


}
