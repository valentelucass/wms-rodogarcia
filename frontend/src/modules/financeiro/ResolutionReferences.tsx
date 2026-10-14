import { useContext } from "react";
import { FormReferences } from "../../components/FormReferences";
import type { Values } from "../../contracts/runtime";
import { businessTitle } from "../../domain/referencePresentation";

const targets = [
    ["PedidoEntradaDto.Resumo", "PEDIDO_ENTRADA", "pedido de entrada"],
    ["PedidoSaidaDto.Detalhe", "PEDIDO_SAIDA", "pedido de saída"],
    ["FechamentoCobrancaDto.Fechamento", "FECHAMENTO", "fechamento"],
];
export function ResolutionReferences({
    onChange,
}: {
    onChange: (value: Values) => void;
}) {
    const { records } = useContext(FormReferences);
    return (
        <div>
            <p>
                Escolha o compromisso consultado. O servidor verifica o vínculo
                histórico e a autorização do Gestor.
            </p>
            {targets.flatMap(([dto, tipo, title]) =>
                (records?.[dto] ?? []).map((row) => (
                    <button
                        type="button"
                        key={tipo + ":" + String(row.id)}
                        onClick={() =>
                            onChange({ tipo, compromissoId: row.id })
                        }
                    >
                        Usar {title} {businessTitle(row, dto, records)} como
                        compromisso
                    </button>
                )),
            )}
        </div>
    );
}
