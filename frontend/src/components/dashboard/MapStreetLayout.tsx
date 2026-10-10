import { useLayoutEffect, useRef, type ReactNode } from "react";

// Fine tracks let differently sized streets fill gaps without equal-width cards.
// DOM order stays stable; each street remains an individually named region.
export function MapStreetLayout({ children }: { children: ReactNode }) {
    const ref = useRef<HTMLDivElement>(null);
    useLayoutEffect(() => {
        const container = ref.current;
        if (!container || typeof ResizeObserver === "undefined") return;
        const streets = Array.from(container.children) as HTMLElement[];
        let frame = 0;
        let active = true;
        const layout = () => {
            if (!container.clientWidth) return;
            const context = document.createElement("canvas").getContext("2d");
            const tracks = Math.max(1, Math.floor(container.clientWidth / 4));
            container.style.setProperty(
                "--map-street-max-width",
                `${Math.max(4, tracks * 4 - 12)}px`,
            );
            container.dataset.layout = "dense";
            // Start from the compact packing; growth never pushes a neighbour out of its row.
            const needs = streets.map((street) => {
                street.style.setProperty("--map-extra-width", "0px");
                const base = parseFloat(
                    getComputedStyle(street).getPropertyValue(
                        "--map-base-slot-width",
                    ),
                );
                const columns = parseInt(
                    street.style.getPropertyValue("--map-column-count"),
                    10,
                );
                let preferred = base;
                for (const label of street.querySelectorAll<HTMLElement>(
                    ".map-position strong",
                )) {
                    const font = getComputedStyle(label);
                    const button = getComputedStyle(label.parentElement!);
                    if (context) {
                        context.font = `${font.fontWeight} ${font.fontSize} ${font.fontFamily}`;
                        preferred = Math.max(
                            preferred,
                            Math.ceil(
                                context.measureText(label.textContent ?? "")
                                    .width +
                                    parseFloat(button.paddingLeft) +
                                    parseFloat(button.paddingRight) +
                                    parseFloat(button.borderLeftWidth) +
                                    parseFloat(button.borderRightWidth) +
                                    2,
                            ),
                        );
                    }
                }
                // Leave breathing room after the code, without turning sparse streets into bars.
                const maximum = Math.min(240, Math.max(192, preferred + 32));
                street.style.setProperty(
                    "--map-max-slot-width",
                    `${maximum}px`,
                );
                return {
                    columns,
                    wanted: Math.ceil(((preferred - base) * columns) / 4),
                    limit: Math.floor(((maximum - base) * columns) / 4),
                    extra: 0,
                };
            });
            const place = () => {
                const sizes = streets.map((street) =>
                    street.getBoundingClientRect(),
                );
                streets.forEach((street, i) => {
                    street.style.gridColumnEnd = `span ${Math.min(tracks, Math.ceil((sizes[i].width + 12) / 4))}`;
                    street.style.gridRowEnd = `span ${Math.ceil((sizes[i].height + 12) / 4)}`;
                });
            };
            place();
            const boxes = streets.map((street) =>
                street.getBoundingClientRect(),
            );
            const rows = new Map<number, number[]>();
            boxes.forEach((box, index) => {
                const key = Math.round(box.top);
                rows.set(key, [...(rows.get(key) ?? []), index]);
            });
            for (const indices of rows.values()) {
                const top = boxes[indices[0]].top;
                const bottom = Math.max(...indices.map((i) => boxes[i].bottom));
                // A taller card may share its vertical band with another packed row.
                // Preserve those occupied gaps instead of expanding into them.
                if (
                    boxes.some(
                        (box, i) =>
                            !indices.includes(i) &&
                            box.top < bottom &&
                            box.bottom > top,
                    )
                )
                    continue;
                let spare =
                    tracks -
                    indices.reduce(
                        (sum, i) => sum + Math.ceil((boxes[i].width + 12) / 4),
                        0,
                    );
                while (spare > 0) {
                    const expandable = indices.filter(
                        (i) => needs[i].extra < needs[i].limit,
                    );
                    if (!expandable.length) break;
                    const clipped = expandable.filter(
                        (i) => needs[i].extra < needs[i].wanted,
                    );
                    // Share remaining space only while cells stay within their useful size.
                    const candidates = clipped.length ? clipped : expandable;
                    const next = candidates.reduce((a, b) =>
                        needs[a].extra / needs[a].columns <=
                        needs[b].extra / needs[b].columns
                            ? a
                            : b,
                    );
                    needs[next].extra++;
                    spare--;
                }
            }
            streets.forEach((street, i) =>
                street.style.setProperty(
                    "--map-extra-width",
                    `${needs[i].extra * 4}px`,
                ),
            );
            place();
        };
        const schedule = () => {
            cancelAnimationFrame(frame);
            if (active) frame = requestAnimationFrame(layout);
        };
        layout();
        const observer = new ResizeObserver(schedule);
        observer.observe(container);
        streets.forEach((street) => observer.observe(street));
        void document.fonts?.ready.then(schedule);
        return () => {
            active = false;
            cancelAnimationFrame(frame);
            observer.disconnect();
        };
    }, [children]);
    return (
        <div ref={ref} className="map-streets">
            {children}
        </div>
    );
}
