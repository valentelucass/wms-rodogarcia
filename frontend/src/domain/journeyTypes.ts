export interface Step {
    title: string;
    help: string;
    actions: string[];
}
export interface Journey {
    id: string;
    title: string;
    fe: string;
    be: string;
    steps: Step[];
    lookups?: [string, string][];
    references: [string, string][];
    referenceFields: Record<string, string>;
}
export interface NextAction {
    page: string;
    action: string;
    title: string;
}
export const s = (
    title: string,
    help: string,
    controller: string,
    actions: string[],
): Step => ({ title, help, actions: actions.map((a) => controller + "." + a) });
export const cadastro = (name: string, controller: string): Step =>
    s(
        name,
        "Consulte o cadastro antes de alterar. Revisão e motivo preservam o histórico; inativação definitiva exige verificar impedimentos.",
        controller,
        ["listar", "consultar", "criar", "alterar", "encerrar", "reativar"],
    );
