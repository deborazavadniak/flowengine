package com.serasa.flowengine.domain.engine;


import com.serasa.flowengine.domain.model.BlockDefinition;
import com.serasa.flowengine.domain.model.ExecutionContext;

/**
 * Contrato de um bloco executável.
 *
 * Cada implementação:
 *  - recebe a definição declarativa do bloco (config, routes)
 *  - lê/escreve no ExecutionContext
 *  - retorna a chave de rota que determina o próximo bloco
 *
 * Retornar null ou uma rota que não existe em BlockDefinition.routes()
 * sinaliza fim de fluxo.
 *
 * Implementações DEVEM ser stateless — o estado vive no ExecutionContext.
 */
@FunctionalInterface
public interface Block {

    /**
     * Executa a operação atômica do bloco.
     *
     * @param definition a definição declarativa deste bloco (config + routes)
     * @param context    o contexto de execução compartilhado do fluxo
     * @return chave de rota para o próximo bloco, ou null para encerrar o fluxo
     */
    String execute(BlockDefinition definition, ExecutionContext context);
}