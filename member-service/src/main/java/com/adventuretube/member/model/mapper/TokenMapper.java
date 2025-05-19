package com.adventuretube.member.model.mapper;




import com.adventuretube.member.model.dto.token.TokenDTO;
import com.adventuretube.member.model.entity.Token;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = MemberMapper.class)
public interface TokenMapper {
    TokenMapper INSTANCE = Mappers.getMapper(TokenMapper.class);
    @Mapping(source = "memberDTO", target = "member")
    Token tokenDTOToToken(TokenDTO tokenDTO);
}
