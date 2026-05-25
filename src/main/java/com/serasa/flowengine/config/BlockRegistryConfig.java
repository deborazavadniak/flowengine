package com.serasa.flowengine.config;


import com.serasa.flowengine.blocks.*;
import com.serasa.flowengine.domain.engine.Block;
import com.serasa.flowengine.domain.engine.BlockRegistry;
import com.serasa.flowengine.domain.engine.FlowEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Configuração central da engine.
 *
 * Para adicionar um novo tipo de bloco:
 *   1. Crie a classe em /blocks implementando Block
 *   2. Adicione uma entrada no Map abaixo
 *   → Zero mudanças na FlowEngine (Open/Closed Principle)
 */
@Configuration
public class BlockRegistryConfig {

    @Bean
    public BlockRegistry blockRegistry() {
        Map<String, Block> blocks = Map.of(
                "input",        new InputBlock(),
                "condition",    new ConditionBlock(),
                "math",         new MathBlock(),
                "set_variable", new SetVariableBlock(),
                "output",       new OutputBlock()
        );
        return new BlockRegistry(blocks);
    }

    @Bean
    public FlowEngine flowEngine(BlockRegistry blockRegistry) {
        return new FlowEngine(blockRegistry);
    }
}