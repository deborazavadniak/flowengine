package com.serasa.flowengine.integration;


import com.serasa.flowengine.blocks.*;
import com.serasa.flowengine.domain.engine.BlockRegistry;
import com.serasa.flowengine.domain.engine.ExecutionResult;
import com.serasa.flowengine.domain.engine.FlowEngine;
import com.serasa.flowengine.domain.model.BlockDefinition;
import com.serasa.flowengine.domain.model.Flow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Teste de integração end-to-end do fluxo primo.
 *
 * Usa @Spy nos blocos reais para garantir que a lógica de negócio
 * foi exercida, podendo verificar interações quando necessário.
 * A FlowEngine é instanciada com um BlockRegistry real (sem mock)
 * porque o objetivo aqui é validar a composição completa dos blocos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Fluxo Primo — integração end-to-end")
class PrimeFlowIntegrationTest {

    @Spy InputBlock     inputBlock     = new InputBlock();
    @Spy ConditionBlock conditionBlock = new ConditionBlock();
    @Spy MathBlock      mathBlock      = new MathBlock();
    @Spy SetVariableBlock setVariableBlock = new SetVariableBlock();
    @Spy OutputBlock    outputBlock    = new OutputBlock();

    private FlowEngine engine;
    private Flow primeFlow;

    @BeforeEach
    void setUp() {
        BlockRegistry registry = new BlockRegistry(Map.of(
                "input",        inputBlock,
                "condition",    conditionBlock,
                "math",         mathBlock,
                "set_variable", setVariableBlock,
                "output",       outputBlock
        ));
        engine = new FlowEngine(registry);
        primeFlow = buildPrimeFlow();
    }

    // ── casos parametrizados ──────────────────────────────────────────────────

    @ParameterizedTest(name = "n={0} → {1}")
    @CsvSource({
            // n <= 1 (não são primos)
            "-5,  Não é primo",
            "0,   Não é primo",
            "1,   Não é primo",
            // caso especial: 2 é o único primo par
            "2,   É primo",
            // pares maiores que 2 (não são primos)
            "4,   Não é primo",
            "100, Não é primo",
            // primos ímpares
            "3,   É primo",
            "5,   É primo",
            "7,   É primo",
            "11,  É primo",
            "13,  É primo",
            "17,  É primo",
            "19,  É primo",
            "23,  É primo",
            "97,  É primo",
            "101, É primo",
            // compostos ímpares
            "9,   Não é primo",
            "15,  Não é primo",
            "25,  Não é primo",
            "49,  Não é primo",
    })
    @DisplayName("Deve identificar corretamente se o número é primo")
    void shouldIdentifyPrimes(long number, String expected) {
        ExecutionResult result = engine.execute(primeFlow, Map.of("number", number));

        assertThat(result.isSuccess())
                .as("Execução para n=%d deve ter sucesso", number)
                .isTrue();

        assertThat(result.getOutput())
                .as("Output para n=%d deve ser '%s'", number, expected.trim())
                .isEqualTo(expected.trim());
    }

    // ── verificações de interação com @Spy ────────────────────────────────────

    @ParameterizedTest(name = "n={0}: inputBlock deve ser invocado exatamente 1x")
    @CsvSource({ "2", "17", "4" })
    @DisplayName("InputBlock deve ser chamado exatamente uma vez por execução")
    void inputBlockShouldBeCalledOnce(long number) {
        engine.execute(primeFlow, Map.of("number", number));

        verify(inputBlock, times(1)).execute(any(), any());
    }

    @ParameterizedTest(name = "n={0}: outputBlock deve ser invocado exatamente 1x")
    @CsvSource({ "1", "2", "17", "9" })
    @DisplayName("OutputBlock deve ser chamado exatamente uma vez por execução")
    void outputBlockShouldBeCalledOnce(long number) {
        engine.execute(primeFlow, Map.of("number", number));

        verify(outputBlock, times(1)).execute(any(), any());
    }

    @ParameterizedTest(name = "n={0}: mathBlock não deve ser chamado")
    @CsvSource({ "-1", "0", "1", "2", "4" })
    @DisplayName("MathBlock não deve ser chamado para casos resolvidos antes do loop")
    void mathBlockShouldNotBeCalledForEarlyExit(long number) {
        engine.execute(primeFlow, Map.of("number", number));

        verify(mathBlock, never()).execute(any(), any());
    }

    // ── construção do fluxo primo ─────────────────────────────────────────────

    private Flow buildPrimeFlow() {
        return new Flow("prime-check", "Verificação de Número Primo", "b1", List.of(
                new BlockDefinition("b1", "input",
                        Map.of("inputKey", "number", "outputVar", "n"),
                        Map.of("next", "b2")),

                new BlockDefinition("b2", "condition",
                        Map.of("left", "n", "operator", "<=", "right", "1"),
                        Map.of("true", "b-not-prime", "false", "b3")),

                new BlockDefinition("b3", "condition",
                        Map.of("left", "n", "operator", "==", "right", "2"),
                        Map.of("true", "b-prime", "false", "b4")),

                new BlockDefinition("b4", "condition",
                        Map.of("left", "n", "operator", "%", "right", "2"),
                        Map.of("true", "b-not-prime", "false", "b5")),

                new BlockDefinition("b5", "set_variable",
                        Map.of("key", "divisor", "value", "3"),
                        Map.of("next", "b6")),

                new BlockDefinition("b6", "condition",
                        Map.of("left", "divisor", "operator", "pow2_lte", "right", "n"),
                        Map.of("true", "b7", "false", "b-prime")),

                new BlockDefinition("b7", "condition",
                        Map.of("left", "n", "operator", "%", "right", "divisor"),
                        Map.of("true", "b-not-prime", "false", "b8")),

                new BlockDefinition("b8", "math",
                        Map.of("target", "divisor", "operation", "+", "operand", "2"),
                        Map.of("next", "b6")),

                new BlockDefinition("b-prime", "output",
                        Map.of("value", "É primo"),
                        Map.of()),

                new BlockDefinition("b-not-prime", "output",
                        Map.of("value", "Não é primo"),
                        Map.of())
        ));
    }
}