package com.yowyob.businesscore._standalone;

import com.yowyob.businesscore.shared.error.ProblemException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * ⚠️ STANDALONE ONLY — mappe ProblemException -> RFC 7807.
 * Dans le socle, c'est GlobalProblemHandler qui s'en charge. À SUPPRIMER lors de l'intégration.
 */
@RestControllerAdvice
public class StandaloneProblemHandler {

    @ExceptionHandler(ProblemException.class)
    public ProblemDetail handle(ProblemException ex) {
        return ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getDetail());
    }
}
