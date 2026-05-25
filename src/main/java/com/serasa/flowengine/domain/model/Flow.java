package com.serasa.flowengine.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Agregado raiz. Representa um fluxo completo com todos os seus blocos.
 * Imutável após a criação.
 */
public class Flow {

    private final String id;
    private final String name;
    private final String startBlockId;
    private final Map<String, BlockDefinition> blocks;   // id → definição
    private final Instant createdAt;

    public Flow(String id, String name, String startBlockId, List<BlockDefinition> blocks) {
        if (id == null || id.isBlank())          throw new IllegalArgumentException("Flow.id não pode ser vazio");
        if (startBlockId == null || startBlockId.isBlank()) throw new IllegalArgumentException("Flow.startBlockId não pode ser vazio");
        if (blocks == null || blocks.isEmpty())  throw new IllegalArgumentException("Flow deve ter ao menos um bloco");

        this.id = id;
        this.name = name != null ? name : id;
        this.startBlockId = startBlockId;
        this.createdAt = Instant.now();
        this.blocks = blocks.stream()
                .collect(Collectors.toUnmodifiableMap(BlockDefinition::id, Function.identity()));

        validate();
    }

    // ── Consultas ────────────────────────────────────────────────────────────

    public String getId()          { return id; }
    public String getName()        { return name; }
    public String getStartBlockId(){ return startBlockId; }
    public Instant getCreatedAt()  { return createdAt; }

    public BlockDefinition getStartBlock() {
        return getBlock(startBlockId);
    }

    public BlockDefinition getBlock(String blockId) {
        return Optional.ofNullable(blocks.get(blockId))
                .orElseThrow(() -> new BlockNotFoundException(id, blockId));
    }

    public boolean hasBlock(String blockId) {
        return blocks.containsKey(blockId);
    }

    public int blockCount() {
        return blocks.size();
    }

    // ── Validação interna ────────────────────────────────────────────────────

    private void validate() {
        if (!blocks.containsKey(startBlockId)) {
            throw new IllegalArgumentException(
                    "startBlockId '%s' não corresponde a nenhum bloco definido.".formatted(startBlockId));
        }
        // Verifica que todas as rotas apontam para blocos existentes (ou null = fim)
        blocks.values().forEach(block ->
                block.routes().forEach((route, targetId) -> {
                    if (targetId != null && !blocks.containsKey(targetId)) {
                        throw new IllegalArgumentException(
                                "Bloco '%s' rota '%s' aponta para '%s' que não existe no fluxo."
                                        .formatted(block.id(), route, targetId));
                    }
                })
        );
    }

    // ── Exceção ───────────────────────────────────────────────────────────────

    public static class BlockNotFoundException extends RuntimeException {
        public BlockNotFoundException(String flowId, String blockId) {
            super("Bloco '%s' não encontrado no fluxo '%s'.".formatted(blockId, flowId));
        }
    }
}