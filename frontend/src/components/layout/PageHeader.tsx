import type { ReactNode } from "react";
import { Icon } from "../../design-system/Icon";

export function PageHeader({
    title,
    description,
    icon,
    actions,
    children,
    className = "",
}: {
    title: string;
    description: string;
    icon: string;
    actions?: ReactNode;
    children?: ReactNode;
    className?: string;
}) {
    return (
        <header className={`page-header ${className}`.trim()}>
            <div className="page-heading">
                <span className="page-icon">
                    <Icon name={icon} />
                </span>
                <div>
                    <h1>{title}</h1>
                    <p>{description}</p>
                </div>
            </div>
            {children}
            {actions && <div className="page-actions">{actions}</div>}
        </header>
    );
}
