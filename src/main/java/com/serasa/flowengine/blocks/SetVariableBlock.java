package com.serasa.flowengine.blocks;

import com.serasa.flowengine.domain.engine.Block;
import com.serasa.flowengine.domain.model.BlockDefinition;
import com.serasa.flowengine.domain.model.ExecutionContext;

/**
 * Bloco de atribuição de variável. Define uma variável no contexto
 * com um valor literal configurado estaticamente no fluxo.
 *
 * Config obrigatória:
 *   - "key"   : nome da variável a definir
 *   - "value" : valor literal (será interpretado como Long se possível)
 *
 * Exemplo — inicializar divisor = 3:
 * <pre>
 * { "key": "divisor", "value": "3" }
 * </pre>
 */
public class SetVariableBlock implements Block {

    @Override
    public String execute(BlockDefinition definition, ExecutionContext context) {
        String key   = definition.configString("key");
        String value = definition.configString("value");

        // Tenta converter para Long para facilitar operações matemáticas
        try {
            context.set(key, Long.parseLong(value));
        } catch (NumberFormatException e) {
            context.set(key, value);
        }

        return "next";
    }
}