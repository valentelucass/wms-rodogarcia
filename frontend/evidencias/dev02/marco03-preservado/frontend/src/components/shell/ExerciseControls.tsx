import type { ExerciseSession } from "../../hooks/useExerciseSession";
import type { Scenario } from "../../api/fictitious";
export function ExerciseControls({ session: s }: { session: ExerciseSession }) {
    return (
        <footer>
            <label>
                Resposta do exercício fictício
                <select
                    value={s.scenario}
                    onChange={(e) =>
                        s.changeScenario(e.target.value as Scenario)
                    }
                >
                    <option value="sucesso">Sucesso fictício</option>
                    <option value="vazio">Lista vazia</option>
                    <option value="conflito">
                        Conflito de revisão/disponibilidade
                    </option>
                    <option value="falha">Indisponibilidade</option>
                    <option value="perdida">
                        Resposta perdida após registro fictício
                    </option>
                    <option value="expirada">Sessão expirada</option>
                    <option value="negada">Acesso negado</option>
                    <option value="lento">Resposta lenta</option>
                </select>
            </label>
            <p>
                Dados somente em memória. Recarregar encerra o exercício.
                Nenhuma sincronização offline.
            </p>
        </footer>
    );
}
