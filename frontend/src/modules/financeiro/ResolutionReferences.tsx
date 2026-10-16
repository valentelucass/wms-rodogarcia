import { useContext } from "react";
import { FormReferences } from "../../components/FormReferences";
import type { Values } from "../../contracts/runtime";
import { businessTitle } from "../../domain/referencePresentation";
import { information } from "../../content/information";
import { Icon } from "../../design-system/Icon";

const targets = [
    ["PedidoEntradaDto.Resumo", "PEDIDO_ENTRADA", "pedido de entrada"],
    ["PedidoSaidaDto.Detalhe", "PEDIDO_SAIDA", "pedido de saída"],
    ["FechamentoCobrancaDto.Fechamento", "FECHAMENTO", "fechamento"],
];
export function ResolutionReferences({
    onChange,
    value,
    disabled = false,
}: {
    onChange: (value: Values) => void;
    value?: Values;
    disabled?: boolean;
}) {
    const { records } = useContext(FormReferences);
    const choices = targets.flatMap(([dto, tipo, title]) =>
        (records?.[dto] ?? []).map((row) => ({ row, dto, tipo, title })),
    );
    return (
        <div className="resolution-references">
            <p className="hint">
                {information.resolutionReferences.description}
            </p>
            {choices.length ? (
                <div className="resolution-reference-list">
                    {choices.map(({ row, dto, tipo, title }) => {
                        const selected =
                            value?.tipo === tipo &&
                            String(value?.compromissoId) === String(row.id);
                        const name =
                            businessTitle(row, dto, records) ??
                            (row.id != null
                                ? `Registro ${String(row.id)}`
                                : "Compromisso sem identificação");
                        return (
                            <div
                                className="resolution-reference"
                                key={tipo + ":" + String(row.id)}
                                data-selected={selected}
                            >
                                <div className="resolution-reference-name">
                                    <span className="hint">
                                        {title.charAt(0).toUpperCase() +
                                            title.slice(1)}
                                    </span>
                                    <strong>{name}</strong>
                                    {selected && (
                                        <span className="resolution-reference-selected">
                                            Compromisso selecionado
                                        </span>
                                    )}
                                </div>
                                <button
                                    type="button"
                                    className="action-details"
                                    disabled={disabled}
                                    aria-label={`Usar ${title} ${name} como compromisso`}
                                    onClick={() =>
                                        onChange({
                                            tipo,
                                            compromissoId: row.id,
                                        })
                                    }
                                >
                                    <Icon name="bookmark" />
                                    Usar compromisso
                                </button>
                            </div>
                        );
                    })}
                </div>
            ) : (
                <p className="hint">
                    {information.resolutionReferences.footer}
                </p>
            )}
        </div>
    );
}
