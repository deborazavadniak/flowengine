package com.serasa.flowengine.domain.engine;

import com.serasa.flowengine.domain.model.ExecutionContext;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Resultado imutável de uma execução de fluxo.
 * Carrega o valor final, o histórico de passos e metadados de tempo.
 */
public class ExecutionResult {

    public enum Status { SUCCESS, ERROR }

    private final Status status;
    private final Object output;
    private final String errorMessage;
    private final List<StepRecord> steps;
    private final Map<String, Object> finalContext;
    private final Duration duration;

    private ExecutionResult(Builder builder) {
        this.status       = builder.status;
        this.output       = builder.output;
        this.errorMessage = builder.errorMessage;
        this.steps        = List.copyOf(builder.steps);
        this.finalContext = builder.finalContext;
        this.duration     = builder.duration;
    }

    // ── Acessores ────────────────────────────────────────────────────────────

    public Status getStatus()              { return status; }
    public Object getOutput()              { return output; }
    public String getErrorMessage()        { return errorMessage; }
    public List<StepRecord> getSteps()     { return steps; }
    public Map<String, Object> getFinalContext() { return finalContext; }
    public Duration getDuration()          { return duration; }
    public boolean isSuccess()             { return status == Status.SUCCESS; }

    // ── Fábrica ──────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Status status;
        private Object output;
        private String errorMessage;
        private List<StepRecord> steps = List.of();
        private Map<String, Object> finalContext = Map.of();
        private Duration duration;

        public Builder success(Object output)           { this.status = Status.SUCCESS; this.output = output; return this; }
        public Builder error(String msg)                { this.status = Status.ERROR; this.errorMessage = msg; return this; }
        public Builder steps(List<StepRecord> steps)    { this.steps = steps; return this; }
        public Builder context(ExecutionContext ctx)     { this.finalContext = ctx.snapshot(); return this; }
        public Builder duration(Instant start)          { this.duration = Duration.between(start, Instant.now()); return this; }
        public ExecutionResult build()                  { return new ExecutionResult(this); }
    }

    // ── Registro de passo ────────────────────────────────────────────────────

    /**
     * Snapshot de um único passo da execução para auditoria/debug.
     */
    public record StepRecord(
            int step,
            String blockId,
            String blockType,
            String routeTaken,
            String nextBlockId,
            Map<String, Object> contextSnapshot
    ) {}
}