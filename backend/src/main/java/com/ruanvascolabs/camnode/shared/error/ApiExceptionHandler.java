package com.ruanvascolabs.camnode.shared.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.ruanvascolabs.camnode.device.InvalidDeviceKeyException;
import com.ruanvascolabs.camnode.photo.PhotoStorageUnavailableException;
import com.ruanvascolabs.camnode.ring.EmptyPhotoException;
import com.ruanvascolabs.camnode.ring.UnsupportedPhotoFormatException;

/**
 * Framework exceptions (missing part, upload too large) are handled by the superclass as Problem Details.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidDeviceKeyException.class)
    ProblemDetail invalidDeviceKey() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Chave do dispositivo ausente ou inválida.");
    }

    @ExceptionHandler(EmptyPhotoException.class)
    ProblemDetail emptyPhoto() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "A foto enviada está vazia.");
    }

    @ExceptionHandler(UnsupportedPhotoFormatException.class)
    ProblemDetail unsupportedPhotoFormat() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "A foto precisa estar em JPEG.");
    }

    @ExceptionHandler(PhotoStorageUnavailableException.class)
    ProblemDetail photoStorageUnavailable(PhotoStorageUnavailableException e) {
        log.error("Photo storage unavailable", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Não foi possível guardar a foto agora. Tente novamente.");
    }
}
