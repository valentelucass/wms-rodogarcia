import {
    journeys as j0,
    nextActions as n0,
} from "../modules/cadastros/definition";
import {
    journeys as j1,
    nextActions as n1,
} from "../modules/recebimento/definition";
import {
    journeys as j2,
    nextActions as n2,
} from "../modules/estoque/definition";
import { journeys as j3, nextActions as n3 } from "../modules/saida/definition";
import {
    journeys as j4,
    nextActions as n4,
} from "../modules/financeiro/definition";
import {
    journeys as j5,
    nextActions as n5,
} from "../modules/regularizacao/definition";
import {
    journeys as j6,
    nextActions as n6,
} from "../modules/relatorios/definition";
export const journeys = [...j0, ...j1, ...j2, ...j3, ...j4, ...j5, ...j6];
export const nextActions = { ...n0, ...n1, ...n2, ...n3, ...n4, ...n5, ...n6 };
export type { Journey, Step, NextAction } from "./journeyTypes";
export { actionLabel } from "./actionLabels";
