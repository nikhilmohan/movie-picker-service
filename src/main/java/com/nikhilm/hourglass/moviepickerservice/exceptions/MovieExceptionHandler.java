package com.nikhilm.hourglass.moviepickerservice.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class MovieExceptionHandler {

    @ExceptionHandler(MovieException.class)
    public ResponseEntity<ApiError> handleMovieException(MovieException e) {
        return ResponseEntity.status(e.getStatus()).body(new ApiError(String.valueOf(e.getStatus()), e.getMessage()));
    }
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleGlobalException(RuntimeException e) {
        log.error("Exception " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("500", "Internal server error!"));
    }

}
