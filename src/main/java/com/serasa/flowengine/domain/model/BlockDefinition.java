package com.serasa.flowengine.domain.model;

import java.util.Collections;
import java.util.Map;

/**
 * Descrição declarativa de um bloco dentro de um fluxo.
 * É um dado puro — sem lógica de execução.
 *
 * <pre>
 * {
 *   "id":     "b1",
 *   "type":   "condition",
 *   "config": { "expression": "n <= 1" },
 *   "routes": { "true": "b-out-false", "false": "b3" }
 * }
 * </pre>
 */
public record BlockDefinition(
        String id,
        String type,
        Map<String, Object> config,
        Map<String, String> routes
) {

    public BlockDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("BlockDefinition.id não pode ser vazio");
        if (type == null || type.isBlank()) throw new IllegalArgumentException("BlockDefinition.type não pode ser vazio");
        config = config != null ? Collections.unmodifiableMap(config) : Map.of();
        routes = routes != null ? Collections.unmodifiableMap(routes) : Map.of();
    }

    /**
     * Retorna o ID do próximo bloco para uma rota específica.
     * Retorna null quando a rota não existe (fim do fluxo).
     */
    public String nextBlockId(String routeKey) {
        return routes.get(routeKey);
    }

    public String configString(String key) {
        Object val = config.get(key);
        if (val == null) throw new IllegalStateException(
                "Bloco '%s' não possui config '%s'".formatted(id, key));
        return val.toString();
    }

    public boolean hasConfig(String key) {
        return config.containsKey(key);
    }
}