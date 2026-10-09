import { isObject, type Values } from "../../contracts/runtime";
import {
    validateField,
    validateValue,
    InputError,
    initialValue,
} from "../../contracts/codec";
import { contingencyTypes } from "../../contracts/contingency";
export function validateContingencyCommand(body: Values) {
    if (!isObject(body.dados))
        throw new InputError("Preencha os dados do fato.");
    const cfg = contingencyTypes[String(body.tipo)];
    if (!cfg) throw new InputError("Selecione o tipo.");
    cfg.targets.forEach((f) =>
        validateField(f, (body.dados as Values)[f.name]),
    );
    const data = body.dados.dados;
    if (!isObject(data)) throw new InputError("Preencha os dados do fato.");
    validateValue(cfg.dto, { ...data, operacaoId: body.operacaoId });
}
export function changeContingencyKind(previous: Values, next: Values): Values {
    return next.tipo === previous.tipo
        ? next
        : {
              ...next,
              dados: {
                  dados: initialValue(
                      contingencyTypes[String(next.tipo)]?.dto ?? "unknown",
                  ),
              },
          };
}
