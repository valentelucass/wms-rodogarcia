import { useLayoutEffect, useRef } from "react";
import { isObject, type Values } from "../../contracts/runtime";
import { Result } from "../../components/Result";
export function CollectorReceipt({
    data,
    type,
    onSelect,
}: {
    data: unknown;
    type: string;
    onSelect: (v: Values, type: string) => void;
}) {
    const ref = useRef<HTMLElement>(null);
    useLayoutEffect(() => {
        ref.current?.focus();
    }, [data]);
    const stock =
        isObject(data) && isObject(data.estoque) ? data.estoque : undefined;
    const unit = stock && isObject(stock.unidade) ? stock.unidade : undefined;
    return (
        <section
            ref={ref}
            tabIndex={-1}
            aria-label="Resumo confirmado no coletor"
        >
            <h3>Confirmação recebida</h3>
            {unit && (
                <p>
                    Unidade {String(unit.id)} · UUID {String(unit.codigo)} ·
                    revisão {String(unit.versao)}. Destino confirmado na
                    resposta abaixo.
                </p>
            )}
            {stock && Array.isArray(stock.posicoes) && (
                <p>
                    Posições confirmadas:{" "}
                    {stock.posicoes
                        .filter(isObject)
                        .map((p) => String(p.codigo))
                        .join(", ")}
                </p>
            )}
            <details>
                <summary>Ver detalhes da confirmação</summary>
                <Result data={data} type={type} onSelect={onSelect} />
            </details>
        </section>
    );
}
