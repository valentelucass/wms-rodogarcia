import { useLayoutEffect, useRef, useState } from "react";
import { useRequestScope } from "./useRequestScope";

/** File.text não aceita abort: a geração decide qual intenção pode publicar. */
export function useXmlFileText(
    onChange: (text: string) => void,
    disabled: boolean,
) {
    const scope = useRequestScope();
    const latest = useRef(onChange);
    const [pending, setPending] = useState(false);
    const [error, setError] = useState("");
    useLayoutEffect(() => {
        latest.current = onChange;
    }, [onChange]);
    const { cancel } = scope;
    useLayoutEffect(() => {
        if (disabled) {
            cancel();
            setPending(false);
        }
    }, [disabled, cancel]);
    const edit = (text: string) => {
        scope.cancel();
        setPending(false);
        setError("");
        latest.current(text);
    };
    const load = async (file: File | undefined) => {
        if (disabled) return;
        const ticket = scope.begin();
        setError("");
        setPending(!!file);
        // Uma nova seleção não pode confirmar silenciosamente o XML anterior.
        latest.current("");
        if (!file) return;
        try {
            const text = await file.text();
            if (ticket.isCurrent()) latest.current(text);
        } catch {
            if (ticket.isCurrent())
                setError(
                    "Não foi possível ler o arquivo XML. Selecione novamente ou informe o texto.",
                );
        } finally {
            if (ticket.isCurrent()) setPending(false);
        }
    };
    return { load, edit, pending, error };
}
