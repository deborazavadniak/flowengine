package com.serasa.flowengine.api.dto;

import com.serasa.flowengine.domain.engine.ExecutionResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class Responses {

    @Schema(description = "Resposta de criação de fluxo")
    public record CreateFlowResponse(
            @Schema(description = "ID do fluxo criado") String id,
            @Schema(description = "Nome do fluxo") String name,
            @Schema(description = "Quantidade de blocos") int blockCount,
            @Schema(description = "Timestamp de criação") Instant createdAt,
            @Schema(description = "Mensagem de status") String message
    ) {}

    @Schema(description = "Resposta de execução de fluxo")
    public record ExecuteFlowResponse(
            @Schema(description = "ID do fluxo executado") String flowId,
            @Schema(description = "Status da execução") String status,
            @Schema(description = "Resultado final da execução") Object output,
            @Schema(description = "Mensagem de erro, se houver") String error,
            @Schema(description = "Número de passos executados") int stepCount,
            @Schema(description = "Duração da execução em ms") long durationMs,
            @Schema(description = "Histórico de passos para debug") List<StepDto> steps,
            @Schema(description = "Estado final do contexto") Map<String, Object> finalContext
    ) {
        public static ExecuteFlowResponse from(String flowId, ExecutionResult result) {
            List<StepDto> stepDtos = result.getSteps().stream()
                    .map(s -> new StepDto(
                            s.step(), s.blockId(), s.blockType(),
                            s.routeTaken(), s.nextBlockId()))
                    .toList();

            return new ExecuteFlowResponse(
                    flowId,
                    result.getStatus().name(),
                    result.getOutput(),
                    result.getErrorMessage(),
                    result.getSteps().size(),
                    result.getDuration() != null ? result.getDuration().toMillis() : 0,
                    stepDtos,
                    result.getFinalContext()
            );
        }
    }

    @Schema(description = "Registro de um passo de execução")
    public record StepDto(
            int step,
            String blockId,
            String blockType,
            String routeTaken,
            String nextBlockId
    ) {}

    @Schema(description = "Resposta de erro da API")
    public record ErrorResponse(
            String error,
            String message,
            Instant timestamp
    ) {
        public static ErrorResponse of(String error, String message) {
            return new ErrorResponse(error, message, Instant.now());
        }
    }
}

