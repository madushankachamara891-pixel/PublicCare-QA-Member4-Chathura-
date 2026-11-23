package lk.publiccare.backend.exception;

import lk.publiccare.backend.auth.AuthController;
import lk.publiccare.backend.auth.AuthDtos;
import lk.publiccare.backend.auth.DuplicateEmailException;
import lk.publiccare.backend.report.ReportNotFoundException;
import lk.publiccare.backend.report.ReportDeletionFailedException;
import lk.publiccare.backend.report.ReportDtos;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import lk.publiccare.backend.report.ReportImageService;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<AuthDtos.ErrorResponse> invalidRequest(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst().map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Request validation failed.");
        return ResponseEntity.badRequest().body(new AuthDtos.ErrorResponse(message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<AuthDtos.ErrorResponse> invalidInput(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new AuthDtos.ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<AuthDtos.ErrorResponse> duplicateEmail() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new AuthDtos.ErrorResponse("An account with this email already exists."));
    }

    @ExceptionHandler(AuthController.InvalidCredentialsException.class)
    ResponseEntity<AuthDtos.ErrorResponse> invalidCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthDtos.ErrorResponse("Invalid email or password."));
    }

    @ExceptionHandler(ReportNotFoundException.class)
    ResponseEntity<ReportDtos.ErrorResponse> reportNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ReportDtos.ErrorResponse("Report not found."));
    }

    @ExceptionHandler(ReportDeletionFailedException.class)
    ResponseEntity<ReportDtos.ErrorResponse> reportDeletionFailed() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ReportDtos.ErrorResponse("Unable to delete report."));
    }

    @ExceptionHandler(ReportImageService.ImageAlreadyExistsException.class)
    ResponseEntity<ReportDtos.ErrorResponse> imageAlreadyExists() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ReportDtos.ErrorResponse("This report already has an image."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ReportDtos.ErrorResponse> uploadTooLarge() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(new ReportDtos.ErrorResponse("Image must be 5 MB or smaller."));
    }
}
