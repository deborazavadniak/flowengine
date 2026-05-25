package com.serasa.flowengine.domain.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Carrega o estado de uma execução de fluxo.
 * Viaja de bloco em bloco, acumulando variáveis.
 * Cada bloco lê e/ou escreve no contexto sem conhecer os demais.
 */
public class ExecutionContext {

    private final Map<String, Object> variables;

    public ExecutionContext() {
        this.variables = new HashMap<>();
    }

    /** Cópia defensiva — útil para auditoria e replay. */
    public ExecutionContext(Map<String, Object> initial) {
        this.variables = new HashMap<>(initial);
    }

    // ── Escrita ──────────────────────────────────────────────────────────────

    public ExecutionContext set(String key, Object value) {
        variables.put(key, value);
        return this;
    }

    // ── Leitura tipada ───────────────────────────────────────────────────────

    public Optional<Object> get(String key) {
        return Optional.ofNullable(variables.get(key));
    }

    public String getString(String key) {
        return get(key)
                .map(Object::toString)
                .orElseThrow(() -> new MissingVariableException(key));
    }

    public long getLong(String key) {
        Object val = get(key).orElseThrow(() -> new MissingVariableException(key));
        if (val instanceof Number n) return n.longValue();
        return Long.parseLong(val.toString());
    }

    public boolean getBoolean(String key) {
        Object val = get(key).orElseThrow(() -> new MissingVariableException(key));
        if (val instanceof Boolean b) return b;
        return Boolean.parseBoolean(val.toString());
    }

    public boolean has(String key) {
        return variables.containsKey(key);
    }

    /** Snapshot imutável para log/auditoria. */
    public Map<String, Object> snapshot() {
        return Map.copyOf(variables);
    }

    @Override
    public String toString() {
        return "ExecutionContext" + variables;
    }

    // ── Exceção interna ───────────────────────────────────────────────────────

    public static class MissingVariableException extends RuntimeException {
        public MissingVariableException(String key) {
            super("Variável '%s' não encontrada no contexto de execução.".formatted(key));
        }
    }
}