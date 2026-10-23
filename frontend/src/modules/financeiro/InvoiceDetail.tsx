import { useEffect, useRef, useState, type ComponentProps } from "react";
import { isObject, type Values } from "../../contracts/runtime";
import type { RecordDialog } from "../../components/records/RecordDialog";
import { ReferenceValue } from "../../components/context/ReferencePresentation";
import { StatusBadge } from "../../design-system/StatusBadge";
import { Result } from "../../components/Result";
import { checkInvoiceVersion, invoiceRead } from "./invoiceRead";
import {
    invoiceDate,
    invoiceMoney,
    invoicePeriod,
} from "./invoicePresentation";

type DialogProps = ComponentProps<typeof RecordDialog>;
type Props = Pick<
    DialogProps,
    "transport" | "perfil" | "onReceipt" | "onRecord"
> & { invoice: Values };

export function InvoiceDetail(props: Props) {
    const { invoice, transport, perfil } = props;
    const callbacks = useRef(props);
    callbacks.current = props;
    const [revision, setRevision] = useState(0);
    const key = `${invoice.id}/${invoice.versaoAtual}/${invoice.versao}/${revision}`;
    const [state, setState] = useState<{
        key: string;
        version?: Values;
        calculation?: Values;
        error?: string;
    }>({ key: "" });
    const [memoryOpen, setMemoryOpen] = useState(false);
    useEffect(() => {
        const abort = new AbortController();
        let current = true;
        void (async () => {
            try {
                const version = await invoiceRead(
                    transport,
                    "FechamentoCobrancaController.versao",
                    { id: invoice.id, numero: invoice.versaoAtual },
                    perfil,
                    abort.signal,
                );
                checkInvoiceVersion(version.data, invoice);
                if (!current) return;
                callbacks.current.onReceipt(
                    version.receipt,
                    version.request.endpoint.response,
                    version.request.endpoint.id,
                    version.request,
                );
                callbacks.current.onRecord(
                    version.data,
                    "FechamentoCobrancaDto.Versao",
                );
                setState({ key, version: version.data });
                const calculation = await invoiceRead(
                    transport,
                    "CalculoCobrancaController.consultar",
                    { id: version.data.calculoId },
                    perfil,
                    abort.signal,
                );
                if (!current) return;
                const data = calculation.data;
                if (
                    String(data.id) !== String(version.data.calculoId) ||
                    [
                        "clienteId",
                        "armazemId",
                        "periodoInicio",
                        "periodoFim",
                    ].some(
                        (field) =>
                            String(data[field]) !== String(invoice[field]),
                    )
                )
                    throw new Error(
                        "O cálculo recebido pertence a outra fatura, período ou cliente.",
                    );
                callbacks.current.onReceipt(
                    calculation.receipt,
                    calculation.request.endpoint.response,
                    calculation.request.endpoint.id,
                    calculation.request,
                );
                callbacks.current.onRecord(
                    data,
                    "CalculoCobrancaDto.Resultado",
                );
                setState({ key, version: version.data, calculation: data });
            } catch (error) {
                if (current)
                    setState((previous) => ({
                        ...(previous.key === key ? previous : {}),
                        key,
                        error:
                            error instanceof Error
                                ? error.message
                                : "Não foi possível consultar a fatura.",
                    }));
            }
        })();
        return () => {
            current = false;
            abort.abort();
        };
    }, [key, invoice, transport, perfil]);
    const data = state.key === key ? state : undefined;
    const calculation = data?.calculation;
    const memory = isObject(calculation?.memoria) ? calculation.memoria : {};
    const services = Array.isArray(memory.servicos)
        ? memory.servicos.filter(isObject)
        : [];
    const days = Array.isArray(memory.diarias)
        ? memory.diarias.filter(isObject)
        : [];
    return (
        <div className="invoice-detail">
            <dl className="invoice-context">
                <div>
                    <dt>Cliente</dt>
                    <dd>
                        <ReferenceValue
                            field="clienteId"
                            value={invoice.clienteId}
                            row={invoice}
                            type="FechamentoCobrancaDto.Fechamento"
                        />
                    </dd>
                </div>
                <div>
                    <dt>Período de fechamento</dt>
                    <dd>
                        {invoicePeriod(
                            invoice.periodoInicio,
                            invoice.periodoFim,
                        )}
                    </dd>
                </div>
                <div>
                    <dt>Situação</dt>
                    <dd>
                        <StatusBadge value={invoice.situacao} />
                    </dd>
                </div>
                <div>
                    <dt>Valor da fatura</dt>
                    <dd className="invoice-total">
                        {data?.version
                            ? invoiceMoney(
                                  data.version.saldo,
                                  calculation?.moeda ?? "BRL",
                              )
                            : "Consultando…"}
                        {data?.version?.natureza === "CREDITO" && (
                            <span> · Crédito</span>
                        )}
                    </dd>
                </div>
            </dl>
            {!data?.calculation && !data?.error && (
                <p role="status">
                    Consultando os serviços e os valores desta fatura…
                </p>
            )}
            {data?.error && (
                <div role="alert">
                    <p>{data.error}</p>
                    <button
                        type="button"
                        onClick={() => setRevision((value) => value + 1)}
                    >
                        Consultar fatura novamente
                    </button>
                </div>
            )}
            {calculation && (
                <>
                    <section aria-label="Serviços cobrados">
                        <h3>Serviços cobrados</h3>
                        {services.length > 0 ? (
                            <div className="invoice-table-scroll">
                                <table className="invoice-table">
                                    <thead>
                                        <tr>
                                            <th>Serviço</th>
                                            <th>Realizado em</th>
                                            <th>Quantidade</th>
                                            <th>Valor</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {services.map((service, index) => (
                                            <tr
                                                key={`${service.fatoId}/${index}`}
                                            >
                                                <td>
                                                    <ReferenceValue
                                                        field="servicoId"
                                                        value={
                                                            service.servicoId
                                                        }
                                                        row={service}
                                                        type="CalculoCobrancaDto.Servico"
                                                    />
                                                </td>
                                                <td>
                                                    {invoiceDate(
                                                        service.executadoEm,
                                                    )}
                                                </td>
                                                <td>
                                                    <ReferenceValue
                                                        field="quantidade"
                                                        value={
                                                            service.quantidade
                                                        }
                                                        row={service}
                                                        type="CalculoCobrancaDto.Servico"
                                                    />
                                                </td>
                                                <td>
                                                    {invoiceMoney(
                                                        service.valor,
                                                        calculation.moeda,
                                                    )}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        ) : (
                            <p>
                                Não há lançamentos de serviços avulsos neste
                                cálculo.
                            </p>
                        )}
                    </section>
                    {days.length > 0 && (
                        <details className="invoice-section">
                            <summary>Armazenagem por dia</summary>
                            <div className="invoice-table-scroll">
                                <table className="invoice-table">
                                    <thead>
                                        <tr>
                                            <th>Dia</th>
                                            <th>Pico cobrável</th>
                                            <th>Valor</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {days.map((day, index) => (
                                            <tr key={`${day.data}/${index}`}>
                                                <td>{invoiceDate(day.data)}</td>
                                                <td>
                                                    <ReferenceValue
                                                        field="picoCobravel"
                                                        value={day.picoCobravel}
                                                        row={day}
                                                        type="CalculoCobrancaDto.Diaria"
                                                    />
                                                </td>
                                                <td>
                                                    {invoiceMoney(
                                                        day.valor,
                                                        calculation.moeda,
                                                    )}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        </details>
                    )}
                    <dl className="invoice-context">
                        <div>
                            <dt>Subtotal conhecido</dt>
                            <dd>
                                {invoiceMoney(
                                    calculation.subtotalConhecido,
                                    calculation.moeda,
                                )}
                            </dd>
                        </div>
                        <div>
                            <dt>Complemento do mínimo</dt>
                            <dd>
                                {invoiceMoney(
                                    calculation.minimoCalculado,
                                    calculation.moeda,
                                )}
                            </dd>
                        </div>
                        <div>
                            <dt>GRIS</dt>
                            <dd>
                                {invoiceMoney(
                                    calculation.grisCalculado,
                                    calculation.moeda,
                                )}
                            </dd>
                        </div>
                        <div>
                            <dt>Total do cálculo</dt>
                            <dd>
                                {invoiceMoney(
                                    calculation.total,
                                    calculation.moeda,
                                )}
                            </dd>
                        </div>
                    </dl>
                    {Array.isArray(calculation.pendencias) &&
                        calculation.pendencias.length > 0 && (
                            <section
                                className="invoice-warning"
                                aria-label="Pendências da fatura"
                            >
                                <h3>Pendências para conferência</h3>
                                <Result
                                    data={calculation.pendencias}
                                    type="List<CalculoCobrancaDto.Pendencia>"
                                />
                            </section>
                        )}
                    <details
                        className="invoice-section"
                        onToggle={(event) =>
                            setMemoryOpen(event.currentTarget.open)
                        }
                    >
                        <summary>
                            Origens e composição completa dos valores
                        </summary>
                        {memoryOpen && (
                            <Result
                                data={calculation}
                                type="CalculoCobrancaDto.Resultado"
                            />
                        )}
                    </details>
                </>
            )}
            {data?.version && (
                <details className="invoice-section">
                    <summary>Ajustes e histórico da fatura</summary>
                    <Result
                        data={data.version}
                        type="FechamentoCobrancaDto.Versao"
                        onSelect={props.onRecord}
                    />
                </details>
            )}
            <section className="invoice-esl" aria-label="Minuta no ESL">
                <h3>Minuta no ESL</h3>
                <p>
                    A minuta será criada no ambiente próprio da Dalga. A emissão
                    da NFS-e é feita no ESL.
                </p>
                <button type="button" disabled>
                    Enviar minuta ao ESL
                </button>
                <p className="hint">
                    Envio ainda indisponível. A integração está em preparação.
                </p>
            </section>
        </div>
    );
}
