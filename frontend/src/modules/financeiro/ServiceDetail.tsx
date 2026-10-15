import type { Values } from "../../contracts/runtime";
import { StatusBadge } from "../../design-system/StatusBadge";
import { display } from "../../domain/labels";

const serviceTypes: Record<string, string> = {
    ARMAZENAGEM: "Armazenagem",
    ENTRADA: "Entrada",
    SAIDA: "Saída",
    ADICIONAL: "Adicional",
};
const billingUnits: Record<string, string> = {
    UNIDADE_LOGISTICA: "Unidade logística",
    QUANTIDADE_PRODUTO: "Quantidade de produto",
    POSICAO_DIA: "Posição por dia",
    VEICULO: "Veículo",
    CONTEINER: "Contêiner",
    PERCENTUAL: "Percentual",
};

export function ServiceDetail({ data }: { data: Values }) {
    return (
        <div className="service-detail">
            <div className="service-detail-heading">
                <h3>{display(data.descricao)}</h3>
                <div className="service-detail-status">
                    <span className="hint">Situação informada</span>
                    <StatusBadge value={data.situacao} />
                </div>
            </div>
            <dl className="service-detail-fields">
                <div>
                    <dt>Código</dt>
                    <dd>{display(data.codigo)}</dd>
                </div>
                <div>
                    <dt>Tipo de serviço</dt>
                    <dd>
                        {serviceTypes[String(data.tipo)] ?? display(data.tipo)}
                    </dd>
                </div>
                <div>
                    <dt>Unidade de cobrança</dt>
                    <dd>
                        {billingUnits[String(data.unidade)] ??
                            display(data.unidade)}
                    </dd>
                </div>
            </dl>
        </div>
    );
}
