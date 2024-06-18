package com.adventuretube.geospatial.filter;

import com.adventuretube.geospatial.service.JwtUtil;
import com.adventuretube.geospatial.service.CustomUserDetailService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailService  customUserDetailService;
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
       log.info("JwtAuthFilter.doFilterInternal has been called");
        /*TODO
            1. skip the token validation and null check since its already done in gateway-service
            2. extract the token
            3. authenticate the request in security context
                by get the userDetail from userDetailService and compare to the data from token
                and get the user role to authorization
            4. deal the exception
         */

        String token = request.getHeader("Authorization");
        //String token = authHeader.substring(7);//token from header which is issued after sign in or login

        try {
            String username = jwtUtil.extractUsername(token);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                log.info("user name is :" + username + "  hasn't been authenticate yet");
                UserDetails userDetails = customUserDetailService.loadUserByUsername(username);
                if (jwtUtil.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, null);//create authenticationToken using a user detail
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));//and set authenticationToken base on request
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);//authenticated request stored in security context
                }

            }

            filterChain.doFilter(request, response);
        }catch (AccessDeniedException e){
            log.error("token error :"+e.getMessage());
        }


    }
}
