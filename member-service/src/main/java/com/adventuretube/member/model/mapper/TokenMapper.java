package com.adventuretube.member.model.mapper;




import com.adventuretube.member.model.dto.token.TokenDTO;
import com.adventuretube.member.model.entity.Token;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", uses = MemberMapper.class)
public interface TokenMapper {
    @Mapping(source = "memberDTO", target = "member")
    Token tokenDTOToToken(TokenDTO tokenDTO);
}
