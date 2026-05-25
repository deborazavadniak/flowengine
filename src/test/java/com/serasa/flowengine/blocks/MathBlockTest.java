package com.serasa.flowengine.blocks;


import com.serasa.flowengine.domain.model.BlockDefinition;
import com.serasa.flowengine.domain.model.ExecutionContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MathBlock — operações aritméticas no contexto")
class MathBlockTest {

    @InjectMocks
    MathBlock mathBlock;

    // ── helper ────────────────────────────────────────────────────────────────

    private BlockDefinition def(String target, String op, String operand) {
        return new BlockDefinition("b", "math",
                Map.of("target", target, "operation", op, "operand", operand),
                Map.of("next", "next"));
    }

    // ── set ───────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("set: deve escrever valor literal na variável do contexto")
    void testSet() {
        ExecutionContext ctx = new ExecutionContext();

        mathBlock.execute(def("divisor", "set", "3"), ctx);

        assertThat(ctx.getLong("divisor")).isEqualTo(3L);
    }

    @Test
    @DisplayName("set: deve sobrescrever variável que já existe no contexto")
    void testSetOverwrite() {
        ExecutionContext ctx = new ExecutionContext(Map.of("divisor", 99L));

        mathBlock.execute(def("divisor", "set", "3"), ctx);

        assertThat(ctx.getLong("divisor")).isEqualTo(3L);
    }

    // ── + ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("+: deve somar operando ao valor atual da variável")
    void testAdd() {
        ExecutionContext ctx = new ExecutionContext(Map.of("divisor", 3L));

        mathBlock.execute(def("divisor", "+", "2"), ctx);

        assertThat(ctx.getLong("divisor")).isEqualTo(5L);
    }

    // ── - ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("-: deve subtrair operando do valor atual da variável")
    void testSubtract() {
        ExecutionContext ctx = new ExecutionContext(Map.of("x", 10L));

        mathBlock.execute(def("x", "-", "3"), ctx);

        assertThat(ctx.getLong("x")).isEqualTo(7L);
    }

    // ── * ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("*: deve multiplicar variável pelo operando")
    void testMultiply() {
        ExecutionContext ctx = new ExecutionContext(Map.of("x", 4L));

        mathBlock.execute(def("x", "*", "3"), ctx);

        assertThat(ctx.getLong("x")).isEqualTo(12L);
    }

    // ── / ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("/: deve dividir variável pelo operando")
    void testDivide() {
        ExecutionContext ctx = new ExecutionContext(Map.of("x", 10L));

        mathBlock.execute(def("x", "/", "2"), ctx);

        assertThat(ctx.getLong("x")).isEqualTo(5L);
    }

    @Test
    @DisplayName("/: deve lançar ArithmeticException ao dividir por zero")
    void testDivideByZero() {
        ExecutionContext ctx = new ExecutionContext(Map.of("x", 10L));

        assertThatThrownBy(() -> mathBlock.execute(def("x", "/", "0"), ctx))
                .isInstanceOf(ArithmeticException.class);
    }

    // ── % ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("%: deve calcular o resto da divisão e armazenar na variável")
    void testModulo() {
        ExecutionContext ctx = new ExecutionContext(Map.of("x", 10L));

        mathBlock.execute(def("x", "%", "3"), ctx);

        assertThat(ctx.getLong("x")).isEqualTo(1L);
    }

    // ── rota de saída ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve sempre retornar a rota 'next' independente da operação")
    void testAlwaysReturnsNext() {
        ExecutionContext ctx = new ExecutionContext(Map.of("x", 1L));

        String route = mathBlock.execute(def("x", "+", "1"), ctx);

        assertThat(route).isEqualTo("next");
    }

    // ── operação inválida ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve lançar IllegalArgumentException para operação desconhecida")
    void testInvalidOperation() {
        ExecutionContext ctx = new ExecutionContext(Map.of("x", 1L));

        assertThatThrownBy(() -> mathBlock.execute(def("x", "^", "2"), ctx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("^");
    }
}
