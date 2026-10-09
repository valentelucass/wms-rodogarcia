import { execFileSync } from "node:child_process";
import { join, resolve } from "node:path";
import type { BackendIdentity } from "./ready-chain";

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
        `$observed=Get-Process -Id ${identity.pid}`,
        `$listeners=@(Get-NetTCPConnection -State Listen -LocalPort ${identity.port} -ErrorAction Stop)`,
        "[pscustomobject]@{pid=$observed.Id;startUtc=$observed.StartTime.ToUniversalTime().ToString('o');path=$observed.Path;listeners=@($listeners|ForEach-Object{[pscustomobject]@{pid=$_.OwningProcess;address=$_.LocalAddress}})}|ConvertTo-Json -Compress -Depth 4",
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
        ) as {
            pid: number;
            startUtc: string;
            path: string;
            listeners: { pid: number; address: string }[];
        };
        return (
            observed.pid === identity.pid &&
            Date.parse(observed.startUtc) === Date.parse(identity.startUtc) &&
            resolve(observed.path).toLowerCase() ===
                resolve(identity.executable).toLowerCase() &&
            Array.isArray(observed.listeners) &&
            observed.listeners.length > 0 &&
            observed.listeners.every(
                (listener) =>
                    listener.pid === identity.pid &&
                    listener.address === "127.0.0.1",
            )
        );
    } catch {
        return false;
    }
}
