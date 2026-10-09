import { useXmlFileText } from "../../hooks/useXmlFileText";
export function XmlField({
    id,
    name,
    value,
    onChange,
    disabled,
    required,
    maxLength,
}: {
    id: string;
    name: string;
    value: unknown;
    onChange: (value: unknown) => void;
    disabled: boolean;
    required: boolean;
    maxLength?: number;
}) {
    const reading = useXmlFileText(onChange, disabled);
    return (
        <div className="field">
            <label htmlFor={id}>{name}</label>
            <textarea
                id={id}
                value={String(value ?? "")}
                onChange={(e) => reading.edit(e.target.value)}
                disabled={disabled}
                required={required}
                maxLength={maxLength}
                rows={5}
            />
            <input
                aria-label={"Carregar arquivo XML para " + name}
                type="file"
                accept=".xml,text/xml,application/xml"
                disabled={disabled}
                onChange={(e) => void reading.load(e.target.files?.[0])}
            />
            {reading.pending && (
                <p role="status">
                    Lendo arquivo XML. Aguarde ou edite o texto para substituir
                    a leitura.
                </p>
            )}
            {reading.error && <p role="alert">{reading.error}</p>}
        </div>
    );
}
