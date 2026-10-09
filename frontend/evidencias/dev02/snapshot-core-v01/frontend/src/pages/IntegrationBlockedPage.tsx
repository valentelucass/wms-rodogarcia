export function IntegrationBlockedPage() {
    return (
        <>
            <a
                className="skip"
                href="#conteudo"
                onClick={(event) => {
                    event.preventDefault();
                    document.getElementById("conteudo")?.focus();
                }}
            >
                Ir para o conteúdo
            </a>
            <header>
                <strong>WMS Rodogarcia</strong>
                <span>DESENVOLVIMENTO REAL · acesso pendente</span>
            </header>
            <main id="conteudo" tabIndex={-1}>
                <h1>Sessão real indisponível</h1>
                <p role="status">
                    Não há sessão autenticada. Nenhuma consulta ou comando
                    operacional foi enviado.
                </p>
                <p>
                    O provedor de identidade e o fluxo de entrada real ainda
                    precisam de configuração. Não há entrada com senha nem
                    emissão de token nesta aplicação.
                </p>
                <p>
                    O perfil e o alcance por cliente e armazém virão da
                    identidade real. O servidor verifica cada operação.
                </p>
                <p>
                    O exercício fictício é um modo separado e explícito; não
                    substitui a integração real.
                </p>
            </main>
        </>
    );
}
