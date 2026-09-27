package elicuci.czelada.mell_apoyo_emocional.service.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import elicuci.czelada.mell_apoyo_emocional.model.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import elicuci.czelada.mell_apoyo_emocional.repository.UserRepository;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

//Intercepta cada request, busca el header "Authorization: Bearer <jwt>",

@Component
public class JwtAuthenticationFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (jwtService.isValid(token)) {
                UUID userId = jwtService.extractUserId(token);
                User user = userRepository.findById(userId).orElse(null);

                if (user != null) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            user, null, Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
