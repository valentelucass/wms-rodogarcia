import type { ExerciseSession } from "../hooks/useExerciseSession";
import type { Navigate } from "../hooks/useNavigation";
import { Operation } from "../components/Operation";
export function HomePage({
    session: s,
    navigate,
}: {
    session: ExerciseSession;
    navigate: Navigate;
}) {
    return (
        <>
            <h1>Início da operação</h1>
            <p>
                Selecione cliente e armazém. Os indicadores vêm da resposta
                fictícia; nenhum total global é calculado pela tela.
            </p>
            <div className="shortcuts">
                <button onClick={() => navigate("entrada")}>
                    Receber e conferir
                </button>
                <button onClick={() => navigate("coletor")}>
                    Ler e endereçar
                </button>
                <button onClick={() => navigate("saida")}>
                    Reservar e separar
                </button>
                <button onClick={() => navigate("fechamento")}>
                    Conferir fechamento
                </button>
            </div>
            <Operation
                id="IndicadorEstoqueController.listar"
                transport={s.transport}
                context={s.context}
                perfil={s.perfil}
                onSelect={() => navigate("estoque")}
            />
            <p>
                Indicadores globais de ocupação e contadores de pedidos aguardam
                contrato agregado. Consulte posições, estoque e pedidos nas
                telas próprias.
            </p>
        </>
    );
}
