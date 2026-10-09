import { useEffect, useState } from "react";
import { canLeavePage } from "../domain/pageLeave";
export function useNavigation() {
    const [page, setPage] = useState(() => location.hash.slice(1) || "inicio");
    const [startAction, setStartAction] = useState("");
    const [revision, setRevision] = useState(0);
    useEffect(() => {
        const back = () => {
            if (!canLeavePage()) {
                history.pushState(null, "", "#" + page);
                return;
            }
            setPage(location.hash.slice(1) || "inicio");
            setStartAction("");
            setRevision((x) => x + 1);
        };
        window.addEventListener("hashchange", back);
        return () => window.removeEventListener("hashchange", back);
    }, [page]);
    const navigate = (id: string, action = "") => {
        if (!canLeavePage()) return;
        setPage(id);
        setStartAction(action);
        setRevision((x) => x + 1);
        history.pushState(null, "", "#" + id);
    };
    return { page, startAction, revision, navigate };
}
export type Navigate = ReturnType<typeof useNavigation>["navigate"];
