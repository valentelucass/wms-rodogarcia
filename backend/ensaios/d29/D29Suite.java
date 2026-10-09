/** Dispatcher D29. Preparacao nao autoriza etapas incompletas. */
final class D29Suite {
    static void run(EnsaioD29 x,String mode) throws Exception {
        if(mode.equals("entrada"))new EntradaD29(x).run();
        else if(mode.equals("estoque"))new EstoqueSaidaD29(x).run();
        else if(mode.equals("concorrencia"))new ConcorrenciaD29(x).run();
        else if(mode.equals("carga-contagem"))new CargaContagemD29(x).run();
        else if(mode.equals("financeiro"))new FinanceiroHttpD29(x).run();
        else if(mode.equals("saida"))new SegurancaXmlD29(x).run();
        else if(mode.equals("fiscal-servicos"))new ConformidadeD29(x).run();
        else if(mode.equals("financeiro-versoes"))new FinanceiroVersoesD29(x).run();
        else if(mode.equals("fifo"))new FifoBobinasD29(x).run();
        else if(mode.equals("retornos"))new RetornosD29(x).run();
        else if(mode.equals("capacidade-integral"))new CapacidadeIntegralD29(x).run();
        else if(mode.equals("xml-fronteiras"))new XmlFronteirasD29(x).run();
        else if(mode.equals("replays-finais"))new ReplayFinalD29(x).run();
        else if(mode.equals("get-final"))new PendenciasD29(x).run(mode);
        else throw new IllegalStateException("D29_ETAPA_AINDA_EM_PREPARO");
    }
}
