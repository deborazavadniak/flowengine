package com.serasa.flowengine.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Schema(description = "Parâmetros de entrada para execução de um fluxo")
public record ExecuteFlowRequest(

        @NotNull(message = "Os parâmetros de entrada são obrigatórios")
        @Schema(
                description = "Mapa de parâmetros de entrada do fluxo",
                example = "{\"number\": 17}"
        )
        Map<String, Object> input
) {}
