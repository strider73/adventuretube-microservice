package com.adventuretube.member.mapper;


import com.adventuretube.common.domain.dto.token.Token;
import com.adventuretube.common.domain.dto.token.TokenDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = MemberMapper.class)
public interface TokenMapper {
    TokenMapper INSTANCE = Mappers.getMapper(TokenMapper.class);
    @Mapping(source = "memberDTO", target = "member")
    Token tokenDTOToToken(TokenDTO tokenDTO);
}
