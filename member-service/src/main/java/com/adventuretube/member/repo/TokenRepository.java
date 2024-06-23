package com.adventuretube.member.repo;

import java.util.List;
import java.util.Optional;

import com.adventuretube.common.domain.dto.token.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TokenRepository extends JpaRepository<Token, Integer> {

    @Query("select t from Token t inner join Member m on t.member.id = m.id where m.id = :id and (t.expired = false or t.revoked = false)")
    List<Token> findAllValidTokenByUser(Long id);



    @Query("select t from Token t inner join Member m on t.member.id = m.id where m.id = :id and (t.accessToken = :token  or t.refreshToken = :token)")
    Optional<Token>  findTokensByToken(Long id , String token);

    Optional<Token> findByAccessToken(String accessToken);

    Optional<Token> findByRefreshToken(String refreshToken);

    }