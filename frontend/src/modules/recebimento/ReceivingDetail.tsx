import { isObject, type Values } from "../../contracts/runtime";
import { recordRoot } from "../../domain/recordContext";
import { display } from "../../domain/labels";
import { receivingDate, receivingNotes, receivingState, firstArrival, situationLabel, receivingSections, type ReceivingSection } from "./orderPresentation";
import { ReceivingSummary } from "./ReceivingSummary";
import type { Workflow } from "../../domain/workflow";

export function ReceivingDetail({ record, section, onSection, onSelect, locked, workflow }: {
    record: Values; section: ReceivingSection; onSection: (section: ReceivingSection) => void;
    onSelect: (row: Values, type: string) => void; locked: boolean; workflow: Workflow;
}) {
    const root = recordRoot(record), notes = receivingNotes(record);
    return <section className="receiving-detail" aria-label="Pedido de entrada selecionado">
        <div className="receiving-summary"><strong>Pedido {String(root.id)} · {String(root.referencia)}</strong><span className="record-status">{situationLabel(root.situacao)}</span><span className="record-status">{receivingState(record)}</span><span>Revisão {String(root.versao)}</span></div>
        <nav className="receiving-sections" aria-label="Seções do pedido">{receivingSections.map((title) => <button type="button" key={title} disabled={locked} aria-current={section === title ? "page" : undefined} onClick={() => onSection(title)}>{title}</button>)}</nav>
        {section === "Dados gerais" && <div className="receiving-general"><dl>{Object.entries({ "Cliente": root.clienteId, "Armazém": root.armazemId, "Criado em": receivingDate(root.criadoEm), "Primeira chegada física": receivingDate(firstArrival(record)), "Efetivado em": receivingDate(root.efetivadoEm), "Motivo da conclusão": root.motivoConclusao ?? "—" }).map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{String(value ?? "—")}</dd></div>)}</dl><p>Notas, chegada física, conferência e efetivação são informações distintas. Dados gerais não têm comando de edição no contrato atual; notas e chegadas usam suas operações próprias.</p></div>}
        {(section === "Notas e itens" || section === "Conferência") && <>
            <p>Selecione o item da nota que será conferido. O produto e sua origem permanecem vinculados ao pedido; registrar XML não confirma chegada física.</p>
            {notes.length === 0 && <p className="empty">Este pedido ainda não possui notas. Inclua uma nota manual ou importe um XML existente.</p>}
            {notes.map((note) => <section className="receiving-note" key={String(note.id)} aria-label={`Nota ${String(note.serie)}/${String(note.numero)}`}><h3>Nota {String(note.serie)}/{String(note.numero)}</h3><p>Emissão {String(note.emissao)} · {note.xmlVinculado ? "XML vinculado" : "Nota manual"} · Primeira chegada {receivingDate(note.primeiraChegada)}</p><div className="table-scroll"><table><caption>Itens da nota {String(note.numero)}</caption><thead><tr>{["Item / SKU", "Previsto", "Bom físico", "Avariado físico", "Diferença", "Ação"].map((title) => <th key={title} scope="col">{title}</th>)}</tr></thead><tbody>{(Array.isArray(note.itens) ? note.itens : []).filter(isObject).map((item) => <tr key={String(item.id)}><th scope="row">{String(item.numeroItem)} · {String(item.sku)}<small className="receiving-order-id">Item {String(item.id)} · produto {String(item.produtoId)}</small></th>{[item.prevista, item.recebidaBoa, item.recebidaAvariada, item.diferenca].map((value, index) => <td key={index}>{display(value)}</td>)}<td><button type="button" disabled={locked} onClick={() => { onSelect(note, "PedidoEntradaDto.Nota"); onSelect(item, "PedidoEntradaDto.ItemConferencia"); }}>Selecionar item {String(item.id)} da nota {String(note.numero)}</button></td></tr>)}</tbody></table></div></section>)}
            {section === "Conferência" && <><ReceivingSummary workflow={workflow} /><p>Conferido não significa efetivado. Falta, sobra ou avaria exige a tratativa e o aceite autorizado no comando Efetivar carga integral; o histórico da divergência permanece. Uma chegada incorreta é corrigida por estorno identificado e novo registro.</p>{root.motivoConclusao && <p>Tratativa registrada na conclusão: {String(root.motivoConclusao)}</p>}</>}
        </>}
        {section === "Histórico" && <p>Consulte as chegadas para ver instante real, responsável, observação, itens e estornos. Entradas conferidas mostram o resultado da efetivação. Um estorno conserva o lançamento original e exige Supervisor/Gestor.</p>}
    </section>;
}
