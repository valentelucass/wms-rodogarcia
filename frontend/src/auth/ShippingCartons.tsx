import { useId } from "react";
import { DistantCartons } from "./DistantCartons";

type ParcelProps = {
    x: number;
    bottom: number;
    width: number;
    height: number;
    tier?: "middle" | "top";
    mark?: "label" | "arrows";
};

// All parcels share one projection and a common baseline.
function Parcel({ x, bottom, width: w, height: h, tier, mark }: ParcelProps) {
    const depth = Math.round(w * 0.16);
    const rise = Math.round(depth * 0.55);
    const tape = Math.round(w * 0.14);
    const seam = Math.round(w * 0.42);
    const tapeDrop = Math.max(7, Math.round(h * 0.24));
    return (
        <g transform={`translate(${x} ${bottom - h})`}>
            <g className={tier && `parcel-${tier}`}>
                <path
                    className="parcel-lid"
                    d={`M0 0l${depth} ${-rise}h${w}l${-depth} ${rise}Z`}
                />
                <path
                    className="parcel-side"
                    d={`M${w} 0l${depth} ${-rise}v${h}l${-depth} ${rise}Z`}
                />
                <rect className="parcel-face" width={w} height={h} rx="0.7" />
                <path
                    className="parcel-tape"
                    d={`M${seam} 0l${depth} ${-rise}h${tape}l${-depth} ${rise}v${tapeDrop}h${-tape}Z`}
                />
                {mark === "label" && (
                    <g transform={`translate(${w - 22} ${h - 14})`}>
                        <rect
                            className="parcel-label"
                            width="16"
                            height="9"
                            rx="1"
                        />
                        <path
                            className="parcel-print"
                            d="M3 2v5m2-5v5m3-5v5m2-5v5m3-5v5"
                        />
                    </g>
                )}
                {mark === "arrows" && (
                    <path
                        className="parcel-print"
                        transform={`translate(6 ${h - 13})`}
                        d="M2 8V1L0 3m2-2 2 2m4 5V1L6 3m2-2 2 2M0 10h10"
                    />
                )}
            </g>
        </g>
    );
}

// Cada posição descreve uma caixa apoiada no chão ou sobre a camada inferior.
type Placement = readonly [
    left: number,
    elevation: number,
    width: number,
    height: number,
];
type Ridge = { width: number; height: number; boxes: readonly Placement[] };

// No último plano as caixas são grandes e as torres permanecem montadas.
const horizonLeft: Ridge = {
    width: 414,
    height: 512,
    boxes: [
        [-25, 0, 168, 79],
        [-19, 79, 151, 72],
        [-10, 151, 135, 85],
        [-8, 236, 124, 58],
        [1, 294, 112, 80],
        [5, 374, 101, 55],
        [12, 429, 88, 68],
        [148, 0, 113, 82],
        [153, 82, 101, 67],
        [166, 149, 79, 96],
        [176, 245, 64, 54],
        [270, 0, 136, 70],
        [281, 70, 111, 52],
    ],
};
const horizonRight: Ridge = {
    width: 380,
    height: 470,
    boxes: [
        [-12, 0, 115, 63],
        [4, 63, 82, 83],
        [120, 0, 125, 69],
        [249, 0, 142, 69],
        [144, 69, 219, 55],
        [153, 124, 83, 78],
        [160, 202, 68, 59],
        [242, 124, 116, 92],
        [250, 216, 112, 71],
        [251, 287, 105, 65],
        [261, 352, 86, 98],
    ],
};

const farLeft: Ridge = {
    width: 500,
    height: 216,
    boxes: [
        // Duas bases e uma caixa comprida que atravessa a emenda.
        [-21, 0, 74, 42],
        [54, 0, 69, 42],
        [-13, 42, 128, 28],
        [-8, 70, 47, 41],
        [43, 70, 67, 32],
        [-4, 111, 38, 28],
        // Um volume baixo no vão antes da torre estreita.
        [139, 0, 63, 23],
        [222, 0, 82, 43],
        [226, 43, 74, 54],
        [232, 97, 58, 45],
        [237, 142, 47, 55],
        // Lotes baixos de outra largura fecham a fileira.
        [307, 0, 99, 34],
        [323, 34, 68, 31],
        [432, 0, 51, 57],
        [438, 57, 39, 39],
    ],
};
const farRight: Ridge = {
    width: 424,
    height: 193,
    boxes: [
        // Duas bases baixas e um único volume alto acima da direita.
        [5, 0, 48, 34],
        [54, 0, 58, 34],
        [16, 34, 87, 24],
        [63, 58, 36, 56],
        // Vão até uma pilha larga com topo plano e caixas desencontradas.
        [148, 0, 88, 51],
        [237, 0, 64, 51],
        [153, 51, 132, 36],
        [158, 87, 69, 35],
        [230, 87, 51, 35],
        [171, 122, 94, 37],
        // A última torre é separada da plataforma.
        [316, 0, 32, 23],
        [362, 0, 74, 45],
        [369, 45, 61, 64],
        [377, 109, 48, 62],
    ],
};
const midLeft: Ridge = {
    width: 362,
    height: 155,
    boxes: [
        [-18, 0, 102, 37],
        [-13, 37, 86, 25],
        [7, 62, 56, 42],
        [12, 104, 45, 34],
        [90, 0, 63, 48],
        [96, 48, 47, 31],
        // Um fardo comprido e raso ao lado de uma pilha compacta.
        [173, 0, 87, 26],
        [181, 26, 72, 27],
        [275, 0, 64, 39],
        [279, 39, 57, 44],
        [289, 83, 39, 21],
    ],
};
const midRight: Ridge = {
    width: 325,
    height: 144,
    boxes: [
        [8, 0, 79, 26],
        [19, 26, 61, 20],
        [111, 0, 67, 49],
        [119, 49, 52, 47],
        [123, 96, 42, 34],
        [186, 0, 78, 28],
        [200, 28, 54, 23],
        [272, 0, 65, 56],
        [287, 56, 48, 38],
    ],
};

