package ng.edu.unilag.tradeup.web.error;

import org.springframework.http.HttpStatus;

/**
 * Base class for every failure TradeUp raises on purpose.
 *
 * <p>Carrying the HTTP status and a stable machine code on the exception itself
 * means {@link GlobalExceptionHandler} can translate any of them without a chain
 * of instanceof checks, and adding a new failure never means editing the handler.
 */
public abstract class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected AppException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
