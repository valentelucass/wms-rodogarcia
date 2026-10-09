import { useExerciseSession } from "./hooks/useExerciseSession";
import { useNavigation } from "./hooks/useNavigation";
import { ExerciseContext } from "./components/shell/ExerciseContext";
import { ExerciseControls } from "./components/shell/ExerciseControls";
import { Navigation } from "./components/shell/Navigation";
import { CurrentPage } from "./pages/CurrentPage";
import { useLayoutEffect } from "react";
export function ExerciseApp() {
    const session = useExerciseSession(),
        nav = useNavigation();
    useLayoutEffect(() => {
        if (session.revision + nav.revision > 0)
            document.getElementById("conteudo")?.focus();
    }, [session.revision, nav.revision]);
    return (
        <>
            <a
                className="skip"
                href="#conteudo"
                onClick={(event) => {
                    event.preventDefault();
                    document.getElementById("conteudo")?.focus();
                }}
            >
                Ir para o conteúdo
            </a>
            <header>
                <strong>WMS Rodogarcia</strong>
                <span>EXERCÍCIO FICTÍCIO · sem backend · sem SQL</span>
            </header>
            <ExerciseContext session={session} />
            <div className="layout">
                <Navigation page={nav.page} navigate={nav.navigate} />
                <main
                    id="conteudo"
                    tabIndex={-1}
                    key={`${session.revision}-${nav.revision}`}
                >
                    <CurrentPage
                        session={session}
                        page={nav.page}
                        startAction={nav.startAction}
                        navigate={nav.navigate}
                    />
                </main>
            </div>
            <ExerciseControls session={session} />
        </>
    );
}
