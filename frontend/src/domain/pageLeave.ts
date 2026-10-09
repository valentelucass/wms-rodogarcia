let guard: (() => boolean) | undefined;
export function registerPageLeave(check: () => boolean) {
    guard = check;
    return () => {
        if (guard === check) guard = undefined;
    };
}
export function canLeavePage() {
    return guard?.() ?? true;
}
