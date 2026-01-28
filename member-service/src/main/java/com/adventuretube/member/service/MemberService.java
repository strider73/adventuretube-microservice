package com.adventuretube.member.service;

import com.adventuretube.member.model.dto.token.TokenDTO;
import com.adventuretube.member.model.entity.Member;
import com.adventuretube.member.model.entity.Token;
import com.adventuretube.member.model.mapper.MemberMapper;
import com.adventuretube.member.model.mapper.TokenMapper;
import com.adventuretube.member.repo.MemberRepository;
import com.adventuretube.member.repo.TokenRepository;
import com.adventuretube.member.exceptions.DuplicateException;
import com.adventuretube.member.exceptions.MemberNotFoundException;
import com.adventuretube.member.exceptions.code.MemberErrorCode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class MemberService {
    private final MemberRepository memberRepository;
    private final TokenRepository tokenRepository;
    private final MemberMapper memberMapper;
    private final TokenMapper tokenMapper;

    public Mono<Member> registerMember(Member member) {
        return memberRepository.findByEmail(member.getEmail())
                .flatMap(existing -> Mono.<Member>error(new DuplicateException(MemberErrorCode.USER_EMAIL_DUPLICATE)))
                .switchIfEmpty(Mono.defer(() -> {
                    // Handle @PrePersist manually - set ID and createAt
                    if (member.getId() == null) {
                        member.setId(UUID.randomUUID());
                    }
                    if (member.getCreateAt() == null) {
                        member.setCreateAt(LocalDateTime.now());
                    }
                    return memberRepository.save(member);
                }));
    }

    public Mono<Member> findEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    public Mono<Member> findMemberByEmailAndTokenCheck(String email) {
        return memberRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new RuntimeException("Member with email " + email + " is not exist")))
                .flatMap(member ->
                        tokenRepository.findAllValidTokenByMember(member.getId())
                                .hasElements()
                                .flatMap(hasTokens -> {
                                    if (hasTokens) {
                                        return Mono.just(member);
                                    } else {
                                        return Mono.error(new RuntimeException("Member with email " + email + " was logged out already"));
                                    }
                                })
                );
    }

    /**
     * Stores a new authentication token for a member.
     *
     * <p>This method is shared by multiple authentication processes such as:
     * <ul>
     *     <li>Login</li>
     *     <li>Refresh Token</li>
     * </ul>
     *
     * <p>Behavior:
     * <ul>
     *     <li>If {@code tokenDTO.memberDTO.id} is null, the method attempts to resolve the member
     *         by email (used during login where ID may not be available).</li>
     *     <li>If no matching member is found, an exception is thrown.</li>
     *     <li>Before saving, all existing valid tokens for the member are revoked (deleted).</li>
     * </ul>
     *
     * @param tokenDTO The token data including member information and token strings
     * @return {@code Mono<Boolean>} true if the token was successfully saved
     */
    public Mono<Boolean> storeToken(TokenDTO tokenDTO) {
        // Resolve member ID if missing (e.g., login scenario)
        Mono<TokenDTO> resolvedTokenDTO;
        if (tokenDTO.getMemberDTO().getId() == null) {
            resolvedTokenDTO = memberRepository.findByEmail(tokenDTO.getMemberDTO().getUsername())
                    .switchIfEmpty(Mono.error(new RuntimeException("User email " + tokenDTO.getMemberDTO().getEmail() + " is not a Member")))
                    .map(member -> {
                        tokenDTO.setMemberDTO(memberMapper.memberToMemberDTO(member));
                        return tokenDTO;
                    });
        } else {
            resolvedTokenDTO = Mono.just(tokenDTO);
        }

        return resolvedTokenDTO.flatMap(dto -> {
            // Convert TokenDTO to entity
            Token token = tokenMapper.tokenDTOToToken(dto);

            // Handle @PrePersist manually (R2DBC doesn't support JPA callbacks)
            if (token.getId() == null) {
                token.setId(UUID.randomUUID());
                token.setNew(true);  // Tell R2DBC this is INSERT, not UPDATE
            }
            if (token.getCreateAt() == null) {
                token.setCreateAt(LocalDateTime.now());
            }

            // Revoke all existing valid tokens before saving the new one
            return tokenRepository.findAllValidTokenByMember(dto.getMemberDTO().getId())
                    .flatMap(tokenRepository::delete)
                    .then(tokenRepository.save(token))
                    .thenReturn(true);
        });
    }

    public Mono<Token> findToken(String token) {
        return tokenRepository.findByRefreshToken(token);
    }

    public Mono<Boolean> deleteAllToken(String token) {
        return tokenRepository.deleteByAccessTokenOrRefreshToken(token)
                .map(deleteCount -> {
                    if (deleteCount > 0) {
                        log.debug("token delete successfully");
                        return true;
                    } else {
                        log.warn("Token deletion failed for access token: {}", token);
                        return false;
                    }
                });
    }

    public Mono<Boolean> deleteUser(String email) {
        return memberRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new MemberNotFoundException(MemberErrorCode.USER_NOT_FOUND)))
                .flatMap(member ->
                        tokenRepository.deleteByMemberId(member.getId())
                                .then(memberRepository.delete(member))
                                .thenReturn(true)
                );
    }
}
