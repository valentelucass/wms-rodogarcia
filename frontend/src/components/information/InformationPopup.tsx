import {
    useEffect,
    useId,
    useLayoutEffect,
    useRef,
    useState,
    type ReactNode,
} from "react";
import { createPortal } from "react-dom";
import {
    information,
    informationLabels,
    type Information,
    type InformationKey,
} from "../../content/information";

const openedEvent = "wms-information-open";

export function useInformationPopup<T extends HTMLElement>(
    content: Information,
    disabled = false,
) {
    const anchor = useRef<T>(null);
    const popup = useRef<HTMLDivElement>(null);
    const id = useId();
    const pendingBounds = useRef<DOMRect | null>(null);
    const [open, setOpen] = useState(false);
    const opening = useRef<ReturnType<typeof setTimeout> | undefined>(
        undefined,
    );
    const closing = useRef<ReturnType<typeof setTimeout> | undefined>(
        undefined,
    );
    const visible = open && !disabled;
    useEffect(() => {
        if (disabled) {
            clearTimeout(opening.current);
            clearTimeout(closing.current);
            setOpen(false);
        }
    }, [disabled]);
    const cancel = () => {
        clearTimeout(opening.current);
        clearTimeout(closing.current);
    };
    const close = () => {
        cancel();
        setOpen(false);
    };
    const show = () => {
        cancel();
        if (disabled) return;
        window.dispatchEvent(new CustomEvent(openedEvent, { detail: id }));
        setOpen(true);
    };
    const leave = () => {
        cancel();
        closing.current = setTimeout(() => setOpen(false), 180);
    };
    useEffect(() => {
        const cancelPending = () => clearTimeout(opening.current);
        const scrollPending = () => {
            const before = pendingBounds.current;
            const current = anchor.current?.getBoundingClientRect();
            if (
                !before ||
                !current ||
                Math.abs(current.top - before.top) > 1 ||
                Math.abs(current.left - before.left) > 1
            )
                cancelPending();
        };
        const otherOpened = (event: Event) => {
            if ((event as CustomEvent<string>).detail !== id) {
                clearTimeout(opening.current);
                clearTimeout(closing.current);
                setOpen(false);
            }
        };
        window.addEventListener(openedEvent, otherOpened);
        window.addEventListener("scroll", scrollPending, true);
        window.addEventListener("resize", cancelPending);
        return () => {
            clearTimeout(opening.current);
            clearTimeout(closing.current);
            window.removeEventListener(openedEvent, otherOpened);
            window.removeEventListener("scroll", scrollPending, true);
            window.removeEventListener("resize", cancelPending);
        };
    }, [id]);
    useLayoutEffect(() => {
        if (!visible || !anchor.current || !popup.current) return;
        const bounds = anchor.current.getBoundingClientRect();
        const tip = popup.current;
        const rect = tip.getBoundingClientRect();
        const left = Math.max(
            8,
            Math.min(
                bounds.left + (bounds.width - rect.width) / 2,
                window.innerWidth - rect.width - 8,
            ),
        );
        const below =
            bounds.bottom + 10 + rect.height <= window.innerHeight - 8;
        tip.style.left = `${left}px`;
        tip.style.top = `${below ? bounds.bottom + 10 : Math.max(8, bounds.top - rect.height - 10)}px`;
        tip.dataset.side = below ? "below" : "above";
        tip.style.visibility = "visible";
        const dismiss = () => {
            clearTimeout(opening.current);
            clearTimeout(closing.current);
            setOpen(false);
        };
        const escape = (event: KeyboardEvent) => {
            if (event.key === "Escape") dismiss();
        };
        const scroll = (event: Event) => {
            if (event.target instanceof Node && tip.contains(event.target))
                return;
            const current = anchor.current?.getBoundingClientRect();
            // Focus/click can finish an automatic scroll before this listener runs.
            // Dismiss only when the anchor actually moves after placement.
            if (
                !current ||
                Math.abs(current.top - bounds.top) > 1 ||
                Math.abs(current.left - bounds.left) > 1
            )
                dismiss();
        };
        const outside = (event: PointerEvent) => {
            if (
                event.target instanceof Node &&
                !tip.contains(event.target) &&
                !anchor.current?.contains(event.target)
            )
                dismiss();
        };
        window.addEventListener("keydown", escape);
        window.addEventListener("resize", dismiss);
        window.addEventListener("scroll", scroll, true);
        document.addEventListener("pointerdown", outside);
        return () => {
            window.removeEventListener("keydown", escape);
            window.removeEventListener("resize", dismiss);
            window.removeEventListener("scroll", scroll, true);
            document.removeEventListener("pointerdown", outside);
        };
    }, [visible, content]);
    return {
        close,
        show,
        anchorProps: {
            ref: anchor,
            "aria-describedby": visible ? id : undefined,
            onPointerEnter: (event: React.PointerEvent<T>) => {
                if (event.pointerType !== "touch") {
                    cancel();
                    pendingBounds.current =
                        anchor.current?.getBoundingClientRect() ?? null;
                    opening.current = setTimeout(show, 260);
                }
            },
            onPointerLeave: leave,
            onFocus: show,
            onBlur: leave,
        },
        popup: visible
            ? createPortal(
                  <div
                      ref={popup}
                      id={id}
                      role="tooltip"
                      className="information-popup"
                      onPointerEnter={cancel}
                      onPointerLeave={leave}
                  >
                      <strong className="information-popup-title">
                          {content.title}
                      </strong>
                      {content.description && <p>{content.description}</p>}
                      {content.facts && (
                          <dl>
                              {content.facts.map((fact, index) => (
                                  <div key={`${fact.label}-${index}`}>
                                      <dt>{fact.label}</dt>
                                      <dd>{fact.value}</dd>
                                  </div>
                              ))}
                          </dl>
                      )}
                      {content.footer && <small>{content.footer}</small>}
                  </div>,
                  document.body,
              )
            : null,
    };
}

export function InformationCard({
    content,
    children,
    className,
    onClick,
    as = "article",
}: {
    content: Information;
    children: ReactNode;
    className?: string;
    onClick?: () => void;
    as?: "article" | "button";
}) {
    const { anchorProps, popup, close, show } =
        useInformationPopup<HTMLElement>(content);
    const Tag = as;
    return (
        <>
            <Tag
                {...anchorProps}
                ref={(element) => {
                    anchorProps.ref.current = element;
                }}
                className={className}
                tabIndex={0}
                type={as === "button" ? "button" : undefined}
                onClick={() => {
                    if (onClick) {
                        close();
                        onClick();
                    } else show();
                }}
            >
                {children}
            </Tag>
            {popup}
        </>
    );
}

export function InformationHint({ topic }: { topic: InformationKey }) {
    const content = information[topic];
    const { anchorProps, popup, show } =
        useInformationPopup<HTMLButtonElement>(content);
    return (
        <>
            <button
                {...anchorProps}
                type="button"
                className="information-hint"
                aria-label={informationLabels.help(content.title)}
                onClick={show}
            >
                <span aria-hidden="true">i</span>
            </button>
            {popup}
        </>
    );
}
