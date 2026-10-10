const paths: Record<string, string> = {
    inicio: "m3 10 9-7 9 7 M5 9v12h14V9 M9 21v-8h6v8",
    cadastros: "M4 5h16v16H4z M8 3v4 M16 3v4 M4 11h16 M8 15h2 M14 15h2",
    entrada: "M12 3v12 m-4-4 4 4 4-4 M4 15v6h16v-6",
    unidades: "m3 7 9-4 9 4v10l-9 4-9-4z m0-10 9 4 9-4 M12 11v10 M7 5l9 4",
    estoque: "M3 21V7l9-4 9 4v14 M3 21h18 M7 21V11h10v10 M7 16h10 M12 11v10",
    saida: "M12 17V3 m-4 4 4-4 4 4 M4 15v6h16v-6",
    fiscal: "M6 3h9l4 4v14H6z M14 3v5h5 M9 12h7 M9 16h7",
    servicos: "M4 7h16v14H4z M8 7V3h8v4 M4 12h16 M10 12v3h4v-3",
    precos: "M3 3h9l9 9-9 9-9-9z M7 7h.01 M12 10l3 3 M10 12l3 3",
    cobranca: "M4 7h16v14H4z M8 7V3h8v4 M4 12h16 M10 12v3h4v-3",
    fechamento:
        "M5 3h14v18H5z M8 7h8 M8 11h2 M14 11h2 M8 15h2 M14 15h2 M8 18h2 M14 18h2",
    contagem: "M9 5h-4v16h14V5h-4 M9 3h6v4H9z M8 12l2 2 5-5 M8 18h8",
    contingencia: "m12 3 10 18H2z M12 9v5 M12 17v1",
    relatorios: "M4 3v18h17 M8 17v-5 M13 17V7 M18 17V4",
    coletor: "M8 2h8v20H8z M10 5h4 M10 9h4 M10 12h4 M11 19h2",
    usuarios:
        "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8 M17 4a4 4 0 0 1 0 8 M22 21v-2a4 4 0 0 0-3-4",
    acesso: "m12 3 8 3v6c0 5-8 9-8 9s-8-4-8-9V6z M8 12l3 3 5-6",
    senha: "M5 10h14v11H5z M8 10V6a4 4 0 0 1 8 0v4 M12 14v3",
    menu: "M4 6h16 M4 12h16 M4 18h16",
    close: "m6 6 12 12 M6 18 18 6",
    collapse: "M3 4h18v16H3z M9 4v16 m7-12-4 4 4 4",
    expand: "M3 4h18v16H3z M9 4v16 m4-12 4 4-4 4",
    "chevron-left": "m15 5-7 7 7 7",
    "chevron-right": "m9 5 7 7-7 7",
    "chevron-down": "m5 9 7 7 7-7",
    armazens: "M3 21V7l9-4 9 4v14 M3 21h18 M7 21V11h10v10 M7 16h10 M12 11v10",
    moon: "M20.9 13a9 9 0 1 1-9.9-9.9A7 7 0 0 0 20.9 13z",
    sun: "M12 3v2 M12 19v2 M3 12h2 M19 12h2 m-13-7-2-2 m12 12 2 2 M7 17l-2 2 M17 7l2-2 M16 12a4 4 0 1 0-8 0 4 4 0 0 0 8 0",
    theme: "M12 3v2 M12 19v2 M3 12h2 M19 12h2 m-13-7-2-2 m12 12 2 2 M7 17l-2 2 M17 7l2-2 M16 12a4 4 0 1 0-8 0 4 4 0 0 0 8 0",
    logout: "M9 3H4v18h5 M9 12h12 m-4-4 4 4-4 4",
    mail: "M3 5h18v14H3z m0 0 9 7 9-7",
    help: "M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20 M9 8a3 3 0 0 1 6 0c0 2-3 2-3 5 M12 17h.01",
    eye: "M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12 M15 12a3 3 0 1 0-6 0 3 3 0 0 0 6 0",
    edit: "m16 3 5 5-12 12-6 1 1-6z M14 5l5 5",
    refresh: "M20 7v5h-5 M4 17v-5h5 M6 6a8 8 0 0 1 13 3 M18 18a8 8 0 0 1-13-3",
    "eye-off":
        "m3 3 18 18 M10 5c5-1 10 4 12 7l-3 4 M6 6c-2 2-3 4-4 6 3 5 7 8 12 7l3-1 M10 10a3 3 0 0 0 4 4",
    arrow: "M4 12h16 m-6-6 6 6-6 6",
    support:
        "M3 14v-3a9 9 0 0 1 18 0v3 M3 13h4v7H5a2 2 0 0 1-2-2z M21 13h-4v7h2a2 2 0 0 0 2-2z",
};
export function Icon({ name }: { name: string }) {
    return (
        <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.75"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
            focusable="false"
        >
            <path d={paths[name] ?? paths.unidades} />
        </svg>
    );
}
