package com.adventuretube.auth.provider;

import com.adventuretube.auth.service.CustomUserDetailService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;

//@Component
//@AllArgsConstructor
//@NoArgsConstructor
public class CustomAuthenticationProvider extends DaoAuthenticationProvider {

    //@Autowired
    private  CustomUserDetailService userDetailsService;
    //@Autowired
    private  PasswordEncoder passwordEncoder;

    //AuthenticationException which is explicitly thrown from this method will be handled
    //spring security authentication mechanism.
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String password = authentication.getCredentials().toString();

        UserDetails userDetails;
        userDetails = userDetailsService.loadUserByUsername(email);

        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        return new UsernamePasswordAuthenticationToken(email, password, userDetails.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
