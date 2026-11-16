package com.ruanvascolabs.camnode.shared.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.ruanvascolabs.camnode.admin.AdminKeyLockedException;
import com.ruanvascolabs.camnode.admin.InvalidAdminKeyException;
import com.ruanvascolabs.camnode.device.InvalidDeviceKeyException;
import com.ruanvascolabs.camnode.photo.PhotoNotFoundException;
import com.ruanvascolabs.camnode.photo.PhotoStorageUnavailableException;
import com.ruanvascolabs.camnode.ring.EmptyPhotoException;
import com.ruanvascolabs.camnode.ring.RingNotFoundException;
import com.ruanvascolabs.camnode.ring.UnsupportedPhotoFormatException;

/**
 * Framework exceptions are handled by the superclass as Problem Details, with the PT-BR detail looked up in
 * messages.properties ({@code problemDetail.<exception class>}).
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // The messages.properties key for this exception is ignored on Spring 7.0.x (the detail stays "Validation
    // failure"), so it is set here. Move it there once a Spring upgrade makes RingHistoryIT pass that way.
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(ex, ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Parâmetros inválidos."), headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(InvalidDeviceKeyException.class)
    ProblemDetail invalidDeviceKey() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Chave do dispositivo ausente ou inválida.");
    }

    @ExceptionHandler(InvalidAdminKeyException.class)
    ProblemDetail invalidAdminKey() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Senha de administrador ausente ou incorreta.");
    }

    // Rounded up so a client that waits Retry-After seconds is not refused again by a fraction of a second.
    @ExceptionHandler(AdminKeyLockedException.class)
    ResponseEntity<ProblemDetail> adminKeyLocked(AdminKeyLockedException e) {
        long seconds = Math.max(1, e.remaining().plusMillis(999).toSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(seconds))
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                        "Muitas tentativas. Tente de novo em alguns minutos."));
    }

    @ExceptionHandler(EmptyPhotoException.class)
    ProblemDetail emptyPhoto() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "A foto enviada está vazia.");
    }

    @ExceptionHandler(UnsupportedPhotoFormatException.class)
    ProblemDetail unsupportedPhotoFormat() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "A foto precisa estar em JPEG.");
    }

    @ExceptionHandler(RingNotFoundException.class)
    ProblemDetail ringNotFound() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Toque não encontrado.");
    }

    // The ring exists but its object does not: data loss, not an outage, so retrying would not help.
    @ExceptionHandler(PhotoNotFoundException.class)
    ProblemDetail photoNotFound(PhotoNotFoundException e) {
        log.warn("Ring photo missing from storage: {}", e.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Foto não encontrada.");
    }

    @ExceptionHandler(PhotoStorageUnavailableException.class)
    ProblemDetail photoStorageUnavailable(PhotoStorageUnavailableException e) {
        log.error("Photo storage unavailable", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "O armazenamento de fotos está indisponível agora. Tente novamente.");
    }
}
