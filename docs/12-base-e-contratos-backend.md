# Base técnica e contratos iniciais do backend

Registro de 05/10/2026, referente a **BE01 (recorte técnico)** e **BE02**. O pedido D11 autorizou iniciar o backend e escolher uma frente. A escolha foi preparar a base executável. São decisões técnicas de execução; não aprovam nem alteram propostas de negócio AC01 a AC16.

Este é o registro da primeira base. O bloco posterior D12 acrescenta JPA, cadastros, JWT e auditoria; contratos/configurações vigentes e limites estão no [documento 14](14-cadastros-acesso-e-persistencia.md). As descrições de pacotes reservados e ausência de persistência abaixo correspondem à entrega inicial.

## Escolhas desta entrega

| Item | Escolha e motivo |
| --- | --- |
| Java | JDK 21 LTS, já instalado nesta máquina, utilizado para compilação e execução |
| Spring | Spring Boot 4.1.1, versão estável consultada; Spring MVC para API REST |
| Build | Maven 3.9.16, Wrapper 3.3.4 oficial `only-script`; distribuição e SHA-256 fixados |
| Identidade técnica | `br.com.rodogarcia:wms-backend`, pacote `br.com.rodogarcia.wms`; escolha para o WMS, sem determinar nome de banco ou ambiente |
| Dependências | Starters MVC, Validation e Security; versões transitivas gerenciadas pelo Spring Boot |
| Testes | JUnit, AssertJ, Spring Test/MockMvc e servidor HTTP embarcado; dados fictícios |
| Persistência | SQL Server permanece definido; driver, ORM, migrations, modelo físico e alvo em BE03 |
| Ambientes | `local` por padrão e `test`; outros perfis ou combinações são recusados |
| Rede local | `127.0.0.1`, porta `8080`, substituível por `WMS_PORT` |
| Autenticação | Ainda não escolhida. Spring Security libera somente GET de status e nega os demais acessos; sem usuário/senha gerados |
| Datas técnicas | `Instant` UTC por `Clock` injetado; FIFO, chegada e cobrança serão modeladas separadamente |

A compatibilidade foi conferida nos [requisitos oficiais do Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html). O pacote principal fica acima das camadas, conforme as [orientações do Spring](https://docs.spring.io/spring-boot/reference/using/structuring-your-code.html), preservando o MVC por camadas escolhido pelo responsável. O [Maven Wrapper](https://maven.apache.org/tools/wrapper/) fixa a distribuição e seu checksum. Os artefatos foram encontrados no Maven Central e usados no build local.

## Contrato disponível para FE02

Prefixo `/api/v1`; JSON em UTF-8. Frontend ainda não implementado. CORS será definido com FE02; nenhuma origem externa está liberada nesta base.

### Consulta de status

`GET /api/v1/status`, sem parâmetros ou autenticação. Resposta `200 application/json`:

```json
{
  "aplicacao": "wms-rodogarcia",
  "status": "DISPONIVEL",
  "instante": "2026-10-05T15:00:00Z"
}
```

Instante fictício no exemplo. Comprova somente que o processo atende HTTP; não representa disponibilidade de banco/equipamentos ou prontidão do piloto. Não expõe versão, perfis ou configuração.

### Identificação

Cada solicitação que atravessa a aplicação recebe `X-Request-Id`, UUID novo gerado pelo servidor. Um cabeçalho recebido com esse nome é substituído. O ID aparece em erros e no contexto dos logs. Serve para diagnóstico; **não é chave de idempotência** de reserva, baixa ou cobrança. A prevenção de repetição dessas ações pertence aos respectivos serviços.

### Falhas

Formato `application/problem+json`, com `ProblemDetail` do Spring:

```json
{
  "type": "urn:wms:erro:ACESSO_NEGADO",
  "title": "Não foi possível concluir a solicitação",
  "status": 403,
  "detail": "Acesso não disponível nesta etapa do WMS.",
  "instance": "urn:uuid:00000000-0000-4000-8000-000000000001",
  "codigo": "ACESSO_NEGADO",
  "idOperacao": "00000000-0000-4000-8000-000000000001"
}
```

UUID fictício. `instance` identifica a solicitação sem repetir sua URL ou parâmetros.

| Situação | HTTP / código | Tratamento |
| --- | --- | --- |
| Rota/método não liberado | `403 / ACESSO_NEGADO` | Sem login, redirecionamento ou sessão automática |
| JSON inválido em rota liberada | `400 / HTTP_400` | Não reproduz conteúdo recebido |
| DTO inválido em rota liberada | `400 / DADOS_INVALIDOS` | `campos` contém `campo` e código da restrição, como `NotBlank`, sem valor rejeitado |
| Falha de protocolo MVC | HTTP original / `HTTP_<status>` | Preserva cabeçalhos como `Allow` em 405 |
| Falha inesperada | `500 / ERRO_INTERNO` | Mensagem segura; log com classe da exceção e ID, sem mensagem original/stacktrace potencialmente sensível |

Erros MVC foram preparados e testados com controllers exclusivos dos testes. Não há rota artificial de escrita/falha no código principal. Rotas desconhecidas recebem 403 antes do MVC; essa resposta não comprova a existência de um recurso.

CSRF permanece ativo. O modo sem sessão desta base não determina a solução de BE04. Novas rotas exigem autorização adequada e testes no backend.

## Organização e evolução

Fluxo `controllers → services → repositories`; `models` representa negócio/persistência, `dto` delimita API, `config` configura e `exceptions` trata falhas. O status usa controller/service/DTO e não precisa de repositório. `models` e `repositories` têm documentação de pacote, sem entidades/tabelas ou armazenamento simulado.

Modelo conceitual completo, transições, precisão de quantidades/valores, contratos operacionais, filtros e paginação continuam em **BE01**. O contrato de status não conclui essa etapa. Próximo recorte: cliente, armazém, produto e embalagem, preservando vínculos para entrada/reserva/saída/cobrança.

BE03 definirá alvo e procedimento antes de conectar ou migrar. BE04 implementará Gestor, Supervisor e Operação com seus alcances. Com essas dependências, BE05 poderá implementar cadastros. A base atende ao contrato inicial de FE02, mas FE01/FE02 e M01 continuam sem entrega conjunta.
