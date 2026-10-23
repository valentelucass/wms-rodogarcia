import { useState, type ComponentProps } from "react";
import type { JourneyPage } from "../../components/JourneyPage";
import { RecordWorkspace } from "../../components/records/RecordWorkspace";
import { journeys } from "./definition";

type Props = Omit<
    ComponentProps<typeof JourneyPage>,
    "journey" | "startAction"
>;
export function InvoicePreparationTools(props: Props) {
    const [services, setServices] = useState(false);
    const [damages, setDamages] = useState(false);
    const journey = journeys.find((item) => item.id === "cobranca")!;
    return (
        <div className="invoice-preparation-tools">
            <p>
                Confira os serviços realizados e o período. O cálculo será
                apresentado antes de preparar a fatura; o fechamento depende da
                aprovação do Gestor.
            </p>
            <details
                className="invoice-section"
                onToggle={(event) => setServices(event.currentTarget.open)}
            >
                <summary>Conferir ou incluir serviços realizados</summary>
                {services && (
                    <RecordWorkspace
                        {...props}
                        journey={journey}
                        step={0}
                        startAction=""
                    />
                )}
            </details>
            {props.perfil === "GESTOR" && (
                <details
                    className="invoice-section"
                    onToggle={(event) => setDamages(event.currentTarget.open)}
                >
                    <summary>Conferir avarias na cobrança</summary>
                    {damages && (
                        <RecordWorkspace
                            {...props}
                            journey={journey}
                            step={1}
                            startAction=""
                        />
                    )}
                </details>
            )}
        </div>
    );
}
