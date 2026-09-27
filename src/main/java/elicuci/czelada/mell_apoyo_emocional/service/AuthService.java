package elicuci.czelada.mell_apoyo_emocional.service;

import elicuci.czelada.mell_apoyo_emocional.auth.AuthResponse;
import elicuci.czelada.mell_apoyo_emocional.auth.GoogleAuthRequest;
import elicuci.czelada.mell_apoyo_emocional.auth.LoginRequest;
import elicuci.czelada.mell_apoyo_emocional.auth.RegisterRequest;
import elicuci.czelada.mell_apoyo_emocional.model.User;
import elicuci.czelada.mell_apoyo_emocional.model.enums.AuthProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import elicuci.czelada.mell_apoyo_emocional.repository.UserRepository;
import elicuci.czelada.mell_apoyo_emocional.service.security.GoogleTokenVerifierService;
import elicuci.czelada.mell_apoyo_emocional.service.security.JwtService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifierService googleTokenVerifierService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       GoogleTokenVerifierService googleTokenVerifierService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleTokenVerifierService = googleTokenVerifierService;
    }

    public AuthResponse register(RegisterRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(u -> {
            throw new IllegalArgumentException("Ya existe una cuenta con ese email");
        });

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .authProvider(AuthProvider.LOCAL)
                .build();

        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));

        if (user.getAuthProvider() != AuthProvider.LOCAL || user.getPasswordHash() == null) {
            throw new IllegalArgumentException("Esta cuenta usa inicio de sesión con Google");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }

        return buildAuthResponse(user);
    }

    public AuthResponse loginOrRegisterWithGoogle(GoogleAuthRequest request) {
        GoogleTokenVerifierService.GooglePayload payload = googleTokenVerifierService.verify(request.idToken());

        User user = userRepository.findByEmail(payload.email())
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(payload.email())
                        .firstName(payload.firstName())
                        .authProvider(AuthProvider.GOOGLE)
                        .providerId(payload.sub())
                        .build()));

        // Si el usuario ya existía como cuenta LOCAL, vinculamos el provider_id
        // para futuras verificaciones, sin romper su login por password.
        if (user.getProviderId() == null) {
            user.setProviderId(payload.sub());
            userRepository.save(user);
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFirstName());
    }
}
