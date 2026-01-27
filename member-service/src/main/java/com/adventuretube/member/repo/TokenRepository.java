package com.adventuretube.member.repo;

import com.adventuretube.member.model.entity.Token;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface TokenRepository extends ReactiveCrudRepository<Token, UUID> {

    // Native SQL - R2DBC doesn't support JPQL
    @Query("SELECT * FROM token WHERE member_id = :memberId AND (expired = false OR revoked = false)")
    Flux<Token> findAllValidTokenByMember(UUID memberId);

    @Query("SELECT * FROM token WHERE member_id = :memberId AND (access_token = :token OR refresh_token = :token)")
    Mono<Token> findTokenByMemberIdAndToken(UUID memberId, String token);

    // Delete and return count
    @Modifying
    @Query("DELETE FROM token WHERE access_token = :token OR refresh_token = :token")
    Mono<Integer> deleteByAccessTokenOrRefreshToken(String token);

    Mono<Token> findByAccessToken(String accessToken);

    Mono<Token> findByRefreshToken(String refreshToken);

    // Delete all tokens for a member
    Mono<Void> deleteByMemberId(UUID memberId);
}
