import { isObject } from "../../contracts/runtime";
export function ImportPreview({ data }: { data: unknown }) {
    if (!isObject(data)) return null;
    const errors = Array.isArray(data.erros) ? data.erros.filter(isObject) : [];
    return (
        <section aria-label="Situação da prévia Excel">
            <h4>
                Prévia {String(data.id)} · revisão {String(data.versao)} ·{" "}
                {String(data.situacao)}
            </h4>
            <p>
                {data.confirmadaEm
                    ? "Importação confirmada pelo retorno fictício."
                    : "Prévia consultada; nenhum endereço foi criado por esta consulta."}
            </p>
            {errors.map((e, i) => (
                <p role="alert" key={i}>
                    Linha {String(e.linha)} · coluna {String(e.coluna)}:{" "}
                    {String(e.mensagem)}
                </p>
            ))}
            <p>Hash do arquivo consultado: {String(data.arquivoHash)}</p>
        </section>
    );
}
