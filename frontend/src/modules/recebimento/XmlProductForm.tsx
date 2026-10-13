import { useMemo } from "react";
import { Operation } from "../../components/Operation";
import type { Transport } from "../../api/client";
import type { Perfil } from "../../contracts/runtime";
import type {
    NfeImportacaoDto_Item,
    ProdutoDto_Resposta,
} from "../../contracts/types";

export function XmlProductForm({
    item,
    clienteId,
    transport,
    perfil,
    onCreated,
    onLock,
}: {
    item: NfeImportacaoDto_Item;
    clienteId: string;
    transport: Transport;
    perfil: Perfil;
    onCreated: (product: ProdutoDto_Resposta) => void;
    onLock: (locked: boolean) => void;
}) {
    const context = useMemo(() => {
        const mass = ["TO", "T", "TON", "KG"].includes(
            item.unidadeComercial?.toUpperCase() ?? "",
        );
        return {
            clienteId,
            sku: item.codigo,
            descricao: item.descricao,
            unidadeMedida: mass ? "KG" : item.unidadeComercial,
            tipoQuantidade: mass ? "MEDIDA" : "CONTAGEM",
            precisaoQuantidade: mass ? 3 : 0,
            controlaLote: false,
            controlaValidade: false,
        };
    }, [clienteId, item]);
    return (
        <section
            className="nfe-product-form"
            aria-label="Cadastrar produto da nota"
        >
            <h3>Cadastrar produto da nota</h3>
            <p>
                Confira os dados sugeridos pelo XML e os controles de lote e
                validade. O produto será cadastrado para este cliente; depois,
                atualize a prévia da entrada.
            </p>
            <Operation
                id="ProdutoController.criar"
                transport={transport}
                perfil={perfil}
                context={context}
                fixedFields={["clienteId"]}
                inDialog
                hideHeading
                onSelect={() => {}}
                onSelectionLockChange={onLock}
                onReceipt={(receipt) =>
                    onCreated(receipt.data as ProdutoDto_Resposta)
                }
            />
        </section>
    );
}
