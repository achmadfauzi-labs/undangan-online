package com.undangan.online.security;

import com.undangan.online.repository.UsersRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UsersRepository usersRepository;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, UsersRepository usersRepository) {
        this.tokenProvider = tokenProvider;
        this.usersRepository = usersRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        if (!tokenProvider.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = tokenProvider.extractUsername(token);
        String role = tokenProvider.extractClaim(token, "role", String.class);
        Long clientId = tokenProvider.extractClaim(token, "clientId", Long.class);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            usersRepository.findByUsername(username).ifPresent(user -> {
                var authority = new SimpleGrantedAuthority(role != null ? role : "");
                var authToken = new UsernamePasswordAuthenticationToken(user, null, List.of(authority));
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            });
        }

        filterChain.doFilter(request, response);
    }
}
