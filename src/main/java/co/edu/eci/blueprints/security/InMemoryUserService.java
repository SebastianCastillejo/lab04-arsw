package co.edu.eci.blueprints.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class InMemoryUserService {
    private final Map<String, String> users; // username -> hash
    private final Map<String, String> scopes; // username -> scopes separados por espacio
    private final PasswordEncoder encoder;

    public InMemoryUserService(PasswordEncoder encoder) {
        this.encoder = encoder;
        this.users = Map.of(
            "student", encoder.encode("student123"),
            "assistant", encoder.encode("assistant123")
        );
        // student puede leer y escribir; assistant solo puede consultar
        this.scopes = Map.of(
            "student", "blueprints.read blueprints.write",
            "assistant", "blueprints.read"
        );
    }

    public boolean isValid(String username, String rawPassword) {
        String hash = users.get(username);
        return hash != null && encoder.matches(rawPassword, hash);
    }

    public String scopesOf(String username) {
        return scopes.getOrDefault(username, "");
    }
}
