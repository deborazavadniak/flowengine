package com.serasa.flowengine.blocks;

import com.serasa.flowengine.domain.engine.Block;
import com.serasa.flowengine.domain.model.BlockDefinition;
import com.serasa.flowengine.domain.model.ExecutionContext;

/**
 * Bloco matemático. Realiza uma operação aritmética e armazena o resultado
 * em uma variável do contexto.
 *
 * Config obrigatória:
 *   - "target"    : variável onde o resultado será armazenado
 *   - "operation" : operação a realizar (+, -, *, /, %, set)
 *   - "operand"   : valor literal ou nome de variável
 *
 * Exemplos:
 * <pre>
 * // divisor = 3   (set inicial)
 * { "target": "divisor", "operation": "set", "operand": "3" }
 *
 * // divisor += 2  (incremento no loop)
 * { "target": "divisor", "operation": "+", "operand": "2" }
 * </pre>
 */
public class MathBlock implements Block {

    @Override
    public String execute(BlockDefinition definition, ExecutionContext context) {
        String target    = definition.configString("target");
        String operation = definition.configString("operation");
        String operandStr = definition.configString("operand");

        long operand = resolveAsLong(operandStr, context);

        long result = switch (operation) {
            case "set" -> operand;
            case "+"   -> context.getLong(target) + operand;
            case "-"   -> context.getLong(target) - operand;
            case "*"   -> context.getLong(target) * operand;
            case "/"   -> {
                if (operand == 0) throw new ArithmeticException("Divisão por zero no bloco '%s'".formatted(definition.id()));
                yield context.getLong(target) / operand;
            }
            case "%"   -> context.getLong(target) % operand;
            default -> throw new IllegalArgumentException(
                    "Operação '%s' não suportada pelo MathBlock.".formatted(operation));
        };

        context.set(target, result);
        return "next";
    }

    private long resolveAsLong(String token, ExecutionContext context) {
        try {
            return Long.parseLong(token);
        } catch (NumberFormatException e) {
            return context.getLong(token);
        }
    }
}