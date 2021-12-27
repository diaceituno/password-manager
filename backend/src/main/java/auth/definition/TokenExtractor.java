package auth.definition;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

public interface TokenExtractor {

    public String extractToken(ServletRequest request);
}
