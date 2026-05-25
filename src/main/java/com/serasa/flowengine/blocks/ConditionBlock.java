package com.serasa.flowengine.blocks;


import com.serasa.flowengine.domain.engine.Block;
import com.serasa.flowengine.domain.model.BlockDefinition;
import com.serasa.flowengine.domain.model.ExecutionContext;

/**
 * Bloco de condição. Avalia uma expressão booleana simples contra
 * variáveis do contexto e retorna "true" ou "false" como chave de rota.
 *
 * Config obrigatória:
 *   - "left"     : nome da variável do lado esquerdo (ex: "n")
 *   - "operator" : operador de comparação (<=, <, >=, >, ==, !=, %)
 *   - "right"    : valor literal ou nome de variável do lado direito
 *
 * Operadores especiais:
 *   - "%"   : testa se left % right == 0  (divisibilidade)
 *   - "%!=" : testa se left % right != 0
 *
 * Exemplo — "n ≤ 1":
 * <pre>
 * { "left": "n", "operator": "<=", "right": "1" }
 * </pre>
 *
 * Exemplo — "divisor² ≤ n":
 * <pre>
 * { "left": "divisor", "operator": "pow2_lte", "right": "n" }
 * </pre>
 */
public class ConditionBlock implements Block {

    @Override
    public String execute(BlockDefinition definition, ExecutionContext context) {
        String leftVar   = definition.configString("left");
        String operator  = definition.configString("operator");
        String rightStr  = definition.configString("right");

        long leftVal  = resolveAsLong(leftVar, context);
        long rightVal = resolveAsLong(rightStr, context);

        boolean result = evaluate(leftVal, operator, rightVal);
        return Boolean.toString(result);
    }

    private long resolveAsLong(String token, ExecutionContext context) {
        // Tenta interpretar como literal numérico primeiro
        try {
            return Long.parseLong(token);
        } catch (NumberFormatException e) {
            // Não é literal — busca no contexto como variável
            return context.getLong(token);
        }
    }

    private boolean evaluate(long left, String operator, long right) {
        return switch (operator) {
            case "<="       -> left <= right;
            case "<"        -> left < right;
            case ">="       -> left >= right;
            case ">"        -> left > right;
            case "=="       -> left == right;
            case "!="       -> left != right;
            case "%"        -> left % right == 0;    // é divisível
            case "%!="      -> left % right != 0;    // não é divisível
            case "pow2_lte" -> left * left <= right; // divisor² ≤ n
            case "pow2_lt"  -> left * left < right;
            default -> throw new IllegalArgumentException(
                    "Operador '%s' não suportado pelo ConditionBlock.".formatted(operator));
        };
    }
}