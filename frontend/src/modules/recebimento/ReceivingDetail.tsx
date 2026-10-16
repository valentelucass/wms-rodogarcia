import { SavedNfeDocument } from "./NfeDocument";
import type { Transport } from "../../api/client";
import { isObject, type Values } from "../../contracts/runtime";
import { StatusBadge } from "../../design-system/StatusBadge";
import { recordRoot } from "../../domain/recordContext";
import { display } from "../../domain/labels";
import { formatProductQuantity } from "../../domain/quantityPresentation";
import {
    receivingDate,
    receivingNotes,
    receivingState,
    firstArrival,
    situationLabel,
    receivingSections,
    type ReceivingSection,
} from "./orderPresentation";
import { ReceivingSummary } from "./ReceivingSummary";
import { information } from "../../content/information";
import type { Workflow } from "../../domain/workflow";
import { useReferenceCatalog } from "../../components/context/ReferenceCatalog";
import { formatDate } from "../../domain/referencePresentation";
import { Icon } from "../../design-system/Icon";

export function ReceivingDetail({
    record,
    section,
    onSection,
    onSelect,
    locked,
    workflow,
    transport,
}: {
    transport: Transport;
    record: Values;
    section: ReceivingSection;
    onSection: (section: ReceivingSection) => void;
    onSelect: (row: Values, type: string) => void;
    locked: boolean;
    workflow: Workflow;
}) {
    const catalog = useReferenceCatalog();
    const root = recordRoot(record),
        notes = receivingNotes(record);
    return (
        <section
            className="receiving-detail"
            aria-label="Pedido de entrada selecionado"
        >
            <div className="receiving-summary">
                <strong>Pedido {String(root.referencia)}</strong>
                <StatusBadge
                    value={root.situacao}
                    label={situationLabel(root.situacao)}
                />
                <StatusBadge value={receivingState(record)} />
                <span>Revisão {String(root.versao)}</span>
            </div>
            <nav className="receiving-sections" aria-label="Seções do pedido">
                {receivingSections.map((title) => (
                    <button
                        type="button"
                        key={title}
                        disabled={locked}
                        aria-current={section === title ? "page" : undefined}
                        onClick={() => onSection(title)}
                    >
                        {title}
                    </button>
                ))}
            </nav>
            {section === "Dados gerais" && (
                <div className="receiving-general">
                    <dl>
                        {Object.entries({
                            Cliente:
                                catalog?.clientes.find(
                                    (item) =>
                                        item.id === String(root.clienteId),
                                )?.nome ?? "Cliente indisponível",
                            Armazém:
                                catalog?.armazens.find(
                                    (item) =>
                                        item.id === String(root.armazemId),
                                )?.nome ?? "Armazém indisponível",
                            "Origem da criação":
                                root.origemCriacao === "XML" ? "XML" : "Manual",
                            "Criado em": receivingDate(root.criadoEm),
                            "Primeira chegada física": receivingDate(
                                firstArrival(record),
                            ),
                            "Efetivado em": receivingDate(root.efetivadoEm),
                            "Motivo da conclusão": root.motivoConclusao ?? "—",
                        }).map(([label, value]) => (
                            <div key={label}>
                                <dt>{label}</dt>
                                <dd>{String(value ?? "—")}</dd>
                            </div>
                        ))}
                    </dl>
                    <p>
                        Notas, chegada física, conferência e efetivação são
                        informações distintas. Inclua notas e registre ou
                        corrija chegadas pelas ações das seções deste pedido.
                    </p>
                </div>
            )}
            {(section === "Notas e itens" || section === "Conferência") && (
                <>
                    <div className="receiving-note-guidance">
                        <p>{information.receivingNotes.description}</p>
                        {root.situacao !== "RASCUNHO" && (
                            <p className="hint">
                                {information.receivingNotes.footer}
                            </p>
                        )}
                    </div>
                    {notes.length === 0 && (
                        <p className="empty">
                            Este pedido ainda não possui notas. Inclua uma nota
                            manual ou importe um XML existente.
                        </p>
                    )}
                    {section === "Notas e itens" &&
                        notes.map((note) => (
                            <section
                                className="receiving-note"
                                key={String(note.id)}
                                aria-label={`Nota ${String(note.serie)}/${String(note.numero)}`}
                            >
                                <header className="receiving-note-header">
                                    <div>
                                        <h3>Nota {String(note.numero)}</h3>
                                        <p className="hint">
                                            Série {String(note.serie)}
                                        </p>
                                    </div>
                                    <span className="receiving-note-origin">
                                        {note.xmlVinculado
                                            ? "XML vinculado"
                                            : "Nota manual"}
                                    </span>
                                </header>
                                <dl className="receiving-note-meta">
                                    <div>
                                        <dt>Emissão da nota</dt>
                                        <dd>{formatDate(note.emissao)}</dd>
                                    </div>
                                    <div>
                                        <dt>Primeira chegada física</dt>
                                        <dd>
                                            {receivingDate(
                                                note.primeiraChegada,
                                            )}
                                        </dd>
                                    </div>
                                </dl>
                                <div
                                    className="table-scroll receiving-note-items"
                                    role="region"
                                    aria-label={`Itens da nota ${String(note.numero)}`}
                                    tabIndex={0}
                                >
                                    <table>
                                        <caption>
                                            Itens da nota {String(note.numero)}
                                        </caption>
                                        <thead>
                                            <tr>
                                                {[
                                                    "Item / SKU",
                                                    "Unidade do produto",
                                                    "Previsto",
                                                    "Bom físico",
                                                    "Avariado físico",
                                                    "Diferença",
                                                    "Ação",
                                                ].map((title) => (
                                                    <th
                                                        key={title}
                                                        scope="col"
                                                        className={
                                                            [
                                                                "Previsto",
                                                                "Bom físico",
                                                                "Avariado físico",
                                                                "Diferença",
                                                            ].includes(title)
                                                                ? "quantity-cell"
                                                                : undefined
                                                        }
                                                    >
                                                        {title}
                                                    </th>
                                                ))}
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {(Array.isArray(note.itens)
                                                ? note.itens
                                                : []
                                            )
                                                .filter(isObject)
                                                .map((item) => (
                                                    <tr key={String(item.id)}>
                                                        <th scope="row">
                                                            {String(
                                                                item.numeroItem,
                                                            )}{" "}
                                                            · {String(item.sku)}
                                                            <details className="receiving-note-item-identifiers">
                                                                <summary>
                                                                    Identificadores
                                                                </summary>
                                                                <small className="receiving-order-id">
                                                                    Item{" "}
                                                                    {String(
                                                                        item.id,
                                                                    )}{" "}
                                                                    · produto{" "}
                                                                    {String(
                                                                        item.produtoId,
                                                                    )}
                                                                </small>
                                                            </details>
                                                        </th>
                                                        <td>
                                                            {display(
                                                                item.unidadeMedida,
                                                            )}
                                                        </td>
                                                        {[
                                                            item.prevista,
                                                            item.recebidaBoa,
                                                            item.recebidaAvariada,
                                                            item.diferenca,
                                                        ].map(
                                                            (value, index) => (
                                                                <td
                                                                    key={index}
                                                                    className="quantity-cell"
                                                                >
                                                                    {formatProductQuantity(
                                                                        value,
                                                                        item.unidadeMedida,
                                                                    )}
                                                                </td>
                                                            ),
                                                        )}
                                                        <td>
                                                            <button
                                                                type="button"
                                                                className="action-select"
                                                                aria-label={`Selecionar item ${String(item.id)} da nota ${String(note.numero)}`}
                                                                disabled={
                                                                    locked
                                                                }
                                                                onClick={() => {
                                                                    onSelect(
                                                                        note,
                                                                        "PedidoEntradaDto.Nota",
                                                                    );
                                                                    onSelect(
                                                                        item,
                                                                        "PedidoEntradaDto.ItemConferencia",
                                                                    );
                                                                }}
                                                            >
                                                                <Icon name="contagem" />
                                                                Selecionar item
                                                            </button>
                                                        </td>
                                                    </tr>
                                                ))}
                                        </tbody>
                                    </table>
                                </div>
                                {Boolean(note.xmlVinculado) && (
                                    <SavedNfeDocument
                                        transport={transport}
                                        pedidoId={String(root.id)}
                                        notaId={String(note.id)}
                                        disabled={locked}
                                    />
                                )}
                            </section>
                        ))}
                    {section === "Conferência" && (
                        <>
                            <p className="hint">
                                {information.receivingBobinas.description}
                            </p>
                            <ReceivingSummary
                                workflow={workflow}
                                disabled={locked}
                                onSelectItem={(note, item) => {
                                    onSelect(note, "PedidoEntradaDto.Nota");
                                    onSelect(
                                        item,
                                        "PedidoEntradaDto.ItemConferencia",
                                    );
                                }}
                            />
                            <p>
                                Conferido não significa efetivado. Falta, sobra
                                ou avaria exige a tratativa e o aceite
                                autorizado no comando Efetivar carga integral; o
                                histórico da divergência permanece. Uma chegada
                                incorreta é corrigida por estorno identificado e
                                novo registro.
                            </p>
                            {root.motivoConclusao && (
                                <p>
                                    Tratativa registrada na conclusão:{" "}
                                    {String(root.motivoConclusao)}
                                </p>
                            )}
                        </>
                    )}
                </>
            )}
            {section === "Histórico" && (
                <p>
                    Consulte as chegadas para ver instante real, responsável,
                    observação, itens e estornos. Entradas conferidas mostram o
                    resultado da efetivação. Um estorno conserva o lançamento
                    original e exige Supervisor/Gestor.
                </p>
            )}
        </section>
    );
}
