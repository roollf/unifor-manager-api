package org.unifor.security;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * When X-Test-User-Email header is present, stores it in TestUserEmailContext.
 * Used for concurrent enrollment integration tests. Production clients never send this header.
 */
@Provider
@jakarta.annotation.Priority(jakarta.interceptor.Interceptor.Priority.APPLICATION)
public class TestUserEmailFilter implements ContainerRequestFilter {

    private static final String HEADER = "X-Test-User-Email";

    @Inject
    TestUserEmailContext testUserEmailContext;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String email = requestContext.getHeaderString(HEADER);
        if (email != null && !email.isBlank()) {
            testUserEmailContext.setEmail(email.trim());
        }
    }
}
