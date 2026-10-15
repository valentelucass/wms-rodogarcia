import type { VisaoOperacaoDto_Posicao } from "../contracts/types";

/** Catálogo único dos popups informativos. Ao mudar um indicador/fluxo,
 * revise sua explicação aqui e o respectivo consumidor. Não contém regras de cálculo. */
export type Information = {
    title: string;
    description?: string;
    facts?: { label: string; value: string }[];
    footer?: string;
};

export const information = {
    contingencyRegistration: {
        title: "Registro manual da planilha",
        description:
            "Registre os fatos manualmente depois que a conexão for restabelecida. Não há sincronização automática offline.",
        facts: [
            {
                label: "Dados do fato",
                value: "Informe identidade, instante real, operador, fonte e dependências. Para CHEGADA, inclua os dados específicos da chegada.",
            },
            {
                label: "Conciliação",
                value: "Confira versão e hash. Executar ou vincular são ações explícitas; vincular exige prova da operação original.",
            },
            {
                label: "Pendências",
                value: "Dependências mantêm pendência. Conteúdo divergente retorna conflito.",
            },
            {
                label: "Reenvio",
                value: "A repetição conserva a confirmação original. Consulte o estado atual.",
            },
        ],
    },
    auditHistory: {
        title: "Histórico de alterações",
        description:
            "Consulte as alterações do registro indicado. Essa consulta não modifica os dados.",
    },
    auditHistoryLegacy: {
        title: "Legado não atribuído",
        description:
            "O histórico confirmado usa o vínculo selecionado. Candidatos legados ambíguos não são histórico confirmado deste vínculo. Coincidência de ID não comprova que o registro pertence ao vínculo.",
    },
    contractConfiguration: {
        title: "Parâmetros do contrato",
        description:
            "Informe os parâmetros contratados. Ausência de configuração não equivale a zero. Use ponto para decimais.",
    },
    contractMinimum: {
        title: "Mínimo de cobrança",
        description:
            "Informe a aplicação, o valor, a proporção e os serviços previstos no contrato.",
    },
    contractGris: {
        title: "GRIS",
        description:
            "Informe a aplicação, o percentual, a base, a periodicidade e a proporção contratados.",
    },
    contractHistorical: {
        title: "Resolução financeira histórica",
        description:
            "Identifique a resolução histórica com seu compromisso de origem. Ela não reativa cadastros nem cria movimentação física.",
    },
    contractMinimumEmpty: {
        title: "Serviços do mínimo",
        description: "Nenhum serviço adicionado.",
    },
    damageUnit: {
        title: "Unidade afetada",
        description:
            "Consulte e selecione a unidade para carregar sua identificação e revisão atuais.",
    },
    damageQuantity: {
        title: "Quantidade afetada",
        description: "Informe a quantidade afetada; use ponto para decimais.",
    },
    damageDestinations: {
        title: "Destino da mercadoria",
        description:
            "Adicione as posições de destino e informe o código lido de cada endereço.",
    },
    damageDestinationsEmpty: {
        title: "Posições de destino",
        description: "Nenhuma posição adicionada.",
    },
    closureImpediments: {
        title: "Impedimentos do cadastro",
        description:
            "Confira as pendências desta consulta. As regras de encerramento serão verificadas novamente ao confirmar.",
    },
    closureNoImpediments: {
        title: "Consulta sem impedimentos informados",
        description: "Nenhum impedimento informado nesta consulta.",
    },
    closureImpedimentsUnavailable: {
        title: "Impedimentos não informados",
        description:
            "A consulta não informou a lista de impedimentos. Consulte novamente para conferir.",
    },
    billingTableEdit: {
        title: "Edição da tabela de cobrança",
        description:
            "Altere os dados, ajuste valores e adicione ou remova serviços, informando o motivo. As tarifas anteriores e os cálculos já registrados ficam preservados; salvar não recalcula nem altera fechamentos. Cliente, armazém, tipo e vigência são conferidos para proteger vínculos e histórico existentes.",
    },
    collectorMeasures: {
        title: "Medidas da unidade e destino",
        description:
            "Informe as medidas físicas da mercadoria. Para uma unidade em uma posição, indique uma posição necessária e deixe o conjunto vazio. Somente para duas posições, informe o conjunto cadastrado e acrescente o segundo destino.",
    },
    addressCapacity: {
        title: "Capacidade da posição",
        description:
            "Selecione os tipos de unidade permitidos (pallet, bobina e volume) ou marque Todas as opções. Para endereçar, complete também peso máximo, altura, largura, profundidade e empilhamento máximo da posição. Esses limites pertencem ao endereço; as medidas informadas no coletor pertencem à mercadoria. Uma unidade em uma posição não precisa de conjunto. Campos ausentes deixam a capacidade pendente.",
    },
    positionSets: {
        title: "Conjuntos de duas posições",
        description:
            "Use conjuntos somente quando uma única unidade precisar ocupar duas posições juntas. Para uma bobina em uma posição, não crie conjunto. Os limites e o tipo de unidade permitido podem ser preenchidos em Endereços, ao cadastrar ou editar a posição. O botão Configurar também permite completar esses limites.",
    },
    positionSetCreate: {
        title: "Criar conjunto de posições",
        description:
            "Use este conjunto quando uma única unidade precisar ocupar duas posições juntas.",
        footer: "Código gerado ao confirmar",
    },
    positionSetSelection: {
        title: "Escolha das posições",
        description:
            "Escolha duas posições diferentes do mesmo armazém e área. Trocar o armazém limpa a seleção das posições.",
    },
    positionSetLimits: {
        title: "Limites do conjunto",
        description:
            "Limites aferidos para o conjunto. Peso em kg e dimensões em metros; use ponto e até 3 casas decimais (ex.: 2.50).",
    },
    registryEdit: {
        title: "Correção de cadastro",
        description:
            "Gestor pode corrigir os dados com justificativa e revisão atual. Identidades e vínculos de proprietário permanecem preservados. Alterações de medida, conversão e estrutura física são conferidas pelo sistema para proteger operações existentes.",
    },
    unitLabel: {
        title: "Etiqueta da unidade",
        description:
            "Barras e QR identificam o mesmo UUID. A data em destaque é a chegada física; a quantidade é o total do produto nas notas de origem. A numeração considera as unidades ativas do pedido no momento da consulta. Reimprimir não cria estoque. Confira tamanho, leitura e substituição física na impressora.",
    },
    confirmedEntries: {
        title: "Distribuição das entradas conferidas",
        description:
            "Identifique a mercadoria pela nota, item e SKU. Selecione a entrada para distribuir em unidades; as datas de chegada, FIFO e efetivação continuam separadas.",
    },
    receivingFinalizationMatched: {
        title: "Conferência sem divergência",
        description:
            "As quantidades físicas conferem com a previsão e não há avaria. Confirme a efetivação para concluir a entrada. Os dados serão conferidos novamente ao confirmar.",
    },
    receivingFinalizationAcceptance: {
        title: "Aceite de divergências",
        description:
            "Quando houver diferença ou avaria, registre a tratativa no motivo e aceite somente o que foi autorizado com o cliente. O aceite é necessário apenas para efetivar uma conferência com divergência.",
    },
    operationDateTime: {
        title: "Data e horário da operação",
        description:
            "Horário de Brasília. Escolha a data no calendário e informe o horário real do fato.",
    },
    receivingPhysicalQuantity: {
        title: "Quantidade física",
        description:
            "Informe a quantidade física na unidade do produto cadastrado. Não use a unidade comercial do XML nem a contagem de bobinas.",
    },
    receivingBobinas: {
        title: "Peso e contagem de bobinas",
        description:
            "Confira o peso na unidade do produto e conte as bobinas boas e avariadas. Para o cliente/produto com regra de uma bobina por item, o sistema sugere o peso da linha e uma bobina; confirme ou corrija conforme a chegada física. Uma única bobina usa o peso conferido na distribuição. Para várias bobinas no mesmo item, informe cada peso individual. A soma e a contagem são validadas pelo sistema. Cada bobina desta operação terá sua posição, escolhida no endereçamento. Corrija uma chegada incorreta por estorno e novo registro.",
    },
    occupancy: {
        title: "Ocupação",
        description:
            "Percentual de posições de armazenagem ocupadas em relação à capacidade ativa informada na consulta. Posições físicas e quantidade de produtos são medidas diferentes.",
    },
    occupied: {
        title: "Posições ocupadas",
        description:
            "Endereços físicos ocupados no contexto consultado. Observe a área indicada no cartão. Uma posição recebe uma única unidade logística. Uma unidade pode ocupar várias posições conforme a regra de capacidade.",
    },
    free: {
        title: "Posições livres",
        description:
            "Posições de armazenagem ativas e desocupadas. Estar livre não garante que a posição aceite qualquer carga: capacidade, bloqueios e permissões são conferidos na operação.",
    },
    stored: {
        title: "Unidades armazenadas",
        description:
            "Unidades logísticas ativas no contexto consultado. Não representa a quantidade de produtos dentro dessas unidades nem somente o estoque disponível para saída.",
    },
    storedValue: {
        title: "Valor armazenado",
        description:
            "Valoração do estoque confirmado, conforme os dados disponíveis na consulta. Um valor pendente ou acesso restrito não significa estoque de valor zero. Não representa cobrança de armazenagem.",
    },
    quarantine: {
        title: "Em quarentena",
        description:
            "Unidades localizadas na área de quarentena. Esse estoque não atende pedidos de saída enquanto permanecer nessa condição.",
    },
    reservations: {
        title: "Reservas ativas",
        description:
            "Reservas ainda não encerradas no contexto consultado. Reserva não é retirada física e não expira automaticamente.",
    },
    inbound: {
        title: "Entradas abertas",
        description:
            "Pedidos de entrada ainda não efetivados. Documentos registrados e conferência física são etapas distintas; o pedido aberto não comprova estoque disponível.",
    },
    outbound: {
        title: "Saídas abertas",
        description:
            "Pedidos de saída que ainda aguardam conclusão. Inclui etapas de elaboração, reserva e preparação; não representa somente pedidos prontos para retirada.",
    },
    billing: {
        title: "Faturamento do mês",
        description:
            "Visão parcial das NFS-e registradas na competência exibida e no contexto permitido. Não equivale ao valor do estoque nem a todos os serviços ainda por faturar.",
    },
    availableUnits: {
        title: "Unidades disponíveis",
        description:
            "Unidades logísticas consideradas elegíveis à saída na consulta. Disponibilidade, capacidade e permissão são revalidadas ao confirmar a operação.",
    },
    expiry: {
        title: "Avisos de validade",
        description:
            "Unidades vencidas ou próximas do vencimento, conforme a antecedência configurada. Quando essa antecedência não está configurada, o indicador não deve ser interpretado como zero avisos.",
    },
    physicalChart: {
        title: "Ocupação física por área",
        description:
            "Compara posições ocupadas nas áreas do contexto consultado. Armazenagem, triagem, quarentena e separação têm funções distintas; a comparação não indica liberação para saída.",
    },
    queueChart: {
        title: "Fila de saída",
        description:
            "Distribuição dos pedidos que ainda aguardam conclusão entre suas etapas. Os números representam pedidos, não unidades logísticas ou quantidades de produto.",
    },
    productChart: {
        title: "Disponibilidade por produto",
        description:
            "Distingue saldo disponível, reservado, demais indisponíveis e quantidade pendente de unitização. Consulte os produtos das demais páginas; o gráfico não soma quantidades de produtos diferentes.",
    },
    receivingShortcut: {
        title: "Receber e conferir",
        description:
            "Acesse pedidos de entrada, notas, chegada e conferência física da carga. Registrar uma nota não efetiva automaticamente a entrada.",
    },
    collectorShortcut: {
        title: "Ler e endereçar",
        description:
            "Acesse os fluxos de leitura de etiquetas, consulta de unidades e endereçamento. A operação continua sujeita às verificações do sistema.",
    },
    shippingShortcut: {
        title: "Reservar e separar",
        description:
            "Acesse pedidos de saída, reserva e separação do estoque. Confirme as referências e a disponibilidade antes de concluir cada etapa.",
    },
    closingShortcut: {
        title: "Conferir fechamento",
        description:
            "Consulte serviços, valores e demonstrativos do fechamento. A consulta não recalcula nem altera cobranças já fechadas.",
    },
    warehouseMap: {
        title: "Mapa do armazém",
        description:
            "As cores identificam posições livres, ocupadas e indisponíveis. Cadeado e marcador indicam bloqueio e reserva. A prévia usa os dados consultados; abra a posição para seus detalhes. Setas percorrem colunas e a paginação carrega outros endereços.",
    },
} satisfies Record<string, Information>;

