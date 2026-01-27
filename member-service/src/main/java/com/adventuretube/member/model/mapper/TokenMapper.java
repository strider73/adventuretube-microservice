package com.adventuretube.member.model.mapper;

import com.adventuretube.member.model.dto.token.TokenDTO;
import com.adventuretube.member.model.entity.Token;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TokenMapper {
    // Map memberDTO.id to memberId (R2DBC uses foreign key, not object reference)
    @Mapping(source = "memberDTO.id", target = "memberId")
    Token tokenDTOToToken(TokenDTO tokenDTO);
}
