import { useId, useRef, useState, useLayoutEffect } from "react";
import { barShare, formatQuantity } from "./chartFormatting";

type Point = {
    id: string;
    label: string;
    shortLabel: string;
    value: string;
    note?: string;
};

function plotLabel(value: string) {
    const count = BigInt(value);
    for (const [factor, suffix] of [
        [1000000000000000000n, "qui"],
        [1000000000000000n, "qua"],
        [1000000000000n, "tri"],
        [1000000000n, "bi"],
        [1000000n, "mi"],
        [1000n, "mil"],
    ] as const) {
        if (count >= factor) {
            const decimal = ((count % factor) * 10n) / factor;
            return `${count / factor}${decimal ? "," + decimal : ""} ${suffix}`;
        }
    }
    return formatQuantity(value);
}

export function OperationLineChart({
    points,
    total,
    unit,
    variant,
}: {
    points: Point[];
    total: string;
    unit: "posições" | "pedidos";
    variant: "physical" | "queue";
}) {
    const id = useId();
    const container = useRef<HTMLDivElement>(null);
    const [width, setWidth] = useState(600);
    const [active, setActive] = useState<string>();
    useLayoutEffect(() => {
        const element = container.current;
        if (!element) return;
        const style = getComputedStyle(element);
        setWidth(
            Math.max(
                180,
                element.clientWidth -
                    parseFloat(style.paddingLeft || "0") -
                    parseFloat(style.paddingRight || "0"),
            ),
        );
        if (typeof ResizeObserver === "undefined") return;
        const observer = new ResizeObserver(([entry]) =>
            setWidth(Math.max(180, entry.contentRect.width)),
        );
        observer.observe(element);
        return () => observer.disconnect();
    }, []);
    const maximum = points.reduce(
        (current, p) => (BigInt(p.value) > current ? BigInt(p.value) : current),
        0n,
    );
    const magnitude = 10n ** BigInt(Math.max(0, String(maximum).length - 1));
    const rounded = ((maximum + magnitude - 1n) / magnitude) * magnitude;
    const ceiling = rounded > 1n ? rounded + (rounded % 2n) : 2n;
    const ceilingText = String(ceiling);
    const top = 30,
        bottom = 204,
        left = width < 420 ? 32 : 48,
        right = width - 24;
    const coordinates = points.map((p, index) => ({
        ...p,
        x:
            points.length === 1
                ? (left + right) / 2
                : left + ((right - left) * index) / (points.length - 1),
        y: bottom - (barShare(p.value, ceilingText) / 1000) * (bottom - top),
    }));
    const line = coordinates
        .map((p, i) => `${i ? "L" : "M"}${p.x},${p.y}`)
        .join(" ");
    const area = coordinates.length
        ? `${line} L${coordinates.at(-1)!.x},${bottom} L${coordinates[0].x},${bottom} Z`
        : "";
    const selected = points.find((p) => p.id === active) ?? points[0];
    return (
        <div
            ref={container}
            className={`operation-line-chart operation-line-chart--${variant}`}
        >
            <div className="operation-line-summary">
                <div>
                    <strong>{formatQuantity(total)}</strong>
                    <span>
                        {unit === "posições"
                            ? "posições ocupadas no contexto"
                            : "pedidos em aberto"}
                    </span>
                </div>
                <span className="operation-line-caption">
                    {variant === "physical" ? "Por área" : "Por etapa"}
                </span>
            </div>
            {points.length > 0 ? (
                <>
                    <svg
                        className="operation-line-plot"
                        viewBox={`0 0 ${width} 242`}
                        aria-label={
                            variant === "physical"
                                ? "Posições ocupadas por área"
                                : "Pedidos abertos por etapa"
                        }
                        role="group"
                    >
                        <defs>
                            <linearGradient
                                id={`${id}-fill`}
                                x1="0"
                                y1="0"
                                x2="0"
                                y2="1"
                            >
                                <stop
                                    className="operation-line-fill-start"
                                    offset="0%"
                                />
                                <stop
                                    className="operation-line-fill-end"
                                    offset="100%"
                                />
                            </linearGradient>
                        </defs>
                        {[0, 1, 2, 3, 4].map((tick) => {
                            const y = bottom - ((bottom - top) * tick) / 4;
                            return (
                                <g key={tick} aria-hidden="true">
                                    <line
                                        className="operation-line-grid"
                                        x1={left}
                                        x2={right}
                                        y1={y}
                                        y2={y}
                                    />
                                    {tick % 2 === 0 && (
                                        <text
                                            className="operation-line-axis"
                                            x={left - 8}
                                            y={y + 4}
                                            textAnchor="end"
                                        >
                                            {plotLabel(
                                                String(
                                                    (ceiling * BigInt(tick)) /
                                                        4n,
                                                ),
                                            )}
                                        </text>
                                    )}
                                </g>
                            );
                        })}
                        <path
                            d={area}
                            fill={`url(#${id}-fill)`}
                            aria-hidden="true"
                        />
                        <path
                            className="operation-line-stroke"
                            d={line}
                            aria-hidden="true"
                        />
                        {coordinates.map((p, index) => (
                            <g
                                key={p.id}
                                className={`operation-line-point${selected?.id === p.id ? " is-active" : ""}`}
                                tabIndex={0}
                                role="img"
                                aria-label={`${p.label}: ${formatQuantity(p.value)} ${unit}${p.note ? ". " + p.note : ""}`}
                                onFocus={() => setActive(p.id)}
                                onPointerEnter={() => setActive(p.id)}
                                onKeyDown={(event) => {
                                    if (event.key === "Escape")
                                        setActive(undefined);
                                }}
                            >
                                <title>{`${p.label}: ${formatQuantity(p.value)} ${unit}`}</title>
                                <circle
                                    className="operation-line-halo"
                                    cx={p.x}
                                    cy={p.y}
                                    r="16"
                                />
                                <circle
                                    className="operation-line-dot"
                                    cx={p.x}
                                    cy={p.y}
                                    r="5"
                                />
                                <text
                                    className="operation-line-value"
                                    x={p.x}
                                    y={p.y - 17}
                                    textAnchor={
                                        index === 0
                                            ? "start"
                                            : index === coordinates.length - 1
                                              ? "end"
                                              : "middle"
                                    }
                                >
                                    {plotLabel(p.value)}
                                </text>
                                <text
                                    className="operation-line-axis operation-line-category"
                                    x={p.x}
                                    y="232"
                                    textAnchor="middle"
                                >
                                    {p.shortLabel}
                                </text>
                            </g>
                        ))}
                    </svg>
                    <div
                        className="operation-line-breakdown"
                        aria-label={
                            variant === "physical"
                                ? "Valores por área"
                                : "Valores por etapa"
                        }
                    >
                        {points.map((p) => (
                            <button
                                type="button"
                                key={p.id}
                                aria-pressed={selected?.id === p.id}
                                onClick={() => setActive(p.id)}
                                onFocus={() => setActive(p.id)}
                            >
                                <span>{p.label}</span>
                                <strong>{formatQuantity(p.value)}</strong>
                            </button>
                        ))}
                    </div>
                    <div className="operation-line-detail">
                        <strong>{selected?.label}</strong>
                        <span>
                            {selected?.note ??
                                `${formatQuantity(selected?.value ?? "0")} ${unit} nesta etapa.`}
                        </span>
                    </div>
                </>
            ) : (
                <p className="dashboard-empty">
                    Nenhuma informação por{" "}
                    {variant === "physical" ? "área" : "etapa"} nesta consulta.
                </p>
            )}
            <p className="operation-line-footnote">
                {variant === "physical"
                    ? "Comparação entre áreas na consulta atual."
                    : "Pedidos em cada etapa na consulta atual."}
            </p>
        </div>
    );
}
