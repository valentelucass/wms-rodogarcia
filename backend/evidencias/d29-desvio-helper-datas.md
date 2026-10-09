# D29 — desvio do helper de emissão manual

Na rodada D29E3DB9EE9, versão de fontes/classes BA05074DA2569DB8189ACB9DF6AE0AB9CD3F69571EB12AF8778EFADF700B4A77, o auxiliar herdado `JornadasD29.entry` enviou quatro notas fictícias com emissão 2026-10-04. A restrição D29 exige datas sintéticas somente em cálculo isolado; a derivação preparatória removeu retrocesso de chegada, mas deixou esse campo documental.

Os pedidos de entrada 29–32, cliente45/armazém21, permanecem intactos. Chegadas físicas usaram o instante real; nenhuma alteração de Clock ou SQL de negócio ocorreu. Não usar essas emissões como prova de vigência, FIFO, corte, cobrança ou datas históricas válidas. Quantidades/recusas/replays têm evidência própria, com esse desvio explícito. Não corrigir dados ou repetir a jornada para ocultá-lo.

Correção do helper proposta e aplicada depois de preservar a versão executada: emissão manual atual; sem chegada futura de um dia; guarda recursiva antes do envio HTTP de campos de fatos. Regressão negativa isolada deve comprovar recusa anterior ao transporte. Erro do auxiliar, sem achado concreto no backend.
