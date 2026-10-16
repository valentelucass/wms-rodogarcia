import { useEffect, useId, useRef, useState } from "react";
import { recordActionLabel } from "../../domain/recordPages";
import { Icon } from "../../design-system/Icon";
import { useInformationPopup } from "../../components/information/InformationPopup";

export function ReceivingRowActions({
    id,
    available,
    primary,
    pending,
    onOpen,
}: {
    id: string;
    available: string[];
    primary?: string;
    pending: boolean;
    onOpen: (action?: string) => void;
}) {
    const menuId = useId();
    const viewInfo = useInformationPopup<HTMLButtonElement>(
        { title: "Ver pedido" },
        pending,
    );
    const moreInfo = useInformationPopup<HTMLButtonElement>(
        { title: "Mais ações" },
        pending,
    );
    const trigger = moreInfo.anchorProps.ref;
    const menu = useRef<HTMLDivElement>(null);
    const [open, setOpen] = useState(false);
    useEffect(() => {
        if (!open) return;
        const close = () => menu.current?.hidePopover();
        if (pending) {
            close();
            return;
        }
        const scroll = (event: Event) => {
            if (
                event.target instanceof Node &&
                menu.current?.contains(event.target)
            )
                return;
            close();
        };
        window.addEventListener("scroll", scroll, true);
        window.addEventListener("resize", close);
        return () => {
            window.removeEventListener("scroll", scroll, true);
            window.removeEventListener("resize", close);
        };
    }, [open, pending]);
    const placeMenu = () => {
        const element = menu.current;
        const button = trigger.current;
        if (!element || !button || !element.matches(":popover-open")) return;
        const bounds = button.getBoundingClientRect();
        const rect = element.getBoundingClientRect();
        element.style.left = `${Math.max(8, Math.min(bounds.right - rect.width, window.innerWidth - rect.width - 8))}px`;
        element.style.top = `${bounds.bottom + rect.height + 8 <= window.innerHeight - 8 ? bounds.bottom + 8 : Math.max(8, bounds.top - rect.height - 8)}px`;
    };
    return (
        <div className="record-row-actions receiving-row-actions">
            {primary && (
                <button
                    className="receiving-conference-button"
                    type="button"
                    disabled={pending}
                    onClick={() => onOpen(primary)}
                >
                    <Icon name="contagem" />
                    {primary.endsWith(".iniciar")
                        ? "Iniciar conferência"
                        : "Continuar conferência"}
                </button>
            )}
            <button
                {...viewInfo.anchorProps}
                className="record-action-icon"
                type="button"
                disabled={pending}
                aria-label="Ver pedido"
                onClick={() => {
                    viewInfo.close();
                    onOpen();
                }}
            >
                <Icon name="eye" />
            </button>
            {available.length > 0 && (
                <>
                    <button
                        {...moreInfo.anchorProps}
                        className="record-action-icon receiving-more-button"
                        type="button"
                        disabled={pending}
                        popoverTarget={menuId}
                        aria-label={`Mais ações do pedido ${id}`}
                        onClick={moreInfo.close}
                    >
                        <span
                            className="receiving-more-icon"
                            aria-hidden="true"
                        >
                            ⋮
                        </span>
                    </button>
                    <div
                        ref={menu}
                        id={menuId}
                        popover="auto"
                        className="receiving-row-menu"
                        role="group"
                        aria-label={`Ações do pedido ${id}`}
                        onToggle={() => {
                            setOpen(
                                menu.current?.matches(":popover-open") ?? false,
                            );
                            placeMenu();
                        }}
                    >
                        {available.map((action) => (
                            <button
                                type="button"
                                key={action}
                                disabled={pending}
                                onClick={() => {
                                    menu.current?.hidePopover();
                                    onOpen(action);
                                }}
                            >
                                {recordActionLabel(action)}
                            </button>
                        ))}
                    </div>
                </>
            )}
            {viewInfo.popup}
            {moreInfo.popup}
        </div>
    );
}
