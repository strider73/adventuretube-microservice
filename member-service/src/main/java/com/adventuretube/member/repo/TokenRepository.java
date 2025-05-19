package com.adventuretube.member.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


import com.adventuretube.member.model.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface TokenRepository extends JpaRepository<Token, Integer> {

    @Query("select t from Token t inner join Member m on t.member.id = m.id where m.id = :id and (t.expired = false or t.revoked = false)")
    List<Token> findAllValidTokenByMember(UUID id);



    @Query("select t from Token t inner join Member m on t.member.id = m.id where m.id = :id and (t.accessToken = :token  or t.refreshToken = :token)")
    Optional<Token>  findTokensByToken(Long id , String token);


    @Modifying
    @Transactional
    @Query("delete from Token t where t.accessToken = :token or t.refreshToken = :token")
    int deleteAllTokenByAccessToken(String token);

    Optional<Token> findByAccessToken(String accessToken);

    Optional<Token> findByRefreshToken(String refreshToken);

    }