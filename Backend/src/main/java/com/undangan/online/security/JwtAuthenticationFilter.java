package com.undangan.online.security;

import com.undangan.online.entity.Client;
import com.undangan.online.exception.AuthException;
import com.undangan.online.repository.ClientRepository;
import com.undangan.online.repository.UsersRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider tokenProvider;
    private final UsersRepository usersRepository;
    private final ClientRepository clientRepository;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, UsersRepository usersRepository, ClientRepository clientRepository) {
        this.tokenProvider = tokenProvider;
        this.usersRepository = usersRepository;
        this.clientRepository = clientRepository;
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
            log.warn("Invalid JWT token received from remoteAddress={}, path={}",
                    request.getRemoteAddr(), request.getRequestURI());
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

        if (clientId != null && !isPublicOrAuthEndpoint(request.getRequestURI())) {
            validateClient(clientId);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicOrAuthEndpoint(String requestURI) {
        return requestURI.startsWith("/api/v1/public/") || requestURI.startsWith("/api/v1/auth/");
    }

    private void validateClient(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> {
                    log.warn("Client {} tidak ditemukan saat validasi token", clientId);
                    return new AuthException("CLIENT_EXPIRED", "Akun tidak ditemukan", 401);
                });

        if (!"active".equals(client.getStatus()) || client.getExpiresAt().isBefore(LocalDate.now())) {
            log.warn("Client {} sudah expired/nonaktif, request ditolak", clientId);
            throw new AuthException("CLIENT_EXPIRED", "Akun sudah expired atau nonaktif", 401);
        }
    }
}
