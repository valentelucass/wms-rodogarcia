import { isObject, type Values } from "../../contracts/runtime";
import { ReferenceValue } from "../../components/context/ReferencePresentation";
import { Result } from "../../components/Result";
import { StatusBadge } from "../../design-system/StatusBadge";
import { display } from "../../domain/labels";
import { information } from "../../content/information";

const type = "CargaInicialDto.Resultado";
const dataType = "CargaInicialDto.Dados";
type Entry = [string, string];

function LoadValues({
    data,
    fields,
    schema = type,
}: {
    data: Values;
    fields: Entry[];
    schema?: string;
}) {
    return (
        <dl className="initial-load-values">
            {fields.map(([field, title]) => (
                <div key={field}>
                    <dt>{title}</dt>
                    <dd>
                        {data[field] == null || data[field] === "" ? (
                            "Não informado"
                        ) : ["id", "versao", "numero", "conteudoHash"].includes(
                              field,
                          ) ? (
                            display(data[field])
                        ) : field === "tipo" ? (
                            ({
                                PALLET: "Pallet",
                                BOBINA: "Bobina",
                                VOLUME: "Volume",
                            }[String(data[field])] ?? display(data[field]))
                        ) : field === "condicao" ? (
                            ({
                                BOA: "Boa",
                                AVARIADA: "Avariada",
                                QUARENTENA: "Quarentena",
                                TRIAGEM: "Triagem",
                            }[String(data[field])] ?? display(data[field]))
                        ) : (
                            <ReferenceValue
                                field={field}
                                value={data[field]}
                                row={data}
                                type={schema}
                            />
                        )}
                    </dd>
                </div>
            ))}
        </dl>
    );
}

export function InitialLoadDetail({ data }: { data: Values }) {
    const revision = isObject(data.revisao) ? data.revisao : undefined;
    const values =
        revision && isObject(revision.dados) ? revision.dados : undefined;
    const units =
        values && Array.isArray(values.unidades) ? values.unidades : undefined;
    return (
        <div className="initial-load-detail">
            <header className="initial-load-detail-heading">
                <div>
                    <span className="hint">Referência da carga</span>
                    <h3>{display(data.referencia)}</h3>
                </div>
                <StatusBadge value={data.situacao} />
            </header>
            <section className="initial-load-detail-section">
                <h3>Identificação da carga</h3>
                <LoadValues
                    data={data}
                    fields={[
                        ["clienteId", "Cliente"],
                        ["armazemId", "Armazém"],
                        ["produtoId", "Produto / SKU"],
                        ["quantidadeEstagio", "Quantidade da carga"],
                        ["entradaId", "Entrada regularizada"],
                    ]}
                />
            </section>
            <section className="initial-load-detail-section">
                <h3>Pendências</h3>
                {Array.isArray(data.pendencias) ? (
                    data.pendencias.length > 0 ? (
                        <ul className="initial-load-pending">
                            {data.pendencias.map((value, i) => (
                                <li key={i}>{display(value)}</li>
                            ))}
                        </ul>
                    ) : (
                        <p className="hint">
                            Nenhuma pendência informada nesta consulta.
                        </p>
                    )
                ) : (
                    <p className="hint">
                        As pendências não foram informadas nesta consulta.
                    </p>
                )}
            </section>
            {values ? (
                <>
                    <section className="initial-load-detail-section">
                        <h3>Origem e datas</h3>
                        <LoadValues
                            data={{ ...values, entradaId: data.entradaId }}
                            schema={dataType}
                            fields={[
                                ["entradaExistenteId", "Entrada de origem"],
                                ["referenciaPedido", "Referência do pedido"],
                                ["chegadaReal", "Chegada real"],
                                ["dataFifo", "Data FIFO"],
                                ["lote", "Lote"],
                                ["validade", "Validade"],
                            ]}
                        />
                        {isObject(values.nota) ? (
                            <details className="initial-load-note">
                                <summary>Nota fiscal de origem</summary>
                                <Result
                                    data={values.nota}
                                    type="PedidoEntradaDto.NotaManual"
                                />
                            </details>
                        ) : (
                            <p className="hint">Nota fiscal não informada.</p>
                        )}
                    </section>
                    <section className="initial-load-detail-section">
                        <h3>Quantidades levantadas</h3>
                        <LoadValues
                            data={values}
                            schema={dataType}
                            fields={[
                                ["quantidadeBoa", "Quantidade boa"],
                                ["quantidadeAvariada", "Quantidade avariada"],
                            ]}
                        />
                    </section>
                    <section className="initial-load-detail-section">
                        <header>
                            <h3>Unidades informadas</h3>
                            {units && (
                                <span className="hint">
                                    {units.length}{" "}
                                    {units.length === 1
                                        ? "unidade"
                                        : "unidades"}
                                </span>
                            )}
                        </header>
                        {units?.length ? (
                            <div className="initial-load-unit-list">
                                {units.map((unit, i) => (
                                    <article
                                        className="initial-load-unit"
                                        key={i}
                                        aria-label={`Unidade ${i + 1}`}
                                    >
                                        <h4>Unidade {i + 1}</h4>
                                        <LoadValues
                                            data={isObject(unit) ? unit : {}}
                                            schema="UnidadeLogisticaDto.NovaUnidade"
                                            fields={[
                                                [
                                                    "embalagemId",
                                                    "Embalagem / DUN",
                                                ],
                                                ["tipo", "Tipo"],
                                                ["condicao", "Condição"],
                                                ["quantidade", "Quantidade"],
                                            ]}
                                        />
                                    </article>
                                ))}
                            </div>
                        ) : (
                            <p className="hint">
                                {information.initialLoadUnitsEmpty.description}
                            </p>
                        )}
                    </section>
                    <section className="initial-load-detail-section">
                        <h3>Fonte dos dados</h3>
                        <p className="initial-load-source">
                            {display(values.fonte)}
                        </p>
                    </section>
                </>
            ) : (
                <p className="hint">
                    Os dados da revisão não foram informados nesta consulta.
                </p>
            )}
            <section className="initial-load-detail-section">
                <h3>Etiquetas</h3>
                <LoadValues
                    data={data}
                    fields={[["etiquetaFornecida", "Etiqueta fornecida"]]}
                />
                {Array.isArray(data.etiquetas) && data.etiquetas.length > 0 ? (
                    <ul className="initial-load-labels">
                        {data.etiquetas.map((value, i) => (
                            <li key={i}>
                                <span className="hint">Etiqueta {i + 1}</span>
                                <span>{display(value)}</span>
                            </li>
                        ))}
                    </ul>
                ) : (
                    <p className="hint">
                        Nenhuma etiqueta de unidade informada nesta consulta.
                    </p>
                )}
            </section>
            <details className="initial-load-technical">
                <summary>Identificação da revisão</summary>
                <LoadValues
                    data={{
                        id: data.id,
                        versao: data.versao,
                        numero: revision?.numero,
                        conteudoHash: revision?.conteudoHash,
                    }}
                    fields={[
                        ["id", "Identificador da carga"],
                        ["versao", "Versão atual"],
                        ["numero", "Revisão"],
                        ["conteudoHash", "Hash do conteúdo"],
                    ]}
                />
            </details>
        </div>
    );
}
