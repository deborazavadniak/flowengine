package com.serasa.flowengine.api;

import com.serasa.flowengine.api.dto.CreateFlowRequest;
import com.serasa.flowengine.api.dto.ExecuteFlowRequest;
import com.serasa.flowengine.api.dto.Responses;
import com.serasa.flowengine.domain.engine.ExecutionResult;
import com.serasa.flowengine.domain.engine.FlowEngine;
import com.serasa.flowengine.domain.model.BlockDefinition;
import com.serasa.flowengine.domain.model.Flow;
import com.serasa.flowengine.domain.port.FlowRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import java.util.List;

@RestController
@RequestMapping("/api/flows")
@Tag(name = "Flow Engine", description = "Criação e execução de fluxos dinâmicos")
public class FlowController {

    private final FlowRepository flowRepository;
    private final FlowEngine flowEngine;

    public FlowController(FlowRepository flowRepository, FlowEngine flowEngine) {
        this.flowRepository = flowRepository;
        this.flowEngine = flowEngine;
    }

    // ── POST /api/flows ───────────────────────────────────────────────────────

    @PostMapping
    @Operation(
            summary = "Criar um fluxo",
            description = "Recebe a especificação JSON do fluxo, valida e armazena em memória.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Fluxo criado com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Especificação inválida"),
                    @ApiResponse(responseCode = "409", description = "ID já existe")
            }
    )
    public ResponseEntity<Responses.CreateFlowResponse> createFlow(
            @Valid @RequestBody CreateFlowRequest request) {

        if (flowRepository.existsById(request.id())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        List<BlockDefinition> blockDefs = request.blocks().stream()
                .map(b -> new BlockDefinition(b.id(), b.type(), b.config(), b.routes()))
                .toList();

        Flow flow = new Flow(request.id(), request.name(), request.startBlockId(), blockDefs);
        flowRepository.save(flow);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new Responses.CreateFlowResponse(
                        flow.getId(),
                        flow.getName(),
                        flow.blockCount(),
                        flow.getCreatedAt(),
                        "Fluxo criado com sucesso."
                )
        );
    }

    // ── POST /api/flows/{id}/execute ──────────────────────────────────────────

    @PostMapping("/{id}/execute")
    @Operation(
            summary = "Executar um fluxo",
            description = "Recebe o ID do fluxo e parâmetros de entrada, executa passo a passo e retorna o resultado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Execução concluída (verifique 'status' no body)"),
                    @ApiResponse(responseCode = "404", description = "Fluxo não encontrado")
            }
    )
    public ResponseEntity<Responses.ExecuteFlowResponse> executeFlow(
            @Parameter(description = "ID do fluxo a executar", example = "prime-check")
            @PathVariable String id,
            @Valid @RequestBody ExecuteFlowRequest request) {

        Flow flow = flowRepository.findById(id)
                .orElseThrow(() -> new FlowNotFoundException(id));

        ExecutionResult result = flowEngine.execute(flow, request.input());

        return ResponseEntity.ok(Responses.ExecuteFlowResponse.from(id, result));
    }

    // ── GET /api/flows ────────────────────────────────────────────────────────

    @GetMapping
    @Operation(
            summary = "Listar fluxos com paginação",
            description = "Parâmetros: page (default 0), size (default 10), sort (ex: name,asc)"
    )
    public ResponseEntity<Page<Responses.CreateFlowResponse>> listFlows(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {

        List<Responses.CreateFlowResponse> all = flowRepository.findAll().stream()
                .map(f -> new Responses.CreateFlowResponse(
                        f.getId(), f.getName(), f.blockCount(), f.getCreatedAt(), null))
                .toList();

        // Paginação manual sobre a lista em memória
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<Responses.CreateFlowResponse> pageContent =
                start > all.size() ? List.of() : all.subList(start, end);

        return ResponseEntity.ok(new PageImpl<>(pageContent, pageable, all.size()));
    }

    // ── DELETE /api/flows/{id} ────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover um fluxo")
    public ResponseEntity<Void> deleteFlow(@PathVariable String id) {
        if (!flowRepository.existsById(id)) {
            throw new FlowNotFoundException(id);
        }
        flowRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Exceção local ─────────────────────────────────────────────────────────

    public static class FlowNotFoundException extends RuntimeException {
        public FlowNotFoundException(String id) {
            super("Fluxo '%s' não encontrado.".formatted(id));
        }
    }
}