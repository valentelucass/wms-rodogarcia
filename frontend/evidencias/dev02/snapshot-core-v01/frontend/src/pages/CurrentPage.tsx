import { journeys } from "../domain/journeys";
import { operationContext } from "../domain/workflow";
import type { ExerciseSession } from "../hooks/useExerciseSession";
import type { Navigate } from "../hooks/useNavigation";
import { Collector } from "../components/Collector";
import { JourneyPage } from "../components/JourneyPage";
import { HomePage } from "./HomePage";
import { AccessPage } from "./AccessPage";
export function CurrentPage({
    session: s,
    page,
    startAction,
    navigate,
}: {
    session: ExerciseSession;
    page: string;
    startAction: string;
    navigate: Navigate;
}) {
    if (!s.active)
        return (
            <>
                <h1>Exercício encerrado</h1>
                <p>
                    Dados de tela descartados. Entre pelo seletor de perfil
                    fictício.
                </p>
            </>
        );
    if (page === "inicio") return <HomePage session={s} navigate={navigate} />;
    if (page === "acesso") return <AccessPage />;
    const ctx = (id: string) => operationContext(id, s.context, s.workflow);
    if (page === "coletor")
        return (
            <Collector
                transport={s.transport}
                context={ctx("EstoqueController.posicionar")}
                perfil={s.perfil}
                onReceipt={s.onReceipt}
                taskContexts={{
                    ler: ctx("ExpedicaoController.ler"),
                    separar: ctx("ExpedicaoController.separar"),
                    contar: ctx("ContagemController.contar"),
                }}
            />
        );
    const journey = journeys.find((j) => j.id === page);
    return journey ? (
        <JourneyPage
            journey={journey}
            transport={s.transport}
            context={s.context}
            perfil={s.perfil}
            workflow={s.workflow}
            onRecord={s.onRecord}
            onReceipt={s.onReceipt}
            startAction={startAction}
            onNavigate={(n) => navigate(n.page, n.action)}
        />
    ) : (
        <>
            <h1>Módulo não encontrado</h1>
            <button onClick={() => navigate("inicio")}>Voltar ao início</button>
        </>
    );
}
