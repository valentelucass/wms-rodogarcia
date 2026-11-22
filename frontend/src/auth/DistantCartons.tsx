// All rows recede toward the same point on the floor's horizon.
function project(x: number, y: number, depth: number) {
    const scale = 1 / (1 + depth * 0.52);
    return `${(735 + (x - 735) * scale).toFixed(1)},${(225 + (y - 225) * scale).toFixed(1)}`;
}

type Stack = {
    inner: number;
    width: number;
    depth: number;
    length: number;
    heights: number[];
};

// Ordered back to front. Each footprint leaves room to walk past its neighbour.
const stacks: Stack[] = [
    {
        inner: 234,
        width: -198,
        depth: 7.1,
        length: 0.85,
        heights: [120, 126, 105, 140, 117, 134],
    },
    {
        inner: 1150,
        width: 220,
        depth: 6.2,
        length: 1.0,
        heights: [135, 96, 156, 118, 132],
    },
    {
        inner: 1106,
        width: 235,
        depth: 3.85,
        length: 1.2,
        heights: [138, 112, 149, 125, 87, 126],
    },
    {
        inner: 1015,
        width: 147,
        depth: 3.72,
        length: 0.72,
        heights: [115, 141, 96, 129, 104, 139],
    },
    {
        inner: 194,
        width: -250,
        depth: 3.35,
        length: 1.2,
        heights: [142, 116, 135, 102, 120],
    },
    {
        inner: 827,
        width: 179,
        depth: 2.63,
        length: 0.9,
        heights: [128, 105, 128, 121, 112, 118, 98],
    },
    {
        inner: 1192,
        width: 252,
        depth: 1.85,
        length: 1.02,
        heights: [125, 118, 98, 102],
    },
    {
        inner: 247,
        width: -215,
        depth: 1.36,
        length: 0.95,
        heights: [126, 114, 164, 94, 116, 96],
    },
    {
        inner: 51,
        width: -157,
        depth: 0.64,
        length: 0.7,
        heights: [113, 85, 136, 102, 99],
    },
    {
        inner: 1240,
        width: 203,
        depth: 0.3,
        length: 0.86,
        heights: [126, 139, 116, 136, 71],
    },
];

function FloorStack({ inner, width, depth, length, heights }: Stack) {
    let base = 619;
    return (
        <g className="distant-stack">
            {heights.map((height, level) => {
                const bottom = base;
                const top = (base -= height);
                const x =
                    inner + Math.sign(width) * (level * 3 + (level % 2) * 9);
                const end = inner + width - Math.sign(width) * (level % 3) * 8;
                const back = depth + length;
                const front = depth + (level % 2) * 0.035;
                const a = project(x, top, front);
                const b = project(end, top, front);
                const c = project(x, top, back);
                const foot = project(x, bottom, front);
                const tapeX = x + (end - x) * 0.46;
                const tapeEnd = tapeX + width * 0.105;
                return (
                    <g key={level}>
                        {top > 225 && (
                            <polygon
                                className="parcel-lid"
                                points={`${a} ${b} ${project(end, top, back)} ${c}`}
                            />
                        )}
                        <polygon
                            className="parcel-side"
                            points={`${a} ${c} ${project(x, bottom, back)} ${foot}`}
                        />
                        <polygon
                            className="parcel-face"
                            points={`${a} ${b} ${project(end, bottom, front)} ${foot}`}
                        />
                        <polygon
                            className="parcel-tape"
                            points={`${project(tapeX, top, front)} ${project(tapeEnd, top, front)} ${project(tapeEnd, top + 26, front)} ${project(tapeX, top + 26, front)}`}
                        />
                    </g>
                );
            })}
        </g>
    );
}

export function DistantCartons() {
    return (
        <svg
            className="distant-cartons"
            viewBox="0 0 1440 620"
            preserveAspectRatio="xMidYMid slice"
            focusable="false"
        >
            {stacks.map((stack) => (
                <FloorStack key={stack.depth} {...stack} />
            ))}
        </svg>
    );
}
