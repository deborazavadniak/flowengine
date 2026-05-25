package com.serasa.flowengine.blocks;


import com.serasa.flowengine.domain.engine.Block;
import com.serasa.flowengine.domain.model.BlockDefinition;
import com.serasa.flowengine.domain.model.ExecutionContext;

/**
 * Bloco de entrada. Lê um parâmetro do contexto e o mapeia
 * para uma variável interna com o nome configurado.
 *
 * Config obrigatória:
 *   - "inputKey"  : chave do parâmetro de entrada (default: "input")
 *   - "outputVar" : nome da variável no contexto (default: igual ao inputKey)
 *
 * Rota de saída:
 *   - "next" : sempre avança para o próximo bloco
 *
 * Exemplo de config:
 * <pre>
 * {
 *   "inputKey":  "number",
 *   "outputVar": "n"
 * }
 * </pre>
 */
public class InputBlock implements Block {

    @Override
    public String execute(BlockDefinition definition, ExecutionContext context) {
        String inputKey  = definition.hasConfig("inputKey")  ? definition.configString("inputKey")  : "input";
        String outputVar = definition.hasConfig("outputVar") ? definition.configString("outputVar") : inputKey;

        Object value = context.get(inputKey)
                .orElseThrow(() -> new IllegalStateException(
                        "Parâmetro de entrada '%s' não encontrado no contexto.".formatted(inputKey)));

        // Converte para Long se possível (facilita operações matemáticas subsequentes)
        if (value instanceof String s) {
            try { value = Long.parseLong(s); } catch (NumberFormatException ignored) {}
        }

        context.set(outputVar, value);
        return "next";
    }
}