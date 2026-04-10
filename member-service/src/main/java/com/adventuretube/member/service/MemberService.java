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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class MemberService {
    private final MemberRepository memberRepository;
    private final TokenRepository tokenRepository;
    private final MemberMapper memberMapper;
    private final TokenMapper tokenMapper;

    public Member registerMember(Member member) {
        Optional<Member> existing = memberRepository.findByEmail(member.getEmail());
        if (existing.isPresent()) {
            log.warn("Duplicate email registration attempt: {}", member.getEmail());
            throw new DuplicateException(MemberErrorCode.USER_EMAIL_DUPLICATE);
        }
        return memberRepository.save(member);
    }

    public Optional<Member> findEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    public Member findMemberByEmailAndTokenCheck(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Member with email " + email + " is not exist"));

        List<Token> validTokens = tokenRepository.findAllValidTokenByMember(member.getId());
        if (validTokens.isEmpty()) {
            throw new RuntimeException("Member with email " + email + " was logged out already");
        }
        return member;
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
     * @return true if the token was successfully saved
     */
    @Transactional
    public boolean storeToken(TokenDTO tokenDTO) {
        log.info(">>> storeToken called for member: {}", tokenDTO.getMemberDTO().getEmail());

        // Resolve member ID if missing (e.g., login scenario)
        if (tokenDTO.getMemberDTO().getId() == null) {
            Member member = memberRepository.findByEmail(tokenDTO.getMemberDTO().getUsername())
                    .orElseThrow(() -> new RuntimeException("User email " + tokenDTO.getMemberDTO().getEmail() + " is not a Member"));
            tokenDTO.setMemberDTO(memberMapper.memberToMemberDTO(member));
        }

        // Convert TokenDTO to entity
        Token token = tokenMapper.tokenDTOToToken(tokenDTO);

        // Revoke all existing valid tokens before saving the new one
        log.info(">>> storeToken: revoking old tokens and saving new token for memberId: {}", tokenDTO.getMemberDTO().getId());
        List<Token> existingTokens = tokenRepository.findAllValidTokenByMember(tokenDTO.getMemberDTO().getId());
        tokenRepository.deleteAll(existingTokens);

        tokenRepository.save(token);
        log.info(">>> storeToken: token saved successfully for memberId: {}", tokenDTO.getMemberDTO().getId());
        return true;
    }

    public Optional<Token> findToken(String token) {
        return tokenRepository.findByRefreshToken(token);
    }

    @Transactional
    public boolean deleteAllToken(String token) {
        int deleteCount = tokenRepository.deleteByAccessTokenOrRefreshToken(token);
        if (deleteCount > 0) {
            log.debug("token delete successfully");
            return true;
        } else {
            log.warn("Token deletion failed for access token: {}", token);
            return false;
        }
    }

    @Transactional
    public boolean deleteUser(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Member not found for deletion: {}", email);
                    return new MemberNotFoundException(MemberErrorCode.USER_NOT_FOUND);
                });
        tokenRepository.deleteByMemberId(member.getId());
        memberRepository.delete(member);
        return true;
    }
}
