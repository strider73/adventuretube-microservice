package com.adventuretube.member.model.mapper;


import com.adventuretube.member.model.entity.Member;
import com.adventuretube.member.model.dto.member.MemberDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface MemberMapper {
    Member memberDTOtoMember(MemberDTO memberDTO);
    MemberDTO memberToMemberDTO(Member member);


}