export type InformationKey = keyof typeof information;
export const shortcutInformation: Record<string, InformationKey> = {
    entrada: "receivingShortcut",
    coletor: "collectorShortcut",
    saida: "shippingShortcut",
    fechamento: "closingShortcut",
};
export const informationLabels = {
    help: (title: string) => `Informações sobre ${title}`,
    currentValue: "Valor exibido",
    currentContext: "Situação desta consulta",
};

export function metricInformation(
    key: InformationKey,
    value: string,
    note: string,
): Information {
    return {
        ...information[key],
        facts: [
            { label: informationLabels.currentValue, value },
            { label: informationLabels.currentContext, value: note },
        ],
    };
}

export function positionInformation(
    p: VisaoOperacaoDto_Posicao,
    state: string,
    weight: string,
): Information {
    return {
        title: p.codigo,
        description: [
            state,
            p.bloqueada && "Bloqueada",
            p.reservada && "Reservada",
            p.quarentena && "Em quarentena",
        ]
            .filter(Boolean)
            .join(" · "),
        facts: [
            { label: "Armazém", value: p.armazem },
            { label: "Rua", value: p.rua },
            { label: "Nível", value: String(p.nivel) },
            { label: "Posição", value: p.posicao },
            { label: "Área", value: p.tipo.toLowerCase().replaceAll("_", " ") },
            {
                label: "Capacidade de peso",
                value:
                    p.capacidadePesoKg == null
                        ? "Não configurada"
                        : `${weight} kg`,
            },
        ],
        footer: "Clique ou pressione Enter para abrir os detalhes.",
    };
}
