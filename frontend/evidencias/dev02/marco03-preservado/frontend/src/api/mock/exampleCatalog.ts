import { exampleFixtures as receiving } from "../../modules/recebimento/exampleFixtures";
import { exampleFixtures as dispatch } from "../../modules/saida/exampleFixtures";
import { exampleFixtures as billing } from "../../modules/financeiro/exampleFixtures";
import { exampleFixtures as registry } from "../../modules/cadastros/exampleFixtures";
import { exampleFixtures as stock } from "../../modules/estoque/exampleFixtures";
import { exampleFixtures as adjustments } from "../../modules/regularizacao/exampleFixtures";
const examples = {
    ...receiving,
    ...dispatch,
    ...billing,
    ...registry,
    ...stock,
    ...adjustments,
};
export function exampleValues(type: string) {
    return structuredClone(examples[type] ?? {});
}
