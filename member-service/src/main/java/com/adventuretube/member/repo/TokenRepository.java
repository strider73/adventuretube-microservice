package com.adventuretube.member.repo;

import com.adventuretube.member.model.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenRepository extends JpaRepository<Token, UUID> {

    @Query("SELECT t FROM Token t WHERE t.memberId = :memberId AND (t.expired = false OR t.revoked = false)")
    List<Token> findAllValidTokenByMember(@Param("memberId") UUID memberId);

    @Query("SELECT t FROM Token t WHERE t.memberId = :memberId AND (t.accessToken = :token OR t.refreshToken = :token)")
    Optional<Token> findTokenByMemberIdAndToken(@Param("memberId") UUID memberId, @Param("token") String token);

    @Modifying
    @Query("DELETE FROM Token t WHERE t.accessToken = :token OR t.refreshToken = :token")
    int deleteByAccessTokenOrRefreshToken(@Param("token") String token);

    Optional<Token> findByAccessToken(String accessToken);

    Optional<Token> findByRefreshToken(String refreshToken);

    void deleteByMemberId(UUID memberId);
}
