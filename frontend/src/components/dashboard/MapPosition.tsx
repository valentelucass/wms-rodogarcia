import type { CSSProperties } from "react";
import type { VisaoOperacaoDto_Posicao } from "../../contracts/types";
import { Icon } from "../../design-system/Icon";
import { positionInformation } from "../../content/information";
import { useInformationPopup } from "../information/InformationPopup";
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
    const { anchorProps, popup, close } =
        useInformationPopup<HTMLButtonElement>(
            positionInformation(
                p,
                state,
                p.capacidadePesoKg == null
                    ? ""
                    : formatQuantity(p.capacidadePesoKg),
            ),
            hidden || disabled,
        );
    return (
        <>
            <button
                {...anchorProps}
                type="button"
                hidden={hidden}
                disabled={disabled}
                style={style}
                className={`map-position map-state--${p.ocupada ? "occupied" : p.disponivel ? "free" : "other"}`}
                aria-label={`${p.codigo} · ${state}${p.bloqueada ? " · Bloqueado" : ""}${p.reservada ? " · Reservado" : ""}`}
                onClick={() => {
                    close();
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
            {popup}
        </>
    );
}
