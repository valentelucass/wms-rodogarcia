export function IntegrationBlockedPage() {
    return (
        <main id="conteudo" tabIndex={-1}>
            <h1>Integração real preparada e bloqueada</h1>
            <p>
                A guarda atual do ambiente de desenvolvimento está bloqueada.
                Nenhuma consulta ou comando será enviado à API.
            </p>
            <p>
                A integração depende da comprovação segura do ambiente e da
                configuração do provedor de identidade. Não há entrada com senha
                ou emissão de token nesta aplicação.
            </p>
            <p>
                O exercício fictício local permanece disponível no modo de
                demonstração.
            </p>
        </main>
    );
}
