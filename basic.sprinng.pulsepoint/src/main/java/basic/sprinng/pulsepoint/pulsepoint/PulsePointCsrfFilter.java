package basic.sprinng.pulsepoint.pulsepoint;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that exposes the Spring Security CSRF token as the `pp_csrf` cookie
 * required by the PulsePoint v2 wire protocol.
 */
@Component
@Order(10)
public class PulsePointCsrfFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            String tokenValue = csrfToken.getToken();

            // Set main pp_csrf cookie
            Cookie ppCsrfCookie = new Cookie("pp_csrf", tokenValue);
            ppCsrfCookie.setPath("/");
            ppCsrfCookie.setHttpOnly(false); // PulsePoint JS reads this
            ppCsrfCookie.setMaxAge(86400);
            response.addCookie(ppCsrfCookie);

            // Also set port-specific cookie if port is present (dev server support in PulsePoint)
            int port = request.getServerPort();
            if (port > 0 && port != 80 && port != 443) {
                Cookie ppCsrfPortCookie = new Cookie("pp_csrf_" + port, tokenValue);
                ppCsrfPortCookie.setPath("/");
                ppCsrfPortCookie.setHttpOnly(false);
                ppCsrfPortCookie.setMaxAge(86400);
                response.addCookie(ppCsrfPortCookie);
            }
        }

        filterChain.doFilter(request, response);
    }
}
