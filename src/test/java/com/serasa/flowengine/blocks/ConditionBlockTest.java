package com.serasa.flowengine.blocks;


import com.serasa.flowengine.domain.model.BlockDefinition;
import com.serasa.flowengine.domain.model.ExecutionContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConditionBlock — avaliação de expressões booleanas")
class ConditionBlockTest {

    /*
     * ConditionBlock não tem dependências injetáveis — o @InjectMocks aqui
     * serve para documentar a classe sob teste e manter o padrão do projeto.
     * Caso futuramente uma dependência seja adicionada, @Mock + @InjectMocks
     * já estarão prontos para recebê-la.
     */
    @InjectMocks
    ConditionBlock conditionBlock;

    // ── helpers ───────────────────────────────────────────────────────────────

    private BlockDefinition def(String left, String operator, String right) {
        return new BlockDefinition("b", "condition",
                Map.of("left", left, "operator", operator, "right", right),
                Map.of("true", "yes", "false", "no"));
    }

    private ExecutionContext ctx(long n) {
        return new ExecutionContext(Map.of("n", n, "divisor", 3L));
    }

    // ── operador <= ───────────────────────────────────────────────────────────

    @ParameterizedTest(name = "n={0} <= 1 → {1}")
    @CsvSource({ "1,true", "0,true", "-5,true", "2,false", "17,false" })
    @DisplayName("Operador <= : deve retornar true quando n menor ou igual ao limite")
    void testLte(long n, String expected) {
        assertThat(conditionBlock.execute(def("n", "<=", "1"), ctx(n)))
                .isEqualTo(expected);
    }

    // ── operador == ───────────────────────────────────────────────────────────

    @ParameterizedTest(name = "n={0} == 2 → {1}")
    @CsvSource({ "2,true", "3,false", "0,false" })
    @DisplayName("Operador == : deve retornar true apenas quando valores são iguais")
    void testEq(long n, String expected) {
        assertThat(conditionBlock.execute(def("n", "==", "2"), ctx(n)))
                .isEqualTo(expected);
    }

    // ── operador != ───────────────────────────────────────────────────────────

    @ParameterizedTest(name = "n={0} != 2 → {1}")
    @CsvSource({ "2,false", "3,true", "0,true" })
    @DisplayName("Operador != : deve retornar true quando valores são diferentes")
    void testNeq(long n, String expected) {
        assertThat(conditionBlock.execute(def("n", "!=", "2"), ctx(n)))
                .isEqualTo(expected);
    }

    // ── operador > ────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "n={0} > 10 → {1}")
    @CsvSource({ "11,true", "10,false", "9,false" })
    @DisplayName("Operador > : deve retornar true quando n maior que o limite")
    void testGt(long n, String expected) {
        assertThat(conditionBlock.execute(def("n", ">", "10"), ctx(n)))
                .isEqualTo(expected);
    }

    // ── operador % (divisibilidade) ────────────────────────────────────────────

    @ParameterizedTest(name = "n={0} % 2 → divisível={1}")
    @CsvSource({ "4,true", "6,true", "9,false", "17,false" })
    @DisplayName("Operador % : deve retornar true quando n é divisível pelo operando")
    void testModulo(long n, String expected) {
        assertThat(conditionBlock.execute(def("n", "%", "2"), ctx(n)))
                .isEqualTo(expected);
    }

    // ── operador pow2_lte (loop do crivo) ─────────────────────────────────────

    @Test
    @DisplayName("pow2_lte: deve retornar true quando divisor² <= n (loop continua)")
    void testPow2LteTrue() {
        // divisor=3, n=10 → 9 <= 10 → true (loop continua)
        ExecutionContext ctx = new ExecutionContext(Map.of("n", 10L, "divisor", 3L));
        assertThat(conditionBlock.execute(def("divisor", "pow2_lte", "n"), ctx))
                .isEqualTo("true");
    }

    @Test
    @DisplayName("pow2_lte: deve retornar false quando divisor² > n (loop encerra)")
    void testPow2LteFalse() {
        // divisor=4, n=10 → 16 > 10 → false (sai do loop → é primo)
        ExecutionContext ctx = new ExecutionContext(Map.of("n", 10L, "divisor", 4L));
        assertThat(conditionBlock.execute(def("divisor", "pow2_lte", "n"), ctx))
                .isEqualTo("false");
    }

    // ── casos de erro ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve lançar IllegalArgumentException para operador não suportado")
    void testInvalidOperator() {
        assertThatThrownBy(() -> conditionBlock.execute(def("n", "???", "1"), ctx(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("???");
    }

    @Test
    @DisplayName("Deve lançar MissingVariableException quando variável não existe no contexto")
    void testMissingVariable() {
        ExecutionContext emptyCtx = new ExecutionContext();
        assertThatThrownBy(() -> conditionBlock.execute(def("variavel_inexistente", "==", "1"), emptyCtx))
                .isInstanceOf(ExecutionContext.MissingVariableException.class)
                .hasMessageContaining("variavel_inexistente");
    }
}
