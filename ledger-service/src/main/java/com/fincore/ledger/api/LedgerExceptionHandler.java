package com.fincore.ledger.api;

import com.fincore.ledger.service.AccountNotFoundException;
import com.fincore.ledger.service.UnbalancedTransactionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps domain exceptions to proper HTTP responses. Without this, Spring
 * would return a 500 for every uncaught exception. A {@code @RestControllerAdvice}
 * is a global exception handler that applies to all controllers.
 *
 * <p>Uses {@link ProblemDetail} (RFC 9457) — Spring's built-in support for
 * structured error responses. The response body looks like:
 * <pre>
 * {
 *   "type": "about:blank",
 *   "title": "Not Found",
 *   "status": 404,
 *   "detail": "No account found for accountRef: FOO"
 * }
 * </pre>
 */
@RestControllerAdvice
public class LedgerExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleAccountNotFound(AccountNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UnbalancedTransactionException.class)
    public ProblemDetail handleUnbalanced(UnbalancedTransactionException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleConflict(IllegalStateException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
