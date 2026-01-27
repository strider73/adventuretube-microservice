package com.adventuretube.member.repo;

import com.adventuretube.member.model.entity.Member;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface MemberRepository extends ReactiveCrudRepository<Member, UUID> {

    Mono<Member> findByEmail(String email);
}
