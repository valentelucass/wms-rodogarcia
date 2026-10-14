import { useId } from "react";
import { records } from "../../contracts/runtime";
import type { OperationState } from "../../hooks/useOperation";
import { ScalarField } from "../../components/fields/ScalarField";
import { AddressTemplateLink } from "./AddressTemplateLink";

export function AddressImportFields({ state: s }: { state: OperationState }) {
    const fileId = useId();
    const disabled = s.pending || s.uncertain || s.completed;
    return (
        <div className="address-import">
            <div className="address-import-guide">
                <div>
                    <h3>Prepare as posições do armazém</h3>
                    <p>
                        Baixe o modelo, preencha uma posição por linha e envie o
                        arquivo para conferir a prévia.
                    </p>
                </div>
                <AddressTemplateLink />
            </div>
            <fieldset
                className="operation-fields address-import-fields"
                disabled={disabled}
            >
                <legend className="sr-only">Dados da importação</legend>
                <ScalarField
                    f={{ ...s.e.params[0], name: "armazemId" }}
                    path="armazemId"
                    value={s.params.id}
                    onChange={(id) => s.setParams({ ...s.params, id })}
                    disabled={disabled}
                />
                <div className="field address-import-file">
                    <label htmlFor={fileId}>
                        Planilha de endereços .xlsx *
                    </label>
                    <input
                        id={fileId}
                        type="file"
                        accept=".xlsx"
                        aria-describedby={`${fileId}-help`}
                        onChange={(event) => s.setFile(event.target.files?.[0])}
                    />
                    <span className="hint" id={`${fileId}-help`}>
                        {s.file ? `Arquivo selecionado: ${s.file.name}. ` : ""}
                        Use a aba Enderecos e mantenha os nomes das colunas.
                    </span>
                </div>
                <div className="address-import-reason">
                    <ScalarField
                        f={records["ImportacaoEnderecoDto.Previa"].find(
                            (field) => field.name === "motivo",
                        )!}
                        path="motivo"
                        value={s.body.motivo}
                        onChange={(motivo) =>
                            s.updateBody({ ...s.body, motivo })
                        }
                        disabled={disabled}
                    />
                </div>
            </fieldset>
            <div className="address-import-instructions">
                <h4>Como preencher</h4>
                <ul>
                    <li>
                        As sete primeiras colunas são obrigatórias. As demais
                        são opcionais.
                    </li>
                    <li>
                        Código, rua e posição são textos: preserve os zeros à
                        esquerda.
                    </li>
                    <li>
                        Tipo: ARMAZENAGEM, TRIAGEM, QUARENTENA ou SEPARACAO.
                    </li>
                    <li>
                        Em tipoUnidadePermitido, use PALLET, BOBINA, VOLUME,
                        PALLET_BOBINA, PALLET_VOLUME, BOBINA_VOLUME ou TODAS.
                        Preencha também peso, dimensões e empilhamento.
                    </li>
                </ul>
                <p>
                    A prévia aponta os erros antes da confirmação. Os endereços
                    só são criados ao confirmar a importação.
                </p>
            </div>
        </div>
    );
}
