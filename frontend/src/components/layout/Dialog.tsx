import { useId, useLayoutEffect, useRef, type ReactNode } from "react";
import { Icon } from "../../design-system/Icon";

// Native modal: browser focus trap and inert background, with focus restored
// to the initiating control even when the dialog unmounts after a decision.
export function Dialog({
    title,
    children,
    onClose,
    locked = false,
    wide = false,
}: {
    title: string;
    children: ReactNode;
    onClose: () => void;
    locked?: boolean;
    wide?: boolean;
}) {
    const ref = useRef<HTMLDialogElement>(null);
    const opener = useRef(document.activeElement);
    const titleId = useId();
    useLayoutEffect(() => {
        const dialog = ref.current!;
        const trigger = opener.current;
        if (typeof dialog.showModal === "function") dialog.showModal();
        else dialog.setAttribute("open", "");
        return () => {
            if (typeof dialog.close === "function") dialog.close();
            queueMicrotask(() => {
                if (trigger instanceof HTMLElement && trigger.isConnected)
                    trigger.focus();
            });
        };
    }, []);
    return (
        <dialog
            ref={ref}
            className={
                "workspace-dialog" + (wide ? " workspace-dialog--wide" : "")
            }
            aria-labelledby={titleId}
            onCancel={(event) => {
                event.preventDefault();
                if (!locked) onClose();
            }}
            onKeyDown={(event) => {
                if (event.key !== "Tab") return;
                const controls = Array.from(
                    event.currentTarget.querySelectorAll<HTMLElement>(
                        'button:not(:disabled), a[href], input:not(:disabled), select:not(:disabled), textarea:not(:disabled), summary, [tabindex]:not([tabindex="-1"])',
                    ),
                ).filter((item) => item.getClientRects().length > 0);
                const first = controls[0],
                    last = controls.at(-1);
                if (!first) {
                    event.preventDefault();
                    event.currentTarget.focus();
                    return;
                }
                if (
                    (!event.shiftKey && document.activeElement === last) ||
                    (event.shiftKey &&
                        (document.activeElement === first ||
                            document.activeElement === event.currentTarget))
                ) {
                    event.preventDefault();
                    (event.shiftKey ? last : first)?.focus();
                }
            }}
        >
            <div className="dialog-header">
                <h2 id={titleId}>{title}</h2>
                <button
                    type="button"
                    className="dialog-close"
                    aria-label="Fechar diálogo"
                    disabled={locked}
                    onClick={onClose}
                >
                    <Icon name="close" />
                </button>
            </div>
            <div className="dialog-body">{children}</div>
        </dialog>
    );
}
