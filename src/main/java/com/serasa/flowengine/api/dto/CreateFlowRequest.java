package com.serasa.flowengine.api.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

@Schema(description = "Especificação de um fluxo a ser criado")
public record CreateFlowRequest(

        @NotBlank(message = "O ID do fluxo é obrigatório")
        @Schema(description = "Identificador único do fluxo", example = "prime-check")
        String id,

        @Schema(description = "Nome amigável do fluxo", example = "Verificação de Número Primo")
        String name,

        @NotBlank(message = "O startBlockId é obrigatório")
        @Schema(description = "ID do bloco inicial", example = "b1")
        String startBlockId,

        @NotEmpty(message = "O fluxo deve ter ao menos um bloco")
        @Valid
        @Schema(description = "Lista de blocos que compõem o fluxo")
        List<BlockDefinitionRequest> blocks
) {

    @Schema(description = "Definição de um bloco dentro do fluxo")
    public record BlockDefinitionRequest(

            @NotBlank(message = "O ID do bloco é obrigatório")
            @Schema(description = "Identificador único do bloco", example = "b1")
            String id,

            @NotBlank(message = "O tipo do bloco é obrigatório")
            @Schema(description = "Tipo do bloco (input, condition, math, set_variable, output)",
                    example = "condition")
            String type,

            @Schema(description = "Configurações específicas do tipo de bloco",
                    example = "{\"left\": \"n\", \"operator\": \"<=\", \"right\": \"1\"}")
            Map<String, Object> config,

            @Schema(description = "Mapeamento de rota → ID do próximo bloco",
                    example = "{\"true\": \"b-not-prime\", \"false\": \"b3\"}")
            Map<String, String> routes
    ) {}
}