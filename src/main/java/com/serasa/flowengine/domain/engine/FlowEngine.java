package com.serasa.flowengine.domain.engine;


import com.serasa.flowengine.domain.model.BlockDefinition;
import com.serasa.flowengine.domain.model.ExecutionContext;
import com.serasa.flowengine.domain.model.Flow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Orquestra a execução de um fluxo passo a passo.
 *
 * Algoritmo:
 *  1. Obtém o bloco inicial do fluxo
 *  2. Resolve o executor via BlockRegistry
 *  3. Executa o bloco → recebe a chave de rota
 *  4. Usa a rota para obter o ID do próximo bloco
 *  5. Repete até nextBlockId == null (fim do fluxo)
 *
 * A engine não conhece nenhum tipo de bloco — apenas o contrato Block.
 * É pura lógica de loop e roteamento.
 */
public class FlowEngine {

    private static final Logger log = LoggerFactory.getLogger(FlowEngine.class);
    private static final int MAX_STEPS = 1000; // proteção contra loops infinitos

    private final BlockRegistry blockRegistry;

    public FlowEngine(BlockRegistry blockRegistry) {
        this.blockRegistry = blockRegistry;
    }

    /**
     * Executa um fluxo com os parâmetros de entrada fornecidos.
     *
     * @param flow   fluxo a executar
     * @param input  parâmetros de entrada que serão injetados no contexto inicial
     * @return resultado completo da execução
     */
    public ExecutionResult execute(Flow flow, Map<String, Object> input) {
        Instant start = Instant.now();
        ExecutionContext context = new ExecutionContext(input);
        List<ExecutionResult.StepRecord> steps = new ArrayList<>();

        log.info("Iniciando execução do fluxo '{}' com entrada: {}", flow.getId(), input);

        try {
            BlockDefinition currentBlock = flow.getStartBlock();
            int stepNumber = 0;

            while (currentBlock != null) {

                if (stepNumber >= MAX_STEPS) {
                    throw new FlowExecutionException(
                            "Limite de %d passos excedido. Possível loop infinito no fluxo '%s'."
                                    .formatted(MAX_STEPS, flow.getId()));
                }

                log.debug("Passo {}: executando bloco '{}' (tipo={})",
                        stepNumber, currentBlock.id(), currentBlock.type());

                // Resolve e executa o bloco
                Block executor = blockRegistry.resolve(currentBlock.type());
                String routeKey = executor.execute(currentBlock, context);

                // Determina próximo bloco pela rota retornada
                String nextBlockId = (routeKey != null)
                        ? currentBlock.nextBlockId(routeKey)
                        : null;

                // Registra o passo para auditoria
                steps.add(new ExecutionResult.StepRecord(
                        stepNumber,
                        currentBlock.id(),
                        currentBlock.type(),
                        routeKey,
                        nextBlockId,
                        context.snapshot()
                ));

                log.debug("Bloco '{}' concluído → rota='{}' → próximo='{}'",
                        currentBlock.id(), routeKey, nextBlockId);

                // Avança para o próximo bloco
                currentBlock = (nextBlockId != null) ? flow.getBlock(nextBlockId) : null;
                stepNumber++;
            }

            Object output = context.has("output") ? context.get("output").orElse(null) : null;

            log.info("Fluxo '{}' concluído em {} passos. Output: {}",
                    flow.getId(), stepNumber, output);

            return ExecutionResult.builder()
                    .success(output)
                    .steps(steps)
                    .context(context)
                    .duration(start)
                    .build();

        } catch (Exception ex) {
            log.error("Erro na execução do fluxo '{}': {}", flow.getId(), ex.getMessage());
            return ExecutionResult.builder()
                    .error(ex.getMessage())
                    .steps(steps)
                    .context(context)
                    .duration(start)
                    .build();
        }
    }

    // ── Exceção ───────────────────────────────────────────────────────────────

    public static class FlowExecutionException extends RuntimeException {
        public FlowExecutionException(String message) {
            super(message);
        }
    }
}