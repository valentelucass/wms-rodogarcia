package br.com.rodogarcia.wms.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Segredos somente via configuração externa; esta classe nunca é resposta de API. */
@Component
@ConfigurationProperties(prefix = "wms.notazz")
public class NotazzProperties {
    private boolean enabled;
    private boolean productionEnabled;
    private int timeoutSeconds = 12;
    private Map<Long, Emitente> emitentes = new LinkedHashMap<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isProductionEnabled() { return productionEnabled; }
    public void setProductionEnabled(boolean value) { productionEnabled = value; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int value) { timeoutSeconds = Math.clamp(value, 2, 15); }
    public Map<Long, Emitente> getEmitentes() { return emitentes; }
    public void setEmitentes(Map<Long, Emitente> value) { emitentes = value; }

    public static class Emitente {
        private String cnpj;
        private String apiKey;
        private int ambiente = 2;
        private String finalidade;
        private String naturezaOperacao;
        private String finalidadeEntrada;
        private String naturezaEntrada;
        private boolean configuracaoConferida;
        private String documentoConfiguracao = "{}";
        public String getCnpj() { return cnpj; }
        public void setCnpj(String value) { cnpj = value; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String value) { apiKey = value; }
        public int getAmbiente() { return ambiente; }
        public void setAmbiente(int value) { ambiente = value; }
        public String getFinalidade() { return finalidade; }
        public void setFinalidade(String value) { finalidade = value; }
        public String getNaturezaOperacao() { return naturezaOperacao; }
        public void setNaturezaOperacao(String value) { naturezaOperacao = value; }
        public String getFinalidadeEntrada() { return finalidadeEntrada; }
        public void setFinalidadeEntrada(String value) { finalidadeEntrada = value; }
        public String getNaturezaEntrada() { return naturezaEntrada; }
        public void setNaturezaEntrada(String value) { naturezaEntrada = value; }
        public boolean isConfiguracaoConferida() { return configuracaoConferida; }
        public void setConfiguracaoConferida(boolean value) { configuracaoConferida = value; }
        public String getDocumentoConfiguracao() { return documentoConfiguracao; }
        public void setDocumentoConfiguracao(String value) { documentoConfiguracao = value; }
    }
}
