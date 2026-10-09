export const developer = {
    name: "Lucas Andrade",
    profile: "https://www.linkedin.com/in/dev-lucasandrade/",
    support: "mailto:lucasmac.dev@gmail.com",
    email: "lucasmac.dev@gmail.com",
};

export function AppFooter() {
    return (
        <footer className="app-footer">
            <div className="app-footer-content">
                <div className="app-footer-product">
                    <strong>
                        Rodogarcia <span aria-hidden="true">·</span> WMS
                    </strong>
                    <span>
                        © {new Date().getFullYear()} Rodogarcia. Todos os
                        direitos reservados.
                    </span>
                </div>
                <div className="app-footer-credit">
                    <span>
                        Desenvolvido por{" "}
                        <a
                            href={developer.profile}
                            target="_blank"
                            rel="noopener noreferrer"
                        >
                            {developer.name}
                            <span className="sr-only"> (abre em nova aba)</span>
                        </a>
                    </span>
                    <span>
                        Suporte:{" "}
                        <a href={developer.support}>{developer.email}</a>
                    </span>
                </div>
            </div>
        </footer>
    );
}
