import { Icon } from "../../design-system/Icon";

export function Brand({ onHome }: { onHome?: () => void }) {
    return (
        <div className="shell-brand">
            {onHome ? (
                <button
                    type="button"
                    className="brand-symbol brand-home"
                    aria-label="Ir para a página inicial"
                    title="Ir para a página inicial"
                    onClick={onHome}
                >
                    <Icon name="estoque" />
                </button>
            ) : (
                <span className="brand-symbol">
                    <Icon name="estoque" />
                </span>
            )}
            <span className="brand-copy">
                <strong>Rodogarcia</strong>
                <span>WMS · Gestão de armazém</span>
            </span>
        </div>
    );
}
