import { createContext } from "react";
import type { Values } from "../contracts/runtime";
export interface ReferenceOption {
    value: string;
    title: string;
}
export const FormReferences = createContext<{
    defaults: Values;
    records?: Record<string, Values[]>;
    options: Record<string, ReferenceOption[]>;
}>({ defaults: {}, options: {} });
