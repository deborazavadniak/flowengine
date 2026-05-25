package com.serasa.flowengine.infra;


import com.serasa.flowengine.domain.model.Flow;
import com.serasa.flowengine.domain.port.FlowRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementação em memória do FlowRepository.
 *
 * Thread-safe via ConcurrentHashMap.
 * Para produção, basta criar uma JpaFlowRepository implementando
 * o mesmo FlowRepository port — zero impacto no domínio.
 */
@Repository
public class InMemoryFlowRepository implements FlowRepository {

    private final ConcurrentHashMap<String, Flow> store = new ConcurrentHashMap<>();

    @Override
    public void save(Flow flow) {
        store.put(flow.getId(), flow);
    }

    @Override
    public Optional<Flow> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Flow> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }

    @Override
    public boolean existsById(String id) {
        return store.containsKey(id);
    }
}