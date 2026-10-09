import { useCallback, useEffect, useRef } from "react";

// Shared by operation, reference lookup and collector. Invalidating a scope
// rejects stale UI updates; interrupting only the wait preserves uncertainty.
export function useRequestScope() {
    const alive = useRef(true),
        generation = useRef(0),
        controller = useRef<AbortController | null>(null);
    const cancel = useCallback(() => {
        generation.current++;
        controller.current?.abort();
    }, []);
    const interrupt = useCallback(() => controller.current?.abort(), []);
    const begin = useCallback(() => {
        cancel();
        const g = generation.current;
        const c = new AbortController();
        controller.current = c;
        return {
            signal: c.signal,
            isCurrent: () => alive.current && g === generation.current,
        };
    }, [cancel]);
    useEffect(() => {
        alive.current = true;
        return () => {
            alive.current = false;
            cancel();
        };
    }, [cancel]);
    return { begin, cancel, interrupt };
}
