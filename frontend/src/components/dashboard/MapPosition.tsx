import {
    useEffect,
    useId,
    useLayoutEffect,
    useRef,
    useState,
    type CSSProperties,
} from "react";
import { createPortal } from "react-dom";
import type { VisaoOperacaoDto_Posicao } from "../../contracts/types";
import { Icon } from "../../design-system/Icon";
import { formatQuantity } from "./OperationDashboard";

export function MapPosition({
    position: p,
    state,
    hidden,
    disabled,
    style,
    onSelect,
}: {
    position: VisaoOperacaoDto_Posicao;
    state: string;
    hidden: boolean;
    disabled: boolean;
    style: CSSProperties;
    onSelect: (p: VisaoOperacaoDto_Posicao) => void;
}) {
    const [open, setOpen] = useState(false);
    const anchor = useRef<HTMLButtonElement>(null);
    const preview = useRef<HTMLDivElement>(null);
    const timer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
    const id = useId();
    const cancelClose = () => clearTimeout(timer.current);
    const closeSoon = () => {
        cancelClose();
        timer.current = setTimeout(() => setOpen(false), 120);
    };
    const show = () => {
        cancelClose();
        setOpen(true);
    };
    const visible = open && !hidden && !disabled;
    useEffect(() => () => clearTimeout(timer.current), []);
    useLayoutEffect(() => {
        if (!visible || !anchor.current || !preview.current) return;
        const cell = anchor.current.getBoundingClientRect();
        const popup = preview.current;
        const bounds = popup.getBoundingClientRect();
        const left = Math.max(
            8,
            Math.min(cell.left, window.innerWidth - bounds.width - 8),
        );
        const top =
            cell.bottom + 8 + bounds.height <= window.innerHeight - 8
                ? cell.bottom + 8
                : Math.max(8, cell.top - bounds.height - 8);
        popup.style.left = `${left}px`;
        popup.style.top = `${top}px`;
        popup.style.visibility = "visible";
        const close = () => setOpen(false);
        const onScroll = (event: Event) => {
            if (
                !(event.target instanceof Node) ||
                !popup.contains(event.target)
            )
                close();
        };
        const onKey = (event: KeyboardEvent) => {
            if (event.key === "Escape") close();
        };
        window.addEventListener("resize", close);
        window.addEventListener("scroll", onScroll, true);
        window.addEventListener("keydown", onKey);
        return () => {
            window.removeEventListener("resize", close);
            window.removeEventListener("scroll", onScroll, true);
            window.removeEventListener("keydown", onKey);
        };
    }, [visible, p]);
    return (
        <>
            <button
                ref={anchor}
                type="button"
                hidden={hidden}
                disabled={disabled}
                style={style}
                className={`map-position map-state--${p.ocupada ? "occupied" : p.disponivel ? "free" : "other"}`}
                aria-label={`${p.codigo} · ${state}${p.bloqueada ? " · Bloqueado" : ""}${p.reservada ? " · Reservado" : ""}`}
                aria-describedby={visible ? id : undefined}
                onPointerEnter={(event) => {
                    if (event.pointerType !== "touch") show();
                }}
                onPointerLeave={closeSoon}
                onFocus={show}
                onBlur={closeSoon}
                onClick={() => {
                    cancelClose();
                    setOpen(false);
                    onSelect(p);
                }}
            >
                <strong>{p.codigo}</strong>
                {(p.bloqueada || p.reservada) && (
                    <span className="map-position-flags" aria-hidden="true">
                        {p.bloqueada && <Icon name="senha" />}
                        {p.reservada && <Icon name="bookmark" />}
                    </span>
                )}
            </button>
            {visible &&
                createPortal(
                    <div
                        ref={preview}
                        id={id}
                        role="tooltip"
                        className="map-position-preview"
                        onPointerEnter={cancelClose}
                        onPointerLeave={closeSoon}
                    >
                        <strong className="map-preview-code">{p.codigo}</strong>
                        <p className="map-preview-state">{state}</p>
                        <dl>
                            <div className="map-preview-wide">
                                <dt>Armazém</dt>
                                <dd>{p.armazem}</dd>
                            </div>
                            <div className="map-preview-wide">
                                <dt>Rua</dt>
                                <dd>{p.rua}</dd>
                            </div>
                            <div>
                                <dt>Nível</dt>
                                <dd>{p.nivel}</dd>
                            </div>
                            <div>
                                <dt>Posição</dt>
                                <dd>{p.posicao}</dd>
                            </div>
                            <div>
                                <dt>Área</dt>
                                <dd>
                                    {p.tipo.toLowerCase().replaceAll("_", " ")}
                                </dd>
                            </div>
                            <div>
                                <dt>Capacidade de peso</dt>
                                <dd>
                                    {p.capacidadePesoKg == null
                                        ? "Não configurada"
                                        : `${formatQuantity(p.capacidadePesoKg)} kg`}
                                </dd>
                            </div>
                        </dl>
                        {(p.bloqueada || p.reservada || p.quarentena) && (
                            <p className="map-preview-flags">
                                {p.bloqueada && (
                                    <span>
                                        <Icon name="senha" />
                                        Bloqueada
                                    </span>
                                )}
                                {p.reservada && (
                                    <span>
                                        <Icon name="bookmark" />
                                        Reservada
                                    </span>
                                )}
                                {p.quarentena && <span>Em quarentena</span>}
                            </p>
                        )}
                        <small>
                            Clique ou pressione Enter para abrir os detalhes.
                        </small>
                    </div>,
                    document.body,
                )}
        </>
    );
}
