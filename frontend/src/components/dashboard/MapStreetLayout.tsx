import { useLayoutEffect, useRef, type ReactNode } from "react";

// Fine tracks let differently sized streets fill gaps without equal-width cards.
// DOM order stays stable; each street remains an individually named region.
export function MapStreetLayout({ children }: { children: ReactNode }) {
    const ref = useRef<HTMLDivElement>(null);
    useLayoutEffect(() => {
        const container = ref.current;
        if (!container || typeof ResizeObserver === "undefined") return;
        const streets = Array.from(container.children) as HTMLElement[];
        const layout = () => {
            if (!container.clientWidth) return;
            const tracks = Math.max(1, Math.floor(container.clientWidth / 4));
            container.style.setProperty(
                "--map-street-max-width",
                `${Math.max(4, tracks * 4 - 12)}px`,
            );
            container.dataset.layout = "dense";
            const sizes = streets.map((street) =>
                street.getBoundingClientRect(),
            );
            streets.forEach((street, i) => {
                street.style.gridColumnEnd = `span ${Math.min(tracks, Math.ceil((sizes[i].width + 12) / 4))}`;
                street.style.gridRowEnd = `span ${Math.ceil((sizes[i].height + 12) / 4)}`;
            });
        };
        layout();
        const observer = new ResizeObserver(layout);
        observer.observe(container);
        streets.forEach((street) => observer.observe(street));
        return () => observer.disconnect();
    }, [children]);
    return (
        <div ref={ref} className="map-streets">
            {children}
        </div>
    );
}
