import { Icon } from "../../design-system/Icon";

export function AddressTemplateLink() {
    return (
        <a
            className="address-template-link"
            href={`${import.meta.env.BASE_URL}modelos/enderecos.xlsx`}
            download="modelo-enderecos.xlsx"
        >
            <Icon name="download" />
            <span>Baixar planilha modelo</span>
        </a>
    );
}
