# Unifor Manager - Backend

Backend da aplicação Unifor Manager, sistema de gestão de matrículas e turmas para coordenadores e alunos. Desenvolvido em Java com Quarkus, PostgreSQL e autenticação via Keycloak.

## Stack

- **Java 21**
- **Quarkus 3.x**
- **PostgreSQL**
- **Keycloak** (autenticação OIDC)
- **Hibernate ORM / Panache**
- **Flyway** (migrations)

## Pré-requisitos

- JDK 21+
- Maven 3.9+
- PostgreSQL
- Keycloak (para autenticação em ambiente próximo ao de produção; opcional em desenvolvimento)

## Início rápido

### 1. Banco de dados

Crie o banco e o usuário:

```sql
CREATE DATABASE unifor_manager;
CREATE USER unifor WITH PASSWORD 'unifor';
GRANT ALL PRIVILEGES ON DATABASE unifor_manager TO unifor;
```

O Flyway executa as migrations na subida da aplicação (tabelas + dados iniciais).

### 2. Executar a aplicação

**Com Keycloak** (recomendado para autenticação completa):

```bash
# Keycloak deve estar rodando em http://localhost:8081
./mvnw quarkus:dev
```

**Sem Keycloak** (apenas desenvolvimento local):

```bash
./mvnw quarkus:dev -Dquarkus.profile=dev-without-keycloak
```

Com `dev-without-keycloak`, o OIDC fica desativado e os endpoints aceitam requisições sem autenticação. Use apenas em desenvolvimento local.

### 3. Documentação da API

- **OpenAPI:** http://localhost:8080/q/openapi
- **Swagger UI:** http://localhost:8080/q/swagger-ui

## Configuração do Keycloak

Para autenticação completa, configure o Keycloak:

1. Realm: `unifor`
2. Client: `unifor-manager` (confidencial)
3. Roles: `coordinator`, `student`
4. Os e-mails dos usuários devem coincidir com os de `users.email` no seed (veja `V2__seed_data.sql`)

Consulte `application.properties` e o Anexo B do PRD para detalhes.

## Visão geral da API

| Papel       | Endpoints                                                                 |
|-------------|---------------------------------------------------------------------------|
| Coordenador | `/api/coordinator/matrices`, `/api/coordinator/matrices/{id}/classes`, `/api/coordinator/reference/{subjects,professors,time-slots,courses}` |
| Estudante   | `/api/student/enrollments`, `/api/student/classes/available`, `/api/student/me` |

Coordenador: criar/editar/remover matrizes e turmas; dados de referência para os dropdowns do formulário de adicionar turma.  
Estudante: listar matrículas, listar turmas disponíveis, matricular-se.

## Testes

```bash
./mvnw test
```

Os testes usam **Testcontainers** para PostgreSQL; não é necessário banco local. Veja `Phase2Test`–`Phase6Test` para validação por fase.

## Estrutura do projeto

```
org.unifor
├── api/coordinator      # Endpoints REST do coordenador
├── api/student          # Endpoints REST do estudante
├── service              # Regras de negócio (coordenador, estudante)
├── repository           # Repositórios Panache
├── entity               # Entidades JPA
├── dto                  # DTOs de requisição e resposta
├── exception            # Exceções customizadas e mappers
└── security             # CurrentUserService, mapeamento Keycloak
```

## Documentação

- **PRD.md** — Requisitos do produto
- **ARCHITECTURE.md** — Arquitetura técnica
- **PRD_COMPLIANCE.md** — Relatório de conformidade da implementação

A documentação detalhada fica em `documentation/` (PRD, ARCHITECTURE, PRD_COMPLIANCE).

## Docker Compose

Execute a stack completa (PostgreSQL, Keycloak, aplicação) com Docker Compose.

A imagem da aplicação apenas **copia** o conteúdo de `target/quarkus-app/` (não executa Maven), então é preciso **buildar** o app Quarkus **antes** de buildar a imagem Docker. Use sempre esta ordem:

```bash
# 1. Buildar o app Quarkus (obrigatório; gera target/quarkus-app/)
./mvnw package -DskipTests
# No Windows (PowerShell): .\mvnw.cmd package -DskipTests

# 2. Buildar imagens e subir todos os serviços
docker compose up --build
```

**Portas:**

| Serviço   | Porta no host | Porta no container |
|-----------|----------------|---------------------|
| App       | 8080           | 8080                |
| Keycloak  | 8081           | 8080                |

**Credenciais:**

- **Console Admin do Keycloak:** http://localhost:8081 — usuário `admin`, senha `admin`
- **Usuários do seed (coordenador/estudante):** senha `secret`
  - Coordenadores: carmen.lima@unifor.br, roberto.alves@unifor.br, fernanda.souza@unifor.br
  - Estudantes: lucas.ferreira@unifor.br, beatriz.rodrigues@unifor.br, rafael.pereira@unifor.br, juliana.martins@unifor.br, gabriel.costa@unifor.br

O realm `unifor` e o client `unifor-manager` são importados automaticamente de `keycloak/unifor-realm.json`.

Funciona em Linux, Mac e Windows sem configuração extra. O frontend deve usar `http://localhost:8081` para o Keycloak (servidor de autenticação / issuer).

## Empacotamento

```bash
# JAR
./mvnw package

# Nativo
./mvnw package -Dnative -Dquarkus.native.container-build=true
```
