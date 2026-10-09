import { ExerciseApp } from "./ExerciseApp";
import { AuthApp } from "./auth/AuthApp";
import { IntegrationBlockedPage } from "./pages/IntegrationBlockedPage";

export default function App() {
    const selected = import.meta.env.VITE_DATA_MODE;
    const exercise =
        selected === "ficticio" ||
        (!selected && import.meta.env.MODE === "ficticio");
    if (selected && selected !== "real" && selected !== "ficticio")
        return <IntegrationBlockedPage />;
    return exercise ? <ExerciseApp /> : <AuthApp />;
}
