export type DataMode = "real" | "ficticio";
export type DevEnvironment = Record<string, string | undefined>;

export function dataMode(mode: string, environment: DevEnvironment): DataMode {
    const selected = mode === "development" ? "real" : mode;
    if (selected !== "real" && selected !== "ficticio")
        throw new Error(
            "WMS_DEV_MODO_INVALIDO: use real ou ficticio explicitamente.",
        );
    if (environment.VITE_DATA_MODE && environment.VITE_DATA_MODE !== selected)
        throw new Error(
            "WMS_DEV_MODO_CONFLITANTE: comando e ambiente devem concordar.",
        );
    return selected;
}

export function frontendPort(value: string | undefined): number {
    if (!value || !/^[1-9]\d{0,4}$/.test(value) || Number(value) > 65535)
        throw new Error(
            "WMS_DEV_PORTA_INVALIDA: informe WMS_FE_PORT entre 1 e 65535.",
        );
    return Number(value);
}

export function backendTarget(value: unknown): string {
    if (
        typeof value !== "string" ||
        !/^http:\/\/127\.0\.0\.1:[1-9]\d{0,4}$/.test(value)
    )
        throw new Error(
            "WMS_DEV_DESTINO_INVALIDO: informe a origem loopback exata do backend confirmado.",
        );
    const url = new URL(value);
    if (!url.port || Number(url.port) > 65535 || url.origin !== value)
        throw new Error(
            "WMS_DEV_DESTINO_INVALIDO: porta ou origem nao canonica.",
        );
    return url.origin;
}

export interface PublicIdentity {
    issuer: unknown;
    jwkSetUri: unknown;
    audience: unknown;
}
// Parametros publicos, sem token, conta ou fluxo de login.
export function requirePublicIdentity(identity: PublicIdentity): void {
    if (
        [identity.issuer, identity.jwkSetUri, identity.audience].some(
            (value) => typeof value !== "string" || !value.trim(),
        )
    )
        throw new Error(
            "WMS_DEV_AUTH_CONFIGURATION_MISSING: configuracao publica do provedor real ausente; sem fallback ficticio.",
        );
    for (const value of [identity.issuer, identity.jwkSetUri]) {
        let uri: URL;
        try {
            uri = new URL(value as string);
        } catch {
            throw new Error(
                "WMS_DEV_AUTH_CONFIGURATION_INVALID: URI do provedor invalida.",
            );
        }
        if (
            uri.protocol !== "https:" ||
            uri.username ||
            uri.password ||
            uri.search ||
            uri.hash
        )
            throw new Error(
                "WMS_DEV_AUTH_CONFIGURATION_INVALID: issuer/JWK devem ser HTTPS sem credencial, query ou fragmento.",
            );
    }
    const audience = identity.audience as string;
    if (audience.length > 200 || /[\r\n]/.test(audience))
        throw new Error(
            "WMS_DEV_AUTH_CONFIGURATION_INVALID: audience invalida.",
        );
}
