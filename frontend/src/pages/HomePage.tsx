import type { ExerciseSession } from "../hooks/useExerciseSession";
import type { Navigate } from "../hooks/useNavigation";
import { Operation } from "../components/Operation";
import { PageHeader } from "../components/layout/PageHeader";
import { HomeOverview } from "../components/layout/HomeOverview";
export function HomePage({
    session: s,
    navigate,
}: {
    session: ExerciseSession;
    navigate: Navigate;
}) {
    return (
        <>
            <PageHeader
                title="Início da operação"
                icon="inicio"
                description="Selecione cliente e armazém. Os indicadores vêm da resposta fictícia; nenhum total global é calculado pela tela."
            />
            <HomeOverview navigate={navigate} />
            <section
                className="workspace-panel"
                aria-label="Indicadores do contexto"
            >
                <Operation
                    id="IndicadorEstoqueController.listar"
                    transport={s.transport}
                    context={s.context}
                    perfil={s.perfil}
                    onSelect={() => navigate("estoque")}
                />
            </section>
            <p>
                Indicadores globais de ocupação e contadores de pedidos aguardam
                contrato agregado. Consulte posições, estoque e pedidos nas
                telas próprias.
            </p>
        </>
    );
}
