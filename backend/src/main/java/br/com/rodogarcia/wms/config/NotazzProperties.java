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
    private java.util.Set<String> cnpjsHomologacaoPermitidos =
            new java.util.LinkedHashSet<>(java.util.Set.of("04547874000114"));
    private Map<Long, Emitente> emitentes = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isProductionEnabled() {
        return productionEnabled;
    }

    public void setProductionEnabled(boolean value) {
        productionEnabled = value;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int value) {
        timeoutSeconds = Math.clamp(value, 2, 15);
    }

    public Map<Long, Emitente> getEmitentes() {
        return emitentes;
    }

    public java.util.Set<String> getCnpjsHomologacaoPermitidos() {
        return cnpjsHomologacaoPermitidos;
    }

    public void setCnpjsHomologacaoPermitidos(java.util.Set<String> value) {
        cnpjsHomologacaoPermitidos = value;
    }

    /** A chave de homologação deve corresponder àquela previamente conferida para o CNPJ. */
    public boolean ambientePermitido(Emitente e) {
        if (e == null) return false;
        if (e.ambiente == 1) return productionEnabled;
        if (e.ambiente != 2
                || cnpjsHomologacaoPermitidos == null
                || !cnpjsHomologacaoPermitidos.contains(e.cnpj)
                || e.apiKey == null
                || e.apiKeySha256Conferida == null
                || !e.apiKeySha256Conferida.matches("[a-fA-F0-9]{64}")) return false;
        try {
            byte[] atual =
                    java.security.MessageDigest.getInstance("SHA-256")
                            .digest(e.apiKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.security.MessageDigest.isEqual(
                    atual, java.util.HexFormat.of().parseHex(e.apiKeySha256Conferida));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível.");
        }
    }

    public void setEmitentes(Map<Long, Emitente> value) {
        emitentes = value;
    }

    public static class Emitente {
        private String cnpj;
        private String apiKey;
        private String apiKeySha256Conferida;
        private int ambiente = 2;
        private String finalidade;
        private String naturezaOperacao;
        private String finalidadeEntrada;
        private String naturezaEntrada;
        private boolean configuracaoConferida;
        private String documentoConfiguracao = "{}";

        public String getCnpj() {
            return cnpj;
        }

        public void setCnpj(String value) {
            cnpj = value;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String value) {
            apiKey = value;
        }

        public String getApiKeySha256Conferida() {
            return apiKeySha256Conferida;
        }

        public void setApiKeySha256Conferida(String value) {
            apiKeySha256Conferida = value;
        }

        public int getAmbiente() {
            return ambiente;
        }

        public void setAmbiente(int value) {
            ambiente = value;
        }

        public String getFinalidade() {
            return finalidade;
        }

        public void setFinalidade(String value) {
            finalidade = value;
        }

        public String getNaturezaOperacao() {
            return naturezaOperacao;
        }

        public void setNaturezaOperacao(String value) {
            naturezaOperacao = value;
        }

        public String getFinalidadeEntrada() {
            return finalidadeEntrada;
        }

        public void setFinalidadeEntrada(String value) {
            finalidadeEntrada = value;
        }

        public String getNaturezaEntrada() {
            return naturezaEntrada;
        }

        public void setNaturezaEntrada(String value) {
            naturezaEntrada = value;
        }

        public boolean isConfiguracaoConferida() {
            return configuracaoConferida;
        }

        public void setConfiguracaoConferida(boolean value) {
            configuracaoConferida = value;
        }

        public String getDocumentoConfiguracao() {
            return documentoConfiguracao;
        }

        public void setDocumentoConfiguracao(String value) {
            documentoConfiguracao = value;
        }
    }
}
