import type { ExerciseSession } from "../hooks/useExerciseSession";
import type { Navigate } from "../hooks/useNavigation";
import { WarehouseOverview } from "../components/dashboard/WarehouseOverview";
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
                title="Início"
                icon="inicio"
                description="Visão geral da operação · exercício fictício"
            />
            <HomeOverview navigate={navigate} />
            <WarehouseOverview
                key={s.revision}
                transport={s.transport}
                context={s.context}
                onScope={s.selectContext}
                onAddress={(p) => {
                    s.selectContext({
                        ...s.context,
                        armazemId: p.armazemId,
                        enderecoId: p.id,
                        enderecoCodigo: p.codigo,
                    });
                    navigate("coletor");
                }}
                ficticio
            />
        </>
    );
}
