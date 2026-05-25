# Flow Engine

[![CI Pipeline](https://github.com/deborazavadniak/flowengine/actions/workflows/ci.yml/badge.svg)](https://github.com/deborazavadniak/flowengine/actions/workflows/ci.yml)

Engine de execução de fluxos dinâmicos baseados em blocos — desafio técnico para desenvolvedor pleno.

---

## Sumário

- [Sobre o projeto](#sobre-o-projeto)
- [Arquitetura](#arquitetura)
- [Decisões técnicas](#decisões-técnicas)
- [Tecnologias](#tecnologias)
- [Como rodar](#como-rodar)
- [Endpoints](#endpoints)
- [Exemplo de uso — fluxo primo](#exemplo-de-uso--fluxo-primo)
- [Testes](#testes)
- [Estrutura do projeto](#estrutura-do-projeto)

---

## Sobre o projeto

Uma engine backend que permite **criar, armazenar e executar fluxos dinâmicos** compostos por blocos conectados entre si — semelhante ao n8n ou Zapier, porém com foco na modelagem da engine de execução.

Cada bloco realiza uma única operação atômica. O fluxo avança de bloco em bloco através de rotas nomeadas (`true`, `false`, `next`) até que não haja mais próximo bloco definido.

Para validar o funcionamento da engine, o projeto inclui um fluxo que **recebe um número e retorna se ele é primo ou não**, composto por múltiplos blocos com operações simples.

---

## Arquitetura

O projeto segue **arquitetura hexagonal** com separação clara entre domínio, infraestrutura e API:

```
api/                  → controllers REST, DTOs, tratamento de erros
domain/
  engine/             → FlowEngine, Block (interface), BlockRegistry
  model/              → Flow, BlockDefinition, ExecutionContext
  port/               → FlowRepository (interface de persistência)
blocks/               → implementações concretas de cada tipo de bloco
infra/                → InMemoryFlowRepository (implementação do port)
config/               → registro dos blocos disponíveis na engine
```

### Como a engine funciona

```
POST /api/flows/{id}/execute
          ↓
     FlowEngine
          ↓
  bloco atual → execute(definition, context) → routeKey
          ↓
  routeKey → nextBlockId → próximo bloco
          ↓
  repete até nextBlockId == null
          ↓
     ExecutionResult
```

O `ExecutionContext` é um mapa de variáveis compartilhado entre todos os blocos de uma execução. Cada bloco lê e escreve no contexto sem conhecer os demais blocos.

### Tipos de bloco disponíveis

| Tipo | Responsabilidade |
|---|---|
| `input` | Lê parâmetro de entrada e injeta no contexto |
| `condition` | Avalia expressão e retorna rota `true` ou `false` |
| `math` | Operações aritméticas (`+`, `-`, `*`, `/`, `%`, `set`) |
| `set_variable` | Define variável com valor literal no contexto |
| `output` | Define o resultado final e encerra o fluxo |

---

## Decisões técnicas

**Domínio sem Spring**
As classes em `domain/` não importam nenhuma dependência do Spring. São Java puro, testáveis sem contexto de aplicação e portáveis para qualquer framework.

**Interface Block como contrato único**
`Block` é uma interface funcional com um único método `execute(definition, context) → routeKey`. Adicionar um novo tipo de bloco é criar uma classe nova e registrá-la no `BlockRegistryConfig` — zero mudanças na `FlowEngine` (Open/Closed Principle).

**ExecutionContext como mapa tipado**
Em vez de passar parâmetros entre blocos via métodos, o contexto é um mapa com helpers tipados (`getLong`, `getString`). Isso permite que qualquer bloco leia qualquer variável sem acoplamento direto.

**InMemoryFlowRepository via port**
A persistência é feita em memória com `ConcurrentHashMap`. Como o repositório implementa uma interface (`FlowRepository`), trocar para JPA ou Redis no futuro é uma implementação nova do port — sem tocar no domínio ou na engine.

**Proteção contra loops infinitos**
A `FlowEngine` tem um limite de 1000 passos por execução. Se excedido, retorna `ExecutionResult` com status `ERROR` em vez de travar a JVM.

---

## Tecnologias

- Java 21
- Spring Boot 4.0.6
- Spring Web + Validation + Actuator
- SpringDoc OpenAPI 3.0.3 (Swagger UI)
- JUnit 5 + Mockito
- Docker + Docker Compose
- GitHub Actions (CI)

---

## Como rodar

### Pré-requisitos

- Java 21+
- Maven 3.9+
- Docker e Docker Compose (opcional)

### Opção 1 — Maven direto

```bash
# Clonar o repositório
git clone https://github.com/deborazavadniak/flowengine.git
cd flowengine

# Rodar a aplicação
./mvnw spring-boot:run
```

### Opção 2 — Docker Compose

```bash
# Clonar o repositório
git clone https://github.com/deborazavadniak/flowengine.git
cd flowengine

# Buildar e subir
docker compose up --build
```

A aplicação estará disponível em:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health check: `http://localhost:8080/actuator/health`

---

## Endpoints

### `POST /api/flows` — Criar fluxo

Recebe a especificação do fluxo em JSON e armazena em memória.

```bash
curl -X POST http://localhost:8080/api/flows \
  -H "Content-Type: application/json" \
  -d @src/main/resources/flows/prime-check-flow.json
```

### `POST /api/flows/{id}/execute` — Executar fluxo

Executa o fluxo pelo ID com os parâmetros de entrada fornecidos.

```bash
curl -X POST http://localhost:8080/api/flows/prime-check/execute \
  -H "Content-Type: application/json" \
  -d '{ "input": { "number": 17 } }'
```

### `GET /api/flows` — Listar fluxos

```bash
curl http://localhost:8080/api/flows
```

### `DELETE /api/flows/{id}` — Remover fluxo

```bash
curl -X DELETE http://localhost:8080/api/flows/prime-check
```

---

## Exemplo de uso — fluxo primo

O fluxo de verificação de número primo demonstra a engine com 10 blocos atômicos:

```
b1 (input) → b2 (n <= 1?) → b3 (n == 2?) → b4 (n % 2 == 0?)
→ b5 (divisor = 3) → b6 (divisor² <= n?) → b7 (n % divisor == 0?)
→ b8 (divisor += 2) → [loop para b6]
→ b-prime (output: "É primo")
→ b-not-prime (output: "Não é primo")
```

### 1. Criar o fluxo

```bash
curl -X POST http://localhost:8080/api/flows \
  -H "Content-Type: application/json" \
  -d '{
    "id": "prime-check",
    "name": "Verificação de Número Primo",
    "startBlockId": "b1",
    "blocks": [
      {
        "id": "b1",
        "type": "input",
        "config": { "inputKey": "number", "outputVar": "n" },
        "routes": { "next": "b2" }
      },
      {
        "id": "b2",
        "type": "condition",
        "config": { "left": "n", "operator": "<=", "right": "1" },
        "routes": { "true": "b-not-prime", "false": "b3" }
      },
      {
        "id": "b3",
        "type": "condition",
        "config": { "left": "n", "operator": "==", "right": "2" },
        "routes": { "true": "b-prime", "false": "b4" }
      },
      {
        "id": "b4",
        "type": "condition",
        "config": { "left": "n", "operator": "%", "right": "2" },
        "routes": { "true": "b-not-prime", "false": "b5" }
      },
      {
        "id": "b5",
        "type": "set_variable",
        "config": { "key": "divisor", "value": "3" },
        "routes": { "next": "b6" }
      },
      {
        "id": "b6",
        "type": "condition",
        "config": { "left": "divisor", "operator": "pow2_lte", "right": "n" },
        "routes": { "true": "b7", "false": "b-prime" }
      },
      {
        "id": "b7",
        "type": "condition",
        "config": { "left": "n", "operator": "%", "right": "divisor" },
        "routes": { "true": "b-not-prime", "false": "b8" }
      },
      {
        "id": "b8",
        "type": "math",
        "config": { "target": "divisor", "operation": "+", "operand": "2" },
        "routes": { "next": "b6" }
      },
      {
        "id": "b-prime",
        "type": "output",
        "config": { "value": "É primo" },
        "routes": {}
      },
      {
        "id": "b-not-prime",
        "type": "output",
        "config": { "value": "Não é primo" },
        "routes": {}
      }
    ]
  }'
```

### 2. Executar com número primo

```bash
curl -X POST http://localhost:8080/api/flows/prime-check/execute \
  -H "Content-Type: application/json" \
  -d '{ "input": { "number": 17 } }'
```

Resposta:

```json
{
  "flowId": "prime-check",
  "status": "SUCCESS",
  "output": "É primo",
  "stepCount": 7,
  "durationMs": 2
}
```

### 3. Executar com número não primo

```bash
curl -X POST http://localhost:8080/api/flows/prime-check/execute \
  -H "Content-Type: application/json" \
  -d '{ "input": { "number": 15 } }'
```

Resposta:

```json
{
  "flowId": "prime-check",
  "status": "SUCCESS",
  "output": "Não é primo",
  "stepCount": 6,
  "durationMs": 1
}
```

---

## Testes

```bash
# Rodar todos os testes
./mvnw test

# Rodar só os testes de um arquivo
./mvnw test -Dtest=FlowEngineTest

# Rodar só a integração do fluxo primo
./mvnw test -Dtest=PrimeFlowIntegrationTest
```

### Cobertura dos testes

| Classe | Tipo | O que testa |
|---|---|---|
| `FlowEngineTest` | Unitário com `@Mock` | Loop de execução, roteamento, histórico de passos, tratamento de erros |
| `ConditionBlockTest` | Unitário com `@InjectMocks` | Todos os operadores, casos de borda, variável ausente |
| `MathBlockTest` | Unitário com `@InjectMocks` | Todas as operações aritméticas, divisão por zero |
| `PrimeFlowIntegrationTest` | Integração com `@Spy` | 20 casos do fluxo primo end-to-end |

---

## Estrutura do projeto

```
src/
├── main/java/com/serasa/flowengine/
│   ├── api/                        → controllers e DTOs
│   ├── blocks/                     → implementações de blocos
│   ├── config/                     → registro dos blocos
│   ├── domain/
│   │   ├── engine/                 → FlowEngine, Block, BlockRegistry
│   │   ├── model/                  → Flow, BlockDefinition, ExecutionContext
│   │   └── port/                   → FlowRepository (interface)
│   └── infra/                      → InMemoryFlowRepository
└── test/java/com/serasa/flowengine/
    ├── blocks/                     → testes unitários dos blocos
    ├── engine/                     → testes da FlowEngine
    └── integration/                → teste end-to-end do fluxo primo
```