package com.serasa.flowengine.engine;

import com.serasa.flowengine.domain.engine.Block;
import com.serasa.flowengine.domain.engine.BlockRegistry;
import com.serasa.flowengine.domain.engine.ExecutionResult;
import com.serasa.flowengine.domain.engine.FlowEngine;
import com.serasa.flowengine.domain.model.BlockDefinition;
import com.serasa.flowengine.domain.model.Flow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FlowEngine — orquestração do loop de execução")
class FlowEngineTest {

    @Mock
    BlockRegistry blockRegistry;

    @InjectMocks
    FlowEngine engine;

    // ── bloco auxiliar que apenas avança ──────────────────────────────────────

    /** Simula um bloco que escreve no contexto e retorna rota "next". */
    private Block blockThatSets(String key, Object value) {
        return (def, ctx) -> { ctx.set(key, value); return "next"; };
    }

    /** Simula um bloco que define ctx.output e encerra (retorna null). */
    private Block outputBlock(String outputValue) {
        return (def, ctx) -> { ctx.set("output", outputValue); return null; };
    }

    // ── testes ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve executar bloco único e retornar output")
    void shouldExecuteSingleBlockFlow() {
        Flow flow = new Flow("f1", "F1", "b1", List.of(
                new BlockDefinition("b1", "output", Map.of("value", "ok"), Map.of())
        ));

        when(blockRegistry.resolve("output")).thenReturn(outputBlock("ok"));

        ExecutionResult result = engine.execute(flow, Map.of());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).isEqualTo("ok");
        verify(blockRegistry, times(1)).resolve("output");
    }

    @Test
    @DisplayName("Deve encadear dois blocos e passar contexto entre eles")
    void shouldChainTwoBlocksAndShareContext() {
        Flow flow = new Flow("f2", "F2", "b1", List.of(
                new BlockDefinition("b1", "set_var", Map.of(), Map.of("next", "b2")),
                new BlockDefinition("b2", "output",  Map.of(), Map.of())
        ));

        when(blockRegistry.resolve("set_var")).thenReturn(blockThatSets("x", 42L));
        when(blockRegistry.resolve("output")).thenReturn(outputBlock("done"));

        ExecutionResult result = engine.execute(flow, Map.of());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getFinalContext()).containsEntry("x", 42L);
        assertThat(result.getSteps()).hasSize(2);

        org.mockito.InOrder order = inOrder(blockRegistry);
        order.verify(blockRegistry).resolve("set_var");
        order.verify(blockRegistry).resolve("output");
    }

    @Test
    @DisplayName("Deve seguir rota 'true' quando bloco retorna 'true'")
    void shouldFollowTrueRoute() {
        Flow flow = new Flow("f3", "F3", "b1", List.of(
                new BlockDefinition("b1", "condition",
                        Map.of(), Map.of("true", "b-yes", "false", "b-no")),
                new BlockDefinition("b-yes", "output", Map.of(), Map.of()),
                new BlockDefinition("b-no",  "output", Map.of(), Map.of())
        ));

        when(blockRegistry.resolve("condition")).thenReturn((def, ctx) -> "true");
        when(blockRegistry.resolve("output")).thenReturn(outputBlock("yes"));

        ExecutionResult result = engine.execute(flow, Map.of());

        assertThat(result.getOutput()).isEqualTo("yes");
        // b-no nunca deve ter sido executado — "output" chamado só 1x
        verify(blockRegistry, times(1)).resolve("output");
    }

    @Test
    @DisplayName("Deve seguir rota 'false' quando bloco retorna 'false'")
    void shouldFollowFalseRoute() {
        Flow flow = new Flow("f4", "F4", "b1", List.of(
                new BlockDefinition("b1", "condition",
                        Map.of(), Map.of("true", "b-yes", "false", "b-no")),
                new BlockDefinition("b-yes", "output_yes", Map.of(), Map.of()),
                new BlockDefinition("b-no",  "output_no",  Map.of(), Map.of())
        ));

        when(blockRegistry.resolve("condition")).thenReturn((def, ctx) -> "false");
        when(blockRegistry.resolve("output_no")).thenReturn(outputBlock("no"));

        ExecutionResult result = engine.execute(flow, Map.of());

        assertThat(result.getOutput()).isEqualTo("no");
        verify(blockRegistry, never()).resolve("output_yes");
    }

    @Test
    @DisplayName("Deve registrar step com blockId, type e rota tomada")
    void shouldRecordStepMetadata() {
        Flow flow = new Flow("f5", "F5", "b1", List.of(
                new BlockDefinition("b1", "my_type", Map.of(), Map.of("next", "b2")),
                new BlockDefinition("b2", "output",  Map.of(), Map.of())
        ));

        when(blockRegistry.resolve("my_type")).thenReturn((def, ctx) -> "next");
        when(blockRegistry.resolve("output")).thenReturn(outputBlock("x"));

        ExecutionResult result = engine.execute(flow, Map.of());

        ExecutionResult.StepRecord firstStep = result.getSteps().get(0);
        assertThat(firstStep.blockId()).isEqualTo("b1");
        assertThat(firstStep.blockType()).isEqualTo("my_type");
        assertThat(firstStep.routeTaken()).isEqualTo("next");
        assertThat(firstStep.nextBlockId()).isEqualTo("b2");
    }

    @Test
    @DisplayName("Deve retornar status ERROR quando BlockRegistry lança exceção")
    void shouldReturnErrorWhenRegistryThrows() {
        Flow flow = new Flow("f6", "F6", "b1", List.of(
                new BlockDefinition("b1", "unknown_type", Map.of(), Map.of())
        ));

        when(blockRegistry.resolve("unknown_type"))
                .thenThrow(new BlockRegistry.UnknownBlockTypeException("unknown_type", java.util.Set.of()));

        ExecutionResult result = engine.execute(flow, Map.of());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("unknown_type");
    }

    @Test
    @DisplayName("Deve retornar status ERROR quando bloco lança exceção em tempo de execução")
    void shouldReturnErrorWhenBlockThrows() {
        Flow flow = new Flow("f7", "F7", "b1", List.of(
                new BlockDefinition("b1", "bad_block", Map.of(), Map.of())
        ));

        when(blockRegistry.resolve("bad_block"))
                .thenReturn((def, ctx) -> { throw new RuntimeException("falha interna"); });

        ExecutionResult result = engine.execute(flow, Map.of());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("falha interna");
    }

    @Test
    @DisplayName("Deve incluir duração da execução no resultado")
    void shouldIncludeExecutionDuration() {
        Flow flow = new Flow("f8", "F8", "b1", List.of(
                new BlockDefinition("b1", "output", Map.of(), Map.of())
        ));

        when(blockRegistry.resolve("output")).thenReturn(outputBlock("x"));

        ExecutionResult result = engine.execute(flow, Map.of());

        assertThat(result.getDuration()).isNotNull();
        assertThat(result.getDuration().toMillis()).isGreaterThanOrEqualTo(0);
    }
}
