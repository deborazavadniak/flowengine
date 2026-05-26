package com.serasa.flowengine.api;

import com.serasa.flowengine.api.dto.CreateFlowRequest;
import com.serasa.flowengine.api.dto.ExecuteFlowRequest;
import com.serasa.flowengine.api.dto.Responses;
import com.serasa.flowengine.domain.engine.ExecutionResult;
import com.serasa.flowengine.domain.engine.FlowEngine;
import com.serasa.flowengine.domain.model.BlockDefinition;
import com.serasa.flowengine.domain.model.Flow;
import com.serasa.flowengine.domain.port.FlowRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FlowController — camada REST")
class FlowControllerTest {

    @Mock
    FlowRepository flowRepository;

    @Mock
    FlowEngine flowEngine;

    @InjectMocks
    FlowController flowController;

    // ── POST /api/flows ───────────────────────────────────────────────────────

    @Test
    @DisplayName("createFlow — deve retornar 201 e salvar o fluxo")
    void shouldCreateFlowAndReturn201() {
        when(flowRepository.existsById("flow-test")).thenReturn(false);

        CreateFlowRequest request = new CreateFlowRequest(
                "flow-test",
                "Teste",
                "b1",
                List.of(new CreateFlowRequest.BlockDefinitionRequest(
                        "b1", "output", Map.of("value", "ok"), Map.of()))
        );

        ResponseEntity<Responses.CreateFlowResponse> response =
                flowController.createFlow(request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo("flow-test");
        assertThat(response.getBody().blockCount()).isEqualTo(1);

        verify(flowRepository, times(1)).save(any(Flow.class));
    }

    @Test
    @DisplayName("createFlow — deve retornar 409 quando ID já existe")
    void shouldReturn409WhenFlowAlreadyExists() {
        when(flowRepository.existsById("flow-test")).thenReturn(true);

        CreateFlowRequest request = new CreateFlowRequest(
                "flow-test",
                "Teste",
                "b1",
                List.of(new CreateFlowRequest.BlockDefinitionRequest(
                        "b1", "output", Map.of(), Map.of()))
        );

        ResponseEntity<Responses.CreateFlowResponse> response =
                flowController.createFlow(request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        verify(flowRepository, never()).save(any());
    }

    // ── POST /api/flows/{id}/execute ──────────────────────────────────────────

    @Test
    @DisplayName("executeFlow — deve executar fluxo e retornar resultado")
    void shouldExecuteFlowAndReturnResult() {
        Flow flow = new Flow("prime-check", "Primo", "b1", List.of(
                new BlockDefinition("b1", "output", Map.of("value", "É primo"), Map.of())
        ));

        ExecutionResult result = ExecutionResult.builder()
                .success("É primo")
                .steps(List.of())
                .build();

        when(flowRepository.findById("prime-check")).thenReturn(Optional.of(flow));
        when(flowEngine.execute(eq(flow), any())).thenReturn(result);

        ExecuteFlowRequest request = new ExecuteFlowRequest(Map.of("number", 17));

        ResponseEntity<Responses.ExecuteFlowResponse> response =
                flowController.executeFlow("prime-check", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("SUCCESS");
        assertThat(response.getBody().output()).isEqualTo("É primo");

        verify(flowEngine, times(1)).execute(eq(flow), any());
    }

    @Test
    @DisplayName("executeFlow — deve lançar FlowNotFoundException quando fluxo não existe")
    void shouldThrowNotFoundWhenFlowDoesNotExist() {
        when(flowRepository.findById("nao-existe")).thenReturn(Optional.empty());

        ExecuteFlowRequest request = new ExecuteFlowRequest(Map.of("number", 17));

        assertThatThrownBy(() -> flowController.executeFlow("nao-existe", request))
                .isInstanceOf(FlowController.FlowNotFoundException.class)
                .hasMessageContaining("nao-existe");
    }

    // ── GET /api/flows ────────────────────────────────────────────────────────

    @Test
    @DisplayName("listFlows — deve retornar page com os fluxos existentes")
    void shouldReturnPageWithFlows() {
        Flow flow = new Flow("f1", "Fluxo 1", "b1", List.of(
                new BlockDefinition("b1", "output", Map.of(), Map.of())
        ));
        when(flowRepository.findAll()).thenReturn(List.of(flow));

        Pageable pageable = PageRequest.of(0, 10);
        ResponseEntity<Page<Responses.CreateFlowResponse>> response =
                flowController.listFlows(pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTotalElements()).isEqualTo(1);
        assertThat(response.getBody().getContent().get(0).id()).isEqualTo("f1");
    }

    @Test
    @DisplayName("listFlows — deve retornar page vazia quando não há fluxos")
    void shouldReturnEmptyPageWhenNoFlows() {
        when(flowRepository.findAll()).thenReturn(List.of());

        Pageable pageable = PageRequest.of(0, 10);
        ResponseEntity<Page<Responses.CreateFlowResponse>> response =
                flowController.listFlows(pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTotalElements()).isEqualTo(0);
        assertThat(response.getBody().getContent()).isEmpty();
    }

    // ── DELETE /api/flows/{id} ────────────────────────────────────────────────

    @Test
    @DisplayName("deleteFlow — deve remover fluxo e retornar 204")
    void shouldDeleteFlowAndReturn204() {
        when(flowRepository.existsById("f1")).thenReturn(true);

        ResponseEntity<Void> response = flowController.deleteFlow("f1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(flowRepository, times(1)).deleteById("f1");
    }

    @Test
    @DisplayName("deleteFlow — deve lançar FlowNotFoundException quando fluxo não existe")
    void shouldThrowNotFoundOnDeleteWhenFlowDoesNotExist() {
        when(flowRepository.existsById("nao-existe")).thenReturn(false);

        assertThatThrownBy(() -> flowController.deleteFlow("nao-existe"))
                .isInstanceOf(FlowController.FlowNotFoundException.class)
                .hasMessageContaining("nao-existe");
    }
}