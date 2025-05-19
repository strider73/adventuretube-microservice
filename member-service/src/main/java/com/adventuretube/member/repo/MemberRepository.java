package com.adventuretube.member.repo;


import com.adventuretube.member.model.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    @Query("SELECT  s from Member  s where  s.email = ?1")
    Optional<Member>  findMemberByEmail(String email);
}
