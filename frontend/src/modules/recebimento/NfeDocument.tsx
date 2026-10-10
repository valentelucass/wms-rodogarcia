import { useEffect, useState } from "react";
import { call, type Transport } from "../../api/client";
import type {
    NfeImportacaoDto_Documento,
    NfeImportacaoDto_DocumentoSalvo,
} from "../../contracts/types";

const value = (v: unknown) => (v == null || v === "" ? "—" : String(v));
function Info({ fields }: { fields: Record<string, unknown> }) {
    return (
        <dl className="nfe-fields">
            {Object.entries(fields).map(([label, v]) => (
                <div key={label}>
                    <dt>{label}</dt>
                    <dd>{value(v)}</dd>
                </div>
            ))}
        </dl>
    );
}
export function NfeDocument({
    documento: d,
}: {
    documento: NfeImportacaoDto_Documento;
}) {
    return (
        <section
            className="nfe-document"
            aria-label="Documento original da NF-e"
        >
            <h3>
                NF-e {d.numero} · série {d.serie}
            </h3>
            <Info
                fields={{
                    "Chave de acesso": d.chaveAcesso,
                    "Modelo / versão": `${d.modelo} / ${d.versao}`,
                    "Emissão informada": d.emitidaEm,
                    "Natureza da operação": d.naturezaOperacao,
                    "Operação do emitente":
                        d.tipoOperacao === "1"
                            ? "Saída — pode representar entrada no armazém"
                            : "Entrada",
                    "Ambiente fiscal":
                        d.ambiente === "1" ? "Produção" : "Homologação",
                    "Emitente / proprietário": d.emitente?.nome,
                    "Documento do emitente": d.emitente?.documento,
                    "Destinatário original": d.destinatario?.nome,
                    "Documento do destinatário": d.destinatario?.documento,
                    "Valor total da nota": d.valorTotal,
                }}
            />
            <div className="table-scroll">
                <table>
                    <caption>Itens originais da nota · sem agrupamento</caption>
                    <thead>
                        <tr>
                            {[
                                "Item",
                                "Código / descrição",
                                "Comercial",
                                "Tributável",
                                "Valores",
                                "Classificação",
                            ].map((t) => (
                                <th scope="col" key={t}>
                                    {t}
                                </th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {d.itens?.map((i) => (
                            <tr key={i.numeroItem}>
                                <th scope="row">{i.numeroItem}</th>
                                <td>
                                    {i.codigo}
                                    <br />
                                    {i.descricao}
                                    <br />
                                    {i.informacoesAdicionais}
                                </td>
                                <td>
                                    {i.quantidadeComercial} {i.unidadeComercial}
                                    <br />
                                    GTIN: {value(i.gtin)}
                                </td>
                                <td>
                                    {i.quantidadeTributavel}{" "}
                                    {i.unidadeTributavel}
                                    <br />
                                    GTIN: {value(i.gtinTributavel)}
                                </td>
                                <td>
                                    Unitário: {i.valorUnitario}
                                    <br />
                                    Produto: {i.valorProduto}
                                </td>
                                <td>
                                    NCM: {i.ncm}
                                    <br />
                                    CFOP: {i.cfop}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
            <h4>Volumes de transporte</h4>
            {d.volumes?.length ? (
                d.volumes.map((v, index) => (
                    <Info
                        key={index}
                        fields={{
                            "Quantidade de volumes": v.quantidade,
                            Espécie: v.especie,
                            "Peso líquido": v.pesoLiquido,
                            "Peso bruto": v.pesoBruto,
                        }}
                    />
                ))
            ) : (
                <p>Não informados.</p>
            )}
            <p>
                Volumes e pesos não definem unidades de estoque, pallets ou
                posições.
            </p>
            <h4>Protocolo informado no arquivo</h4>
            {d.protocolo ? (
                <Info
                    fields={{
                        Situação: `${d.protocolo.codigoSituacao} · ${d.protocolo.motivo}`,
                        "Número do protocolo": d.protocolo.numero,
                        "Recebimento pela SEFAZ": d.protocolo.recebidoEm,
                        "Chave do protocolo": d.protocolo.chave,
                        "Ambiente do protocolo": d.protocolo.ambiente,
                    }}
                />
            ) : (
                <p>Sem protocolo no arquivo.</p>
            )}
            <Info
                fields={{
                    "Informações complementares": d.informacoesComplementares,
                    "Informações ao fisco": d.informacoesFisco,
                }}
            />
            <p>
                A emissão e o protocolo não comprovam chegada física. A leitura
                local não consulta a situação atual nem verifica a assinatura na
                SEFAZ.
            </p>
        </section>
    );
}
export function downloadXml(xml: string, chave: string | null) {
    const url = URL.createObjectURL(
        new Blob([xml], { type: "application/xml;charset=utf-8" }),
    );
    const a = document.createElement("a");
    a.href = url;
    a.download = `NFe-${chave ?? "original"}.xml`;
    a.click();
    URL.revokeObjectURL(url);
}
export function SavedNfeDocument({
    transport,
    pedidoId,
    notaId,
    disabled,
}: {
    transport: Transport;
    pedidoId: string;
    notaId: string;
    disabled: boolean;
}) {
    const [data, setData] = useState<NfeImportacaoDto_DocumentoSalvo>();
    const [open, setOpen] = useState(false),
        [pending, setPending] = useState(false),
        [error, setError] = useState("");
    useEffect(() => {
        if (!open || data) return;
        const controller = new AbortController();
        setPending(true);
        setError("");
        call(
            transport,
            "PedidoEntradaXmlController.documento",
            undefined,
            { pedidoId, notaId },
            {},
            controller.signal,
        )
            .then(setData)
            .catch((e) => {
                if (!controller.signal.aborted)
                    setError(
                        e instanceof Error
                            ? e.message
                            : "Não foi possível consultar o XML.",
                    );
            })
            .finally(() => {
                if (!controller.signal.aborted) setPending(false);
            });
        return () => controller.abort();
    }, [transport, pedidoId, notaId, open, data]);
    return (
        <div>
            <button
                type="button"
                disabled={disabled || pending}
                onClick={() => setOpen(!open)}
            >
                {open ? "Ocultar documento original" : "Ver documento original"}
            </button>
            {open && (
                <>
                    {pending && <p role="status">Consultando documento…</p>}
                    {error && <p role="alert">{error}</p>}
                    {data?.documento && (
                        <>
                            <NfeDocument documento={data.documento} />
                            <button
                                type="button"
                                onClick={() =>
                                    downloadXml(
                                        data.xmlOriginal ?? "",
                                        data.documento?.chaveAcesso ?? null,
                                    )
                                }
                            >
                                Baixar XML original
                            </button>
                        </>
                    )}
                </>
            )}
        </div>
    );
}
