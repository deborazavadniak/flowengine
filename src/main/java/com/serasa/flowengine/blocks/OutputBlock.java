package com.serasa.flowengine.blocks;

import com.serasa.flowengine.domain.engine.Block;
import com.serasa.flowengine.domain.model.BlockDefinition;
import com.serasa.flowengine.domain.model.ExecutionContext;

/**
 * Bloco de saída. Define o valor final da execução do fluxo.
 * Após este bloco, o fluxo termina (sem próximo bloco definido).
 *
 * Config:
 *   - "value"     : valor literal de saída (opcional)
 *   - "sourceVar" : nome de variável do contexto para usar como saída (opcional)
 *
 * Se nenhum dos dois estiver configurado, usa o conteúdo atual de "output" no contexto.
 *
 * Exemplos:
 * <pre>
 * // Saída literal
 * { "value": "É primo" }
 *
 * // Saída dinâmica a partir do contexto
 * { "sourceVar": "resultado" }
 * </pre>
 */
public class OutputBlock implements Block {

    @Override
    public String execute(BlockDefinition definition, ExecutionContext context) {
        Object outputValue;

        if (definition.hasConfig("value")) {
            outputValue = definition.configString("value");
        } else if (definition.hasConfig("sourceVar")) {
            String sourceVar = definition.configString("sourceVar");
            outputValue = context.get(sourceVar)
                    .orElseThrow(() -> new IllegalStateException(
                            "sourceVar '%s' não encontrada no contexto.".formatted(sourceVar)));
        } else {
            // Fallback: usa o que já estiver em "output" no contexto
            outputValue = context.get("output").orElse(null);
        }

        context.set("output", outputValue);

        // Retorna null → nenhuma rota → fim do fluxo
        return null;
    }
}