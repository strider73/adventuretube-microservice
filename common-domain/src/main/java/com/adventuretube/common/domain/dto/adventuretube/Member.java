package com.adventuretube.common.domain.dto.adventuretube;


import jakarta.persistence.*;
import java.util.Locale;

@Entity
@Table
public class Member {
    @Id
    @SequenceGenerator(
            name = "member_sequence",
            sequenceName = "member_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "member_sequence"
    )
    private Long id;
    private String name;
    private String channeld;
    private String email;

    public Member() {
    }


    public Member(String name, String channeld, String email) {
        this.name = name;
        this.channeld = channeld;
        this.email = email;
    }

    public Member(Long id, String name, String channeld, String email) {
        this.id = id;
        this.name = name;
        this.channeld = channeld;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getChanneld() {
        return channeld;
    }

    public void setChanneld(String channeld) {
        this.channeld = channeld;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "Member{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", channeld='" + channeld + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}