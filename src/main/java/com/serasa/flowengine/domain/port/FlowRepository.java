package com.serasa.flowengine.domain.port;


import com.serasa.flowengine.domain.model.Flow;

import java.util.List;
import java.util.Optional;

/**
 * Port de saída para persistência de fluxos.
 *
 * Seguindo arquitetura hexagonal, o domínio define a interface —
 * a infra fornece a implementação (InMemory, JPA, Redis, etc.)
 * sem que o domínio saiba qual está em uso.
 */
public interface FlowRepository {

    /** Persiste um fluxo. Sobrescreve se o ID já existir. */
    void save(Flow flow);

    /** Busca por ID. Retorna vazio se não existir. */
    Optional<Flow> findById(String id);

    /** Lista todos os fluxos armazenados. */
    List<Flow> findAll();

    /** Remove um fluxo pelo ID. */
    void deleteById(String id);

    /** Verifica existência sem carregar o objeto completo. */
    boolean existsById(String id);
}