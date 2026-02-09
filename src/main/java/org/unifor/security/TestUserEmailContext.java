package org.unifor.security;

import jakarta.enterprise.context.RequestScoped;

/**
 * Holds the X-Test-User-Email header value when present.
 * Used only for concurrent enrollment tests where two different users must make requests from the same test.
 * Production clients never send this header; OIDC provides the identity.
 */
@RequestScoped
public class TestUserEmailContext {

    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
