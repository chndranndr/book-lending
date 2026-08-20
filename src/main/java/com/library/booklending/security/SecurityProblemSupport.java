package com.library.booklending.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.booklending.common.ProblemDetails;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
public class SecurityProblemSupport {

    private final ObjectMapper objectMapper;

    public SecurityProblemSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void writeUnauthorized(HttpServletResponse response) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, "Unauthorized", "Authentication is required to access this resource.", "UNAUTHORIZED");
    }

    public void writeForbidden(HttpServletResponse response) throws IOException {
        write(response, HttpStatus.FORBIDDEN, "Forbidden", "You do not have permission to access this resource.", "FORBIDDEN");
    }

    private void write(
            HttpServletResponse response,
            HttpStatus status,
            String title,
            String detail,
            String code
    ) throws IOException {
        ProblemDetail problemDetail = ProblemDetails.create(status, title, detail, code);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problemDetail);
    }
}
