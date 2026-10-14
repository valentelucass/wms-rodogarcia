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
