import { ExerciseApp } from "./ExerciseApp";
import { IntegrationBlockedPage } from "./pages/IntegrationBlockedPage";

export default function App() {
    const selected = import.meta.env.VITE_DATA_MODE;
    const exercise =
        selected === "ficticio" ||
        (!selected && import.meta.env.MODE === "ficticio");
    return exercise ? <ExerciseApp /> : <IntegrationBlockedPage />;
}