function CartonRidge({
    layout,
    depth,
    side,
}: {
    layout: Ridge;
    depth: "horizon" | "far" | "mid";
    side: "left" | "right";
}) {
    return (
        <svg
            className={`shipping-ridge ridge-${depth} ridge-${side}`}
            viewBox={`0 0 ${layout.width} ${layout.height}`}
            focusable="false"
        >
            {layout.boxes.map(([left, elevation, width, height]) => (
                <Parcel
                    key={`${left}-${elevation}`}
                    x={left}
                    bottom={layout.height - elevation}
                    width={width}
                    height={height}
                    tier={
                        depth === "horizon"
                            ? undefined
                            : elevation >= 80
                              ? "top"
                              : elevation > 0
                                ? "middle"
                                : undefined
                    }
                />
            ))}
        </svg>
    );
}

export function ShippingCartons() {
    const floor = useId();
    return (
        <div className="shipping-cartons" aria-hidden="true">
            <DistantCartons />
            <CartonRidge layout={horizonLeft} depth="horizon" side="left" />
            <CartonRidge layout={horizonRight} depth="horizon" side="right" />
            <CartonRidge layout={farLeft} depth="far" side="left" />
            <CartonRidge layout={farRight} depth="far" side="right" />
            <CartonRidge layout={midLeft} depth="mid" side="left" />
            <CartonRidge layout={midRight} depth="mid" side="right" />
            <svg
                className="aisle-parcels"
                viewBox="0 0 330 66"
                focusable="false"
            >
                <Parcel x={4} bottom={63} width={58} height={29} mark="label" />
                <Parcel x={10} bottom={34} width={40} height={23} />
                <Parcel x={65} bottom={63} width={27} height={18} />
                <Parcel x={261} bottom={56} width={47} height={24} />
            </svg>
            <svg
                className="shipping-pile shipping-pile--left"
                viewBox="0 0 260 136"
                focusable="false"
            >
                <defs>
                    <linearGradient id={floor}>
                        <stop stopColor="currentColor" stopOpacity=".4" />
                        <stop
                            offset="1"
                            stopColor="currentColor"
                            stopOpacity="0"
                        />
                    </linearGradient>
                </defs>
                <path fill={`url(#${floor})`} d="M0 131h260l-24 4H0z" />
                <Parcel
                    x={-14}
                    bottom={131}
                    width={92}
                    height={47}
                    mark="arrows"
                />
                <Parcel
                    x={-10}
                    bottom={84}
                    width={83}
                    height={44}
                    tier="middle"
                    mark="label"
                />
                <Parcel x={-2} bottom={40} width={59} height={31} tier="top" />
                <Parcel
                    x={79}
                    bottom={131}
                    width={88}
                    height={39}
                    mark="label"
                />
                <Parcel
                    x={85}
                    bottom={92}
                    width={64}
                    height={34}
                    tier="middle"
                />
                <Parcel x={171} bottom={131} width={52} height={24} />
            </svg>
            <svg
                className="shipping-pile shipping-pile--right"
                viewBox="0 0 210 111"
                focusable="false"
            >
                <path
                    fill={`url(#${floor})`}
                    transform="translate(210) scale(-1 1)"
                    d="M0 106h210l-18 4H0z"
                />
                <Parcel
                    x={21}
                    bottom={106}
                    width={46}
                    height={26}
                    mark="label"
                />
                <Parcel x={70} bottom={106} width={60} height={33} />
                <Parcel
                    x={74}
                    bottom={73}
                    width={44}
                    height={26}
                    tier="middle"
                />
                <Parcel
                    x={134}
                    bottom={106}
                    width={82}
                    height={41}
                    mark="arrows"
                />
                <Parcel
                    x={139}
                    bottom={65}
                    width={71}
                    height={32}
                    tier="middle"
                    mark="label"
                />
                <Parcel x={151} bottom={33} width={51} height={24} tier="top" />
            </svg>
        </div>
    );
}
