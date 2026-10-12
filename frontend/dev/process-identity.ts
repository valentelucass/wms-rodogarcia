import { execFileSync } from "node:child_process";
import { join, resolve } from "node:path";
import type { BackendIdentity } from "./ready-chain";

export interface ObservedBackend {
    pid: number;
    startUtc: string;
    path: string;
    listeners: { pid: number; address: string; port: number }[];
}

export function backendProcessMatches(
    identity: BackendIdentity,
    observed: ObservedBackend,
): boolean {
    return (
        observed.pid === identity.pid &&
        Number.isFinite(Date.parse(observed.startUtc)) &&
        Date.parse(observed.startUtc) === Date.parse(identity.startUtc) &&
        resolve(observed.path).toLowerCase() ===
            resolve(identity.executable).toLowerCase() &&
        Array.isArray(observed.listeners) &&
        observed.listeners.length === 1 &&
        observed.listeners.every(
            (listener) =>
                listener.pid === identity.pid &&
                listener.address === "127.0.0.1" &&
                listener.port === identity.port,
        )
    );
}

/** Metadados do PID/listener informado; sem HTTP ou intervencao em processo. */
export function inspectBackend(identity: BackendIdentity): boolean {
    if (
        process.platform !== "win32" ||
        !Number.isSafeInteger(identity.pid) ||
        identity.pid < 1 ||
        !Number.isSafeInteger(identity.port) ||
        identity.port < 1 ||
        identity.port > 65535
    )
        return false;
    const script = [
        "$ErrorActionPreference='Stop'",
        `$observed=Get-CimInstance -ClassName Win32_Process -Filter 'ProcessId=${identity.pid}' -Property ProcessId,CreationDate,ExecutablePath -ErrorAction Stop`,
        `$listeners=@(Get-NetTCPConnection -State Listen -ErrorAction Stop|Where-Object{$_.OwningProcess -eq ${identity.pid}})`,
        "[pscustomobject]@{pid=$observed.ProcessId;startUtc=$observed.CreationDate.ToUniversalTime().ToString('o');path=$observed.ExecutablePath;listeners=@($listeners|ForEach-Object{[pscustomobject]@{pid=$_.OwningProcess;address=$_.LocalAddress;port=$_.LocalPort}})}|ConvertTo-Json -Compress -Depth 4",
    ].join("; ");
    try {
        const observed = JSON.parse(
            execFileSync(
                join(
                    process.env.SystemRoot ?? "C:/Windows",
                    "System32/WindowsPowerShell/v1.0/powershell.exe",
                ),
                ["-NoProfile", "-NonInteractive", "-Command", script],
                {
                    encoding: "utf8",
                    windowsHide: true,
                    timeout: 5000,
                    stdio: ["ignore", "pipe", "ignore"],
                },
            ),
        ) as ObservedBackend;
        return backendProcessMatches(identity, observed);
    } catch {
        return false;
    }
}
