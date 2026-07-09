package com.yowyob.businesscore.adapter.in.rest.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;

import com.yowyob.businesscore.adapter.out.kernel.KernelException;
import com.yowyob.businesscore.application.error.ProblemException;

import reactor.test.StepVerifier;

class GlobalProblemHandlerTest {

    private final GlobalProblemHandler handler = new GlobalProblemHandler();

    private ServerWebExchange exchangeNonCommittee() {
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        when(exchange.getResponse()).thenReturn(response);
        when(response.isCommitted()).thenReturn(false);
        return exchange;
    }

    @Test
    @DisplayName("ProblemException badGateway → 502 avec détail kernel")
    void problem_bad_gateway() {
        ResponseEntity<ProblemDetail> reponse = handler.handleProblem(
                ProblemException.badGateway("Kernel HTTP 400 sur /oauth2/token"),
                exchangeNonCommittee()).block();

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(reponse.getBody().getDetail()).contains("oauth2/token");
        assertThat(reponse.getBody().getTitle()).isEqualTo("Service kernel indisponible");
    }

    @Test
    @DisplayName("ProblemException sur réponse déjà commitée → Mono.empty()")
    void problem_reponse_committee() {
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        when(exchange.getResponse()).thenReturn(response);
        when(response.isCommitted()).thenReturn(true);

        StepVerifier.create(handler.handleProblem(
                        ProblemException.forbidden("Contexte d'authentification absent"), exchange))
                .verifyComplete();
    }

    @Test
    @DisplayName("KernelException métier → 422 avec kernelErrorCode")
    void kernel_metier() {
        ResponseEntity<ProblemDetail> reponse = handler.handleKernelMetier(
                new KernelException("ORG_NOT_APPROVED", "Organisation non approuvée"),
                exchangeNonCommittee()).block();

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(reponse.getBody().getProperties().get("kernelErrorCode")).isEqualTo("ORG_NOT_APPROVED");
    }

    @Test
    @DisplayName("Exception encapsulée → délègue à ProblemException")
    void exception_encapsulee_problem() {
        ProblemException cause = ProblemException.badGateway("kernel down");
        RuntimeException wrapper = new RuntimeException("wrapper", cause);

        ResponseEntity<ProblemDetail> reponse = handler.handleUnexpected(wrapper, exchangeNonCommittee()).block();

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(reponse.getBody().getDetail()).isEqualTo("kernel down");
    }

    @Test
    @DisplayName("WebClientResponseException → 502 avec chemin kernel")
    void kernel_transport_avec_chemin() {
        WebClientResponseException transport = WebClientResponseException.create(
                401, "Unauthorized", null, null, null);

        ResponseEntity<ProblemDetail> reponse = handler.handleKernelTransport(
                transport, exchangeNonCommittee()).block();

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(reponse.getBody().getDetail()).contains("401");
        assertThat(reponse.getBody().getTitle()).isEqualTo("Service kernel indisponible");
    }

    @Test
    @DisplayName("WebClientResponseException encapsulée → 502 avec chemin")
    void exception_encapsulee_webclient() {
        WebClientResponseException transport = WebClientResponseException.create(
                400, "Bad Request", null, null, null);
        RuntimeException wrapper = new RuntimeException("retry exhausted", transport);

        ResponseEntity<ProblemDetail> reponse = handler.handleUnexpected(wrapper, exchangeNonCommittee()).block();

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(reponse.getBody().getDetail()).contains("400");
    }
}
