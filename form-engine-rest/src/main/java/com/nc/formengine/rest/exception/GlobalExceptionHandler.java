package com.nc.formengine.rest.exception;

import com.nc.formengine.model.exception.DuplicateResourceException;
import com.nc.formengine.model.exception.FormDefinitionNotEditableException;
import com.nc.formengine.model.exception.InvalidFormDefinitionTransitionException;
import com.nc.formengine.model.exception.ResourceNotFoundException;
import com.nc.formengine.model.exception.ValidationFailedException;
import com.nc.formengine.submission.model.exception.FormNotAcceptingSubmissionsException;
import com.nc.formengine.submission.model.exception.IllegalSubmissionTransitionException;
import com.nc.formengine.submission.model.exception.InvalidSubmissionStatusException;
import com.nc.formengine.submission.model.exception.SubmissionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Translates domain and framework exceptions into RFC 7807 {@code application/problem+json}.
 *
 * <p>Extends {@link ResponseEntityExceptionHandler} so the built-in Spring MVC failures
 * (unreadable body, path-variable type mismatch, unsupported method, unknown route) already come
 * back as ProblemDetail; only the domain exceptions and the bean-validation body are customised
 * here. Because this advice is present, {@code spring.mvc.problemdetails.enabled} must stay off —
 * the advice takes precedence and enabling both is redundant.
 *
 * <p>The definition and submission slices are independent, so their exception roots are unrelated
 * types. Each handler lists both.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String TYPE_PREFIX = "https://form-engine/errors/";

    @ExceptionHandler({ResourceNotFoundException.class, SubmissionNotFoundException.class})
    public ResponseEntity<Object> handleNotFound(RuntimeException ex, WebRequest request) {
        ProblemDetail body = problem(HttpStatus.NOT_FOUND, "not-found", "Resource Not Found", ex.getMessage());
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Object> handleDuplicate(DuplicateResourceException ex, WebRequest request) {
        ProblemDetail body = problem(HttpStatus.CONFLICT, "duplicate-resource", "Duplicate Resource", ex.getMessage());
        body.setProperty("field", ex.getField());
        body.setProperty("value", ex.getValue());
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    /**
     * A domain rule that can only be checked against persisted state: the request was well formed,
     * so 422 rather than 400.
     */
    @ExceptionHandler(ValidationFailedException.class)
    public ResponseEntity<Object> handleDomainValidation(ValidationFailedException ex, WebRequest request) {
        ProblemDetail body = problem(HttpStatus.UNPROCESSABLE_ENTITY, "domain-validation-failed",
                "Domain Validation Failed", ex.getMessage());
        body.setProperty("errors", ex.getReasons());
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.UNPROCESSABLE_ENTITY, request);
    }

    /**
     * A lifecycle rule was broken: the request is well formed and the resource exists, but it is in a
     * state that does not allow what was asked. 409, since retrying the same call cannot help — the
     * state has to change first, or a new version has to be created.
     */
    @ExceptionHandler({
            FormDefinitionNotEditableException.class,
            InvalidFormDefinitionTransitionException.class,
            IllegalSubmissionTransitionException.class,
            FormNotAcceptingSubmissionsException.class})
    public ResponseEntity<Object> handleIllegalState(RuntimeException ex, WebRequest request) {
        ProblemDetail body = problem(HttpStatus.CONFLICT, "illegal-state", "Illegal State", ex.getMessage());
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(InvalidSubmissionStatusException.class)
    public ResponseEntity<Object> handleInvalidStatus(InvalidSubmissionStatusException ex, WebRequest request) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "invalid-request", "Invalid Request", ex.getMessage());
        body.setProperty("allowedValues", ex.getAllowedValues());
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "invalid-request", "Invalid Request", ex.getMessage());
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Last resort. The detail is deliberately generic: the stacktrace goes to the log, never to
     * the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unhandled exception while serving {}", describe(request), ex);
        ProblemDetail body = problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Internal Server Error",
                "The request could not be completed because of an unexpected error.");
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /** Bean Validation on {@code @Valid @RequestBody}: 400, with one entry per violated constraint. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                 HttpHeaders headers,
                                                                 HttpStatusCode status,
                                                                 WebRequest request) {
        List<Map<String, String>> errors = Stream.concat(
                        ex.getBindingResult().getFieldErrors().stream()
                                .map(error -> violation(error.getField(), error)),
                        ex.getBindingResult().getGlobalErrors().stream()
                                .map(error -> violation(error.getObjectName(), error)))
                .sorted(Comparator.comparing(violation -> violation.get("field")))
                .toList();

        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "validation-failed", "Request Validation Failed",
                "The request body failed validation. See 'errors' for details.");
        body.setProperty("errors", errors);

        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /**
     * Every problem body — ours and the ones the superclass builds for framework exceptions — passes
     * through here, so this is where the shared properties are stamped on.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                             Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            problem.setProperty("timestamp", Instant.now().toString());
            if (problem.getInstance() == null && request instanceof ServletWebRequest servletRequest) {
                problem.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
            }
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private static ProblemDetail problem(HttpStatus status, String typeSlug, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(TYPE_PREFIX + typeSlug));
        problem.setTitle(title);
        return problem;
    }

    private static Map<String, String> violation(String field, ObjectError error) {
        // LinkedHashMap rather than Map.of: null-tolerant and keeps field before message in the JSON.
        Map<String, String> violation = new LinkedHashMap<>();
        violation.put("field", field);
        violation.put("message", error.getDefaultMessage() != null ? error.getDefaultMessage() : "is invalid");
        if (error instanceof FieldError fieldError && fieldError.getRejectedValue() != null) {
            violation.put("rejectedValue", String.valueOf(fieldError.getRejectedValue()));
        }
        return violation;
    }

    private static String describe(WebRequest request) {
        return request instanceof ServletWebRequest servletRequest
                ? servletRequest.getHttpMethod() + " " + servletRequest.getRequest().getRequestURI()
                : request.getDescription(false);
    }
}
