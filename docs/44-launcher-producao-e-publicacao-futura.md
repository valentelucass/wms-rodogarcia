# Launcher de produção Windows e publicação futura

PROD-LAUNCH01 atende ao pedido de Lucas com o nome convencional **iniciar-prod.bat**. A grafia `.bar` recebida permanece no [pedido original](../orchestracao/.runtime/launcher-producao-demanda-lucas.txt); não é outro comando.

Execute na raiz WMS:

```bat
iniciar-prod.bat --preparar
iniciar-prod.bat
```

`--preparar` compila a versão atual de backend e frontend REAL em um candidato isolado. Não abre banco, inicia backend nem usa Vite dev. Fontes capturadas, JAR, configuração, assets e respectivos hashes ficam no recibo da versão. Falha de build não substitui artefato anterior. Não há instalação automática de dependências.

O comando sem argumentos confere portas e configuração protegida própria PROD antes do build e da guarda. Só inicia processos próprios com pré-requisitos completos. Banco permitido: **WMS_PROD**, identidade esperada **WMSPROD**, jamais WMSDEV, sa ou fallback DEV. Estes nomes são contrato de produção; não comprovam que identidade/canal já estejam provisionados.

Não há migration, init, bootstrap de usuário ou alteração automática do schema: perfil único sqlserver-prod, Hibernate validate, init never e Flyway/Liquibase desativados. A chave/metadata de login e credencial SQL precisam pertencer ao canal protegido PROD; material auth-dev/api-dev não é reaproveitado. Segredos não são parâmetros, frontend, relatório ou logs. Guarda operacional e readiness público não são teste/homologação de negócio em produção.

Cada execução prepara uma versão nova sob `orchestracao/.runtime/launcher-producao/runs/`. O manifesto fixa os bytes usados; não aponta para dist mutável de desenvolvimento. Se houver produção ou outro processo ocupando a porta, o launcher recusa a subida, mesmo que o processo pareça WMS. Pode-se preparar o próximo candidato sem interromper a versão ativa. Parada/atualização de uma execução existente depende de decisão específica do responsável; não há kill por porta, PM2 compartilhado, tarefa, serviço ou autostart. A janela operacional acompanha apenas os processos que criou.

## Portas e domínios para suporte

| Serviço | Origem local planejada | Domínio futuro exato |
|---|---|---|
| Frontend estático REAL e relay /api | http://127.0.0.1:25591 | https://wms.rodogarcia.com.br |
| Backend Java/Spring | http://127.0.0.1:25590 | https://wms-api.rodogarcia.com.br |

Portas inventariadas livres nesta entrega e revalidadas antes de iniciar; isso não é reserva nem prova de produção ativa. Os servidores DEV/visual existentes permanecem intactos. Bind apenas loopback. O [recibo](../orchestracao/.runtime/launcher-producao-resultado.md) distingue alvo preparado, execução local isolada e eventual subida operacional real.

O navegador do frontend usa **/api na mesma origem**; o servidor estático retransmite ao backend loopback sem rewrite/retry, preservando contrato/correlação e Origin. Não transforma cabeçalhos recebidos em prova de HTTPS: Forwarded, X-Forwarded-* e CF-* enviados pelo cliente são removidos. Origin de login produção é fixa https://wms.rodogarcia.com.br, proxy-origin vazio, Secure/HttpOnly/SameSiteStrict e CSRF preservados; API operacional continua Bearer/escopos verificados no backend. Não transportar a regra dinâmica DEV de devtunnels nem liberar CORS wildcard. Expor o domínio backend posteriormente não exige alterar o browser para acesso entre origens.

HTTP loopback permite conferir assets e readiness técnico, **não comprova login HTTPS operacional** com cookie Secure. O domínio frontend precisará de entrada HTTPS válida na etapa futura. A regra TLS SQL permanece independente: confiança privada corresponde ao processo atual, Encrypt Mandatory e TrustServerCertificate=false, sem bypass/trust global/recaptura automática.

## Próxima etapa de suporte, ainda não executada

O suporte receberá os dois domínios exatos e as origens loopback acima, além do manifesto/estado operacional e configuração pública aprovada. Caberá à etapa autorizada futura definir a entrada HTTPS e encaminhamento restrito para cada serviço, validar Host/Origin, autenticação, CSRF, cookies, CSP e status antes de uso. DNS e túnel são responsabilidades distintas. Nenhum tunnelID, CNAME, credencial, certificado público ou regra Cloudflare foi inventado/configurado neste pedido.

Pré-requisitos ausentes são bloqueios concretos: canal SQL PROD restrito/atestado de permissões e catálogo/histórico atuais, material AUTH PROD próprio e usuário já legitimamente provisionado, além da entrada HTTPS operacional. Não criar estes recursos por fallback nem executar testes/fixtures/escritas de negócio no banco PROD. A correção e validação local permanecem separadas das jornadas DEV anteriores já encerradas.

**Estado desta entrega:** `iniciar-prod.bat --preparar` foi executado de fato e passou, com candidato datado em `runs/5bcb34fc4fc24f2ba967bcdca014ac45`. O BAT normal foi executado e retornou exit40 `PROD_AUTH_PUBLIC_METADATA_MISSING`, antes de guarda SQL ou servidores. O guard PROD atual é somente um gate de recusa: não implementa Open nem ramo PASS. O binding entre credencial SQL protegida PROD, guarda e helper backend ainda precisa ser concluído/revisado quando existir uma fonte própria autorizada; criar metadata ou alterar booleano não libera a produção. Não há URL PROD ativa nesta entrega.

O candidato preparado usa a fonte visual capturada naquele instante. Alterações visuais posteriores foram preservadas; novas capturas recusadas por drift não substituem os assets anteriores. O complemento backend inicial também teve uma incompatibilidade encontrada na revisão PROD-VIG-003: a chave de schema generation, mesmo com valor none, era recusada pela guarda conservadora. Os builds iniciais e seus hashes permanecem históricos; o complemento corrigido DFB55 é identificado separadamente no recibo. Os três achados locais foram corrigidos e reconfirmados na [revisão final](../orchestracao/.runtime/launcher-producao/vigia/parecer-final02.json), sem material local aberto. Build PASS não comprova startup. A prova HTTP dos assets usa upstream sintético, sem backend/banco PROD. Builds e harnesses locais não comprovam subida conjunta operacional ou login HTTPS. Entrega parcial encerrada; aguardar Lucas para os pré-requisitos operacionais, sem tentativa automática.
