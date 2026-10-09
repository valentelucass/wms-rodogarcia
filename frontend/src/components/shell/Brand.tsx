import { Icon } from "../../design-system/Icon";

export function Brand() {
    return (
        <div className="shell-brand">
            <span className="brand-symbol">
                <Icon name="estoque" />
            </span>
            <span className="brand-copy">
                <strong>Rodogarcia</strong>
                <span>WMS · Gestão de armazém</span>
            </span>
        </div>
    );
}
