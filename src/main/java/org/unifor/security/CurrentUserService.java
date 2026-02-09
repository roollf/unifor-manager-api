package org.unifor.security;

import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.unifor.entity.User;
import org.unifor.entity.UserRole;
import org.unifor.exception.ForbiddenException;
import org.unifor.repository.UserRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Resolves the current authenticated user from the OIDC token.
 * Maps Keycloak principal to User entity via email (PRD: AC-05).
 * When JWT is absent (e.g. OIDC disabled in tests with @TestSecurity), falls back to SecurityIdentity principal name as email.
 */
@ApplicationScoped
public class CurrentUserService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_PREFERRED_USERNAME = "preferred_username";

    private final Instance<JsonWebToken> jwtInstance;
    private final UserRepository userRepository;

    @Inject
    SecurityIdentity securityIdentity;

    public CurrentUserService(Instance<JsonWebToken> jwtInstance, UserRepository userRepository) {
        this.jwtInstance = jwtInstance;
        this.userRepository = userRepository;
    }

    /**
     * Returns the current user as coordinator. Fails if user not found or role is not COORDINATOR.
     */
    public User getCurrentCoordinator() {
        User user = resolveCurrentUser();
        if (user.role != UserRole.COORDINATOR) {
            throw new ForbiddenException("Acesso negado: usuário não é coordenador");
        }
        return user;
    }

    /**
     * Returns the current user as student. Fails if user not found or role is not STUDENT.
     */
    public User getCurrentStudent() {
        User user = resolveCurrentUser();
        if (user.role != UserRole.STUDENT) {
            throw new ForbiddenException("Acesso negado: usuário não é estudante");
        }
        return user;
    }

    /**
     * Returns the current user from token. Fails if user not found in database.
     */
    public User resolveCurrentUser() {
        String email = extractEmail();
        if (email == null || email.isBlank()) {
            throw new ForbiddenException("Token inválido: e-mail não encontrado");
        }

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ForbiddenException("Usuário não encontrado para o e-mail: " + email);
        }
        return user;
    }

    private String extractEmail() {
        String email = getClaim(CLAIM_EMAIL);
        if (email != null && !email.isBlank()) {
            return email;
        }
        email = getClaim(CLAIM_PREFERRED_USERNAME);
        if (email != null && !email.isBlank()) {
            return email;
        }
        // Test / mock auth: use principal name as email (e.g. @TestSecurity(user = "email@..."))
        if (securityIdentity != null && securityIdentity.getPrincipal() != null) {
            return securityIdentity.getPrincipal().getName();
        }
        return null;
    }

    private String getClaim(String claimName) {
        if (!jwtInstance.isResolvable()) {
            return null;
        }
        Object claim = jwtInstance.get().getClaim(claimName);
        return claim != null ? claim.toString() : null;
    }
}
