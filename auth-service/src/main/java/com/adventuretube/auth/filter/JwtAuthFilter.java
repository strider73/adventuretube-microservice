/*
The JWT filter is responsible for extracting JWT token(token from adventuretube backend) from the request
,validating it, and setting the authentication in the security context if token is valid

by the time  the request reaches the 'UsernamePasswordAuthenticationFilter' the JWT filter has already
authenticated the user based on token.




 */

package com.adventuretube.auth.filter;
import com.adventuretube.auth.service.CustomUserDetailService;
import com.adventuretube.auth.service.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailService customUserDetailService;
    // List of endpoints to skip JWT validation
    private static final List<String> OPEN_ENDPOINTS = List.of(
            "/auth/register",
            "/auth/login",
            "/web/registerMember",
            "/actuator/health",
            "/healthcheck"
    );




    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Skip filtering for open endpoints
        String path = request.getServletPath();
        if (OPEN_ENDPOINTS.stream().anyMatch(path::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.info("JwtAuthFilter.doFilterInternal has been called");
        //String token = authHeader.substring(7);//token from header which is issued after sign in or login

        try {
            String token = request.getHeader("Authorization");
            //security-service has an exception to deal with null token in
            // JwtAuthfilter for login / signing in request
            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            //This logic is about
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
        }catch (Exception e){
            log.error("Error processing JWT token: " + e.getMessage());
            filterChain.doFilter(request, response);
        }
    }
}
