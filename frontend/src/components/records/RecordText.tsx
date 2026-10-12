import { useLayoutEffect, useState } from "react";
import { useInformationPopup } from "../information/InformationPopup";

export function RecordText({ text }: { text: string }) {
    const [truncated, setTruncated] = useState(false);
    const { anchorProps, popup, show } = useInformationPopup<HTMLSpanElement>(
        { title: text },
        !truncated,
    );
    const anchor = anchorProps.ref;
    useLayoutEffect(() => {
        const element = anchor.current;
        if (!element) return;
        const measure = () =>
            setTruncated(element.scrollWidth > element.clientWidth);
        measure();
        const observer =
            typeof ResizeObserver === "undefined"
                ? undefined
                : new ResizeObserver(measure);
        observer?.observe(element);
        window.addEventListener("resize", measure);
        return () => {
            observer?.disconnect();
            window.removeEventListener("resize", measure);
        };
    }, [anchor, text]);
    return (
        <>
            <span
                {...anchorProps}
                className="record-text"
                tabIndex={truncated ? 0 : undefined}
                onClick={show}
            >
                {text}
            </span>
            {popup}
        </>
    );
}
