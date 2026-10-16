import { useEffect, useId, useState } from "react";
import { call, type Transport } from "../../api/client";
import { formatProductQuantity } from "../../domain/quantityPresentation";
import type {
    NfeImportacaoDto_Documento,
    NfeImportacaoDto_DocumentoSalvo,
} from "../../contracts/types";
import { information } from "../../content/information";
import { Icon } from "../../design-system/Icon";

const value = (v: unknown) => (v == null || v === "" ? "—" : String(v));
function Info({
    fields,
    wide = [],
}: {
    fields: Record<string, unknown>;
    wide?: string[];
}) {
    return (
        <dl className="nfe-fields">
            {Object.entries(fields).map(([label, v]) => (
                <div
                    key={label}
                    className={
                        wide.includes(label) ? "nfe-field-wide" : undefined
                    }
                >
                    <dt>{label}</dt>
                    <dd>{value(v)}</dd>
                </div>
            ))}
        </dl>
    );
}
export function NfeDocument({
    documento: d,
    collapseAdditional = false,
}: {
    documento: NfeImportacaoDto_Documento;
    collapseAdditional?: boolean;
}) {
    return (
        <section
            className="nfe-document"
            aria-label="Documento original da NF-e"
        >
            <header className="nfe-document-heading">
                <h3>
                    NF-e {d.numero} · série {d.serie}
                </h3>
                <p className="hint">
                    {d.formato === "EXPORTACAO_OPERACIONAL"
                        ? "Exportação operacional · dados originais preservados"
                        : "NF-e · estrutura conferida pelo esquema local"}
                </p>
            </header>
            {Boolean(d.avisos?.length) && (
                <aside
                    className="nfe-document-notice"
                    aria-label="Conferência do arquivo"
                >
                    <ul>
                        {d.avisos?.map((aviso) => (
                            <li key={aviso}>{aviso}</li>
                        ))}
                    </ul>
                </aside>
            )}
            <section
                className="nfe-document-section"
                aria-label="Identificação do documento"
            >
                <h4>Identificação do documento</h4>
                <Info
                    wide={["Chave de acesso"]}
                    fields={{
                        "Chave de acesso": d.chaveAcesso,
                        "Modelo / versão": `${value(d.modelo)} / ${value(d.versao)}`,
                        "Emissão informada": d.emitidaEm,
                        "Natureza da operação": d.naturezaOperacao,
                        "Operação do emitente":
                            d.tipoOperacao === "1"
                                ? "Saída — pode representar entrada no armazém"
                                : d.tipoOperacao === "0"
                                  ? "Entrada"
                                  : d.tipoOperacao,
                        "Ambiente fiscal":
                            d.ambiente === "1"
                                ? "Produção"
                                : d.ambiente === "2"
                                  ? "Homologação"
                                  : d.ambiente,
                        "Valor total da nota": d.valorTotal,
                    }}
                />
            </section>
            <div className="nfe-document-participants">
                <section
                    className="nfe-document-section"
                    aria-label="Emitente / proprietário"
                >
                    <h4>Emitente / proprietário</h4>
                    <Info
                        wide={["Nome", "Documento do emitente"]}
                        fields={{
                            Nome: d.emitente?.nome,
                            "Documento do emitente": d.emitente?.documento,
                        }}
                    />
                </section>
                <section
                    className="nfe-document-section"
                    aria-label="Destinatário original"
                >
                    <h4>Destinatário original</h4>
                    <Info
                        wide={["Nome", "Documento do destinatário"]}
                        fields={{
                            Nome: d.destinatario?.nome,
                            "Documento do destinatário":
                                d.destinatario?.documento,
                        }}
                    />
                </section>
            </div>
            <div
                className="table-scroll nfe-original-items"
                role="region"
                aria-label="Itens originais da nota"
                tabIndex={0}
            >
                <table>
                    <caption>Itens originais da nota · sem agrupamento</caption>
                    <thead>
                        <tr>
                            {[
                                "Item",
                                "Código / descrição",
                                "Comercial",
                                "Tributável",
                                "Bobinas previstas",
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
                                    {formatProductQuantity(
                                        i.quantidadeComercial,
                                        i.unidadeComercial,
                                    )}{" "}
                                    {i.unidadeComercial}
                                    <br />
                                    GTIN: {value(i.gtin)}
                                </td>
                                <td>
                                    {formatProductQuantity(
                                        i.quantidadeTributavel,
                                        i.unidadeTributavel,
                                    )}{" "}
                                    {i.unidadeTributavel}
                                    <br />
                                    GTIN: {value(i.gtinTributavel)}
                                </td>
                                <td>
                                    {i.bobinasPrevistas == null
                                        ? "—"
                                        : `${i.bobinasPrevistas} bobina · ${i.bobinasPrevistas} posição prevista`}
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
            <details
                className="nfe-document-additional"
                open={!collapseAdditional}
            >
                <summary>
                    Transporte, protocolo e informações adicionais
                </summary>
                <div>
                    <section
                        className="nfe-document-section"
                        aria-label="Volumes de transporte"
                    >
                        <h4>Volumes de transporte</h4>
                        {d.volumes?.length ? (
                            d.volumes.map((v, index) => (
                                <Info
                                    key={index}
                                    fields={{
                                        "Quantidade de volumes": v.quantidade,
                                        Espécie: v.especie,
                                        "Peso líquido informado (kg)":
                                            formatProductQuantity(
                                                v.pesoLiquido,
                                                "KG",
                                            ),
                                        "Peso bruto informado (kg)":
                                            formatProductQuantity(
                                                v.pesoBruto,
                                                "KG",
                                            ),
                                    }}
                                />
                            ))
                        ) : (
                            <p>Não informados.</p>
                        )}
                        <p>
                            Volumes e pesos não definem unidades de estoque,
                            pallets ou posições.
                        </p>
                    </section>
                    <section
                        className="nfe-document-section"
                        aria-label="Protocolo informado no arquivo"
                    >
                        <h4>Protocolo informado no arquivo</h4>
                        {d.protocolo ? (
                            <Info
                                fields={{
                                    Situação: `${d.protocolo.codigoSituacao} · ${d.protocolo.motivo}`,
                                    "Número do protocolo": d.protocolo.numero,
                                    "Recebimento pela SEFAZ":
                                        d.protocolo.recebidoEm,
                                    "Chave do protocolo": d.protocolo.chave,
                                    "Ambiente do protocolo":
                                        d.protocolo.ambiente,
                                }}
                            />
                        ) : (
                            <p>Sem protocolo no arquivo.</p>
                        )}
                    </section>
                    <section
                        className="nfe-document-section"
                        aria-label="Informações adicionais"
                    >
                        <h4>Informações adicionais</h4>
                        <Info
                            wide={[
                                "Informações complementares",
                                "Informações ao fisco",
                            ]}
                            fields={{
                                "Informações complementares":
                                    d.informacoesComplementares,
                                "Informações ao fisco": d.informacoesFisco,
                            }}
                        />
                    </section>
                </div>
            </details>
            <p className="hint">
                {information.receivingOriginalDocument.footer}
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
    const panelId = useId();
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
            .then((result) => {
                if (controller.signal.aborted) return;
                setPending(false);
                setData(result);
            })
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
        <div className="saved-nfe-document">
            <header className="saved-nfe-header">
                <div>
                    <h4>Documento original</h4>
                    <p className="hint">
                        {information.receivingOriginalDocument.description}
                    </p>
                </div>
                <div className="saved-nfe-actions">
                    <button
                        type="button"
                        className={open ? "action-dismiss" : "action-details"}
                        aria-expanded={open}
                        aria-controls={panelId}
                        disabled={disabled || pending}
                        onClick={() => setOpen(!open)}
                    >
                        <Icon name={open ? "close" : "eye"} />
                        {open
                            ? "Ocultar documento original"
                            : "Ver documento original"}
                    </button>
                    {open && data?.documento && (
                        <button
                            type="button"
                            className="action-details"
                            disabled={disabled || pending}
                            onClick={() =>
                                downloadXml(
                                    data.xmlOriginal ?? "",
                                    data.documento?.chaveAcesso ?? null,
                                )
                            }
                        >
                            <Icon name="download" />
                            Baixar XML original
                        </button>
                    )}
                </div>
            </header>
            <div id={panelId} className="saved-nfe-content" hidden={!open}>
                {open && (
                    <>
                        {pending && <p role="status">Consultando documento…</p>}
                        {error && <p role="alert">{error}</p>}
                        {data?.documento && (
                            <NfeDocument
                                documento={data.documento}
                                collapseAdditional
                            />
                        )}
                    </>
                )}
            </div>
        </div>
    );
}
