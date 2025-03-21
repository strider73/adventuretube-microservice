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
import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailService  customUserDetailService;

    private static final List<String> OPEN_ENDPOINTS = List.of(
            "/auth/register",
            "/auth/login",
            "/web/registerMember",
            "/actuator/health",
            "/healthcheck",
            "/swagger-ui.html",   // Allow Swagger UI
            "/swagger-ui/**",      // Allow Swagger static resources
            "/v3/api-docs/**",     // Allow OpenAPI documentation
            "/v3/api-docs"         // Allow direct API docs access
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

            //so in geospatial-service there should be token for all reqeuest
            //but put this in just in case at this moment
            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

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
            log.error("token error :"+e.getMessage());
        }


    }
}
