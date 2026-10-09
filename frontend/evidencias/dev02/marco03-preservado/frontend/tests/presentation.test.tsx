import { it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { Fields } from "../src/components/Fields";
import { records, type Perfil } from "../src/contracts/runtime";

it.each(["OPERACAO", "SUPERVISOR", "GESTOR"] as Perfil[])(
    "campos condicionais respeitam apresentação de %s",
    (perfil) => {
        render(
            <>
                <Fields
                    fields={records["PedidoSaidaDto.Reservar"].filter(
                        (f) => f.name === "justificativaId",
                    )}
                    values={{}}
                    onChange={() => {}}
                    perfil={perfil}
                    schema="PedidoSaidaDto.Reservar"
                />
                <Fields
                    fields={records["ExpedicaoDto.Leitura"].filter(
                        (f) => f.name === "resolverPendentes",
                    )}
                    values={{ resolverPendentes: false }}
                    onChange={() => {}}
                    perfil={perfil}
                    schema="ExpedicaoDto.Leitura"
                />
            </>,
        );
        const justification = screen.getByLabelText(/Justificativa/);
        const historical = screen.getByLabelText(/Resolver pendências/);
        if (perfil === "OPERACAO") expect(justification).toBeDisabled();
        else expect(justification).toBeEnabled();
        if (perfil === "GESTOR") expect(historical).toBeEnabled();
        else expect(historical).toBeDisabled();
    },
);
