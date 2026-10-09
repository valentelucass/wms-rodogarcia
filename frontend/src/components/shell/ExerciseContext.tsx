import type { Perfil } from "../../contracts/runtime";
import type { ExerciseSession } from "../../hooks/useExerciseSession";
import { ReferenceSelect } from "../context/ReferenceSelect";
export function ExerciseContext({ session: s }: { session: ExerciseSession }) {
    return (
        <>
            <section
                className="context"
                aria-label="Sessão fictícia e contexto"
            >
                <label>
                    Perfil de apresentação fictício
                    <select
                        value={s.perfil}
                        onChange={(e) =>
                            s.changePerfil(e.target.value as Perfil)
                        }
                    >
                        <option value="GESTOR">Gestor</option>
                        <option value="SUPERVISOR">Supervisor</option>
                        <option value="OPERACAO">Operação</option>
                    </select>
                </label>
                <ReferenceSelect
                    kind="clientes"
                    title="Cliente fictício"
                    all
                    value={s.client}
                    onChange={s.setClient}
                />
                <ReferenceSelect
                    kind="armazens"
                    title="Armazém fictício"
                    all
                    value={s.warehouse}
                    onChange={s.setWarehouse}
                />
                <button type="button" onClick={s.applyContext}>
                    Aplicar contexto
                </button>
                <button type="button" onClick={s.exit}>
                    Sair do exercício
                </button>
                {!s.active && (
                    <button
                        type="button"
                        onClick={() => s.changePerfil(s.perfil)}
                    >
                        Entrar no exercício fictício
                    </button>
                )}
            </section>
            {s.message && (
                <p role="status" className="notice">
                    {s.message}
                </p>
            )}
        </>
    );
}
