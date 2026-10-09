import { ReferenceCatalogProvider } from "./components/context/ReferenceCatalog";
import { useExerciseSession } from "./hooks/useExerciseSession";
import { useNavigation } from "./hooks/useNavigation";
import { ExerciseContext } from "./components/shell/ExerciseContext";
import { ExerciseControls } from "./components/shell/ExerciseControls";
import { AppShell } from "./components/shell/AppShell";
import { CurrentPage } from "./pages/CurrentPage";
export function ExerciseApp() {
    const session = useExerciseSession(),
        nav = useNavigation();
    return (
        <ReferenceCatalogProvider
            transport={session.transport}
            version={session.revision}
        >
            <AppShell
                exercise
                page={nav.page}
                navigate={nav.navigate}
                contentKey={`${session.revision}-${nav.revision}`}
                context={<ExerciseContext session={session} />}
                footer={<ExerciseControls session={session} />}
            >
                <CurrentPage
                    session={session}
                    page={nav.page}
                    startAction={nav.startAction}
                    navigate={nav.navigate}
                />
            </AppShell>
        </ReferenceCatalogProvider>
    );
}
