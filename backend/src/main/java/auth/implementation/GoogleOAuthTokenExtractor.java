package auth.implementation;

import auth.definition.TokenExtractor;
import auth.definition.TokenHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
public class GoogleOAuthTokenExtractor implements Filter, TokenExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleOAuthTokenExtractor.class);

    @Autowired
    private TokenHolder tokenHolder;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            tokenHolder.setToken(extractToken(servletRequest));
        } catch (Exception e) {
            LOGGER.error("Error ocurred while extracting oauth token from request", e);
        } finally {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    @Override
    public String extractToken(ServletRequest request) {
        return ((HttpServletRequest) request).getHeader(HttpHeaders.AUTHORIZATION);
    }

}
