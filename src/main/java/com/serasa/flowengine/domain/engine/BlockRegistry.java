package com.serasa.flowengine.domain.engine;


import com.serasa.flowengine.config.BlockRegistryConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Catálogo de todos os tipos de bloco disponíveis na engine.
 *
 * Cada tipo é registrado com um nome (string) e uma instância de {@link Block}.
 * Como Block é stateless, a mesma instância é reutilizada entre execuções.
 *
 * Para adicionar um novo tipo de bloco, basta registrá-lo aqui via
 * {@link BlockRegistryConfig} — zero mudanças na FlowEngine (Open/Closed).
 */
public class BlockRegistry {

    private final Map<String, Block> registry;

    public BlockRegistry(Map<String, Block> blocks) {
        this.registry = Collections.unmodifiableMap(new HashMap<>(blocks));
    }

    /**
     * Resolve o bloco executável para um tipo declarado.
     *
     * @throws UnknownBlockTypeException se o tipo não estiver registrado
     */
    public Block resolve(String type) {
        Block block = registry.get(type);
        if (block == null) {
            throw new UnknownBlockTypeException(type, registry.keySet());
        }
        return block;
    }

    public boolean supports(String type) {
        return registry.containsKey(type);
    }

    public Set<String> registeredTypes() {
        return registry.keySet();
    }

    // ── Exceção ───────────────────────────────────────────────────────────────

    public static class UnknownBlockTypeException extends RuntimeException {
        public UnknownBlockTypeException(String type, Set<String> known) {
            super("Tipo de bloco '%s' não registrado. Tipos disponíveis: %s".formatted(type, known));
        }
    }
}