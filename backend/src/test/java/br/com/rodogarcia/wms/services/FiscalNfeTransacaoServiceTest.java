package br.com.rodogarcia.wms.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.config.NotazzProperties;
import br.com.rodogarcia.wms.dto.ExpedicaoDto;
import br.com.rodogarcia.wms.dto.FiscalNfeDto;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.PedidoSaida;
import br.com.rodogarcia.wms.models.SituacaoPedidoSaida;
import br.com.rodogarcia.wms.models.SolicitacaoNfe;
import br.com.rodogarcia.wms.repositories.CoberturaDocumentoSaidaRepository;
import br.com.rodogarcia.wms.repositories.DocumentoSaidaRepository;
import br.com.rodogarcia.wms.repositories.FiscalNfeRepository;
import br.com.rodogarcia.wms.repositories.PedidoSaidaRepository;
import br.com.rodogarcia.wms.repositories.ReservaSaidaRepository;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.json.JsonMapper;

class FiscalNfeTransacaoServiceTest {
    final FiscalNfeRepository repository=mock(FiscalNfeRepository.class);
    final PedidoSaidaService pedidos=mock(PedidoSaidaService.class);
    final PedidoSaidaRepository pedidosRepo=mock(PedidoSaidaRepository.class);
    final FiscalNfePreparacaoService preparo=mock(FiscalNfePreparacaoService.class);
    final DocumentoSaidaRepository docs=mock(DocumentoSaidaRepository.class);
    final CoberturaDocumentoSaidaRepository coberturas=mock(CoberturaDocumentoSaidaRepository.class);
    final ReservaSaidaRepository reservas=mock(ReservaSaidaRepository.class);
    final FiscalNfeXml xml=mock(FiscalNfeXml.class);
    final AcessoService acesso=mock(AcessoService.class);
    final NotazzProperties properties=new NotazzProperties();
    final JsonMapper mapper=new JsonMapper();
    final PedidoSaida pedido=mock(PedidoSaida.class);
    final FiscalNfeDto.Confirmar comando=new FiscalNfeDto.Confirmar(UUID.randomUUID(),4L,"a".repeat(64),"RETORNO_MERCADORIA",null,"Conferência de teste");
    FiscalNfeTransacaoServiceTest(){
        when(pedidos.bloquear(61L)).thenReturn(pedido);when(pedido.getId()).thenReturn(61L);when(pedido.getVersao()).thenReturn(4L);when(pedido.getSituacao()).thenReturn(SituacaoPedidoSaida.SEPARADO);
        var a=mock(Armazem.class);when(a.getId()).thenReturn(46L);when(pedido.getArmazem()).thenReturn(a);
        var scope=mock(PedidoSaidaRepository.Escopo.class);when(scope.getClienteId()).thenReturn(83L);when(scope.getArmazemId()).thenReturn(46L);when(pedidosRepo.buscarEscopo(61L)).thenReturn(Optional.of(scope));
        var e=new NotazzProperties.Emitente();e.setCnpj(FiscalNfeTestFixture.CNPJ);e.setAmbiente(2);e.setApiKey("ficticio");e.setConfiguracaoConferida(true);properties.setEnabled(true);properties.getEmitentes().put(46L,e);
        when(acesso.usuario()).thenReturn("usuario");
        when(preparo.preparar(eq(61L),any())).thenReturn(new FiscalNfePreparacaoService.Preparacao(new FiscalNfeDto.Previa(61L,4L,"a".repeat(64),true,true,List.of(),List.of(FiscalNfeTestFixture.documento()),null),List.of("{}")));
    }
    FiscalNfeTransacaoService service(){return new FiscalNfeTransacaoService(repository,pedidos,pedidosRepo,preparo,docs,coberturas,reservas,xml,acesso,properties,mapper,Clock.fixed(FiscalNfeTestFixture.AGORA,ZoneOffset.UTC));}
    @Test void confirmaSnapshotParcelaEAuditoriaAntesDeQualquerEnvio() {
        service().confirmar(61L,comando);var s=ArgumentCaptor.forClass(SolicitacaoNfe.class);verify(repository).inserir(s.capture());
        assertThat(s.getValue().situacao()).isEqualTo("PREPARADA");assertThat(s.getValue().corpoJson()).isEqualTo("{}");assertThat(s.getValue().documentoJson()).contains("Saída WMS: SAI-TESTE");
        verify(repository).parcela(eq(s.getValue().id()),eq(7L),eq(8L),eq(9L),any(),any());verify(repository).evento(eq(s.getValue().id()),eq("CONFIRMADA"),eq("usuario"),eq("Conferência de teste"),any());
    }
    @Test void mesmoGrupoAtivoNaoCriaOutraIntencao() {
        when(repository.ativas(61L,"RETORNO_MERCADORIA")).thenReturn(List.of(FiscalNfeTestFixture.solicitacao("DESCONHECIDO")));
        assertThatThrownBy(()->service().confirmar(61L,comando)).hasMessageContaining("Já existe");verify(repository,never()).inserir(any());
    }
    @Test void previaAlteradaNaoPersisteEmissao() {
        var d=new FiscalNfeDto.Confirmar(comando.operacaoId(),4L,"b".repeat(64),comando.natureza(),null,comando.motivo());
        assertThatThrownBy(()->service().confirmar(61L,d)).hasMessageContaining("mudaram");verify(repository,never()).inserir(any());
    }
    @Test void operacaoReutilizadaComConteudoDiferenteEhConflito() {
        when(repository.daOperacao(comando.operacaoId().toString())).thenReturn(List.of(FiscalNfeTestFixture.solicitacao("PREPARADA")));
        assertThatThrownBy(()->service().confirmar(61L,comando)).hasMessageContaining("outros dados");verify(repository,never()).inserir(any());
    }
    @Test void permissaoAtualEhObrigatoriaMesmoEmRepeticao() {
        doThrow(new AccessDeniedException("negado")).when(acesso).exigirSupervisor();assertThatThrownBy(()->service().confirmar(61L,comando)).isInstanceOf(AccessDeniedException.class);verify(repository,never()).daOperacao(any());
    }
    @Test void leaseConcorrenteNaoMudaEstadoNemPermiteSegundoEnvio() {
        var s=FiscalNfeTestFixture.solicitacao("PREPARADA");when(repository.consultar(eq(s.id()),anyBoolean())).thenReturn(s);when(repository.ocuparCanal(any(),anyInt(),any(),any(),any())).thenReturn(false);
        assertThat(service().adquirir(61L,s.id(),"CRIAR",null,"dono")).isNull();verify(repository,never()).estado(any(),any(),any(),any());
    }
    @Test void resultadoPerdidoNaoEhRejeicaoOuCancelamentoConfirmado() {
        var s=FiscalNfeTestFixture.solicitacao("ENVIANDO");when(repository.consultar(eq(s.id()),anyBoolean())).thenReturn(s);service().falhou(61L,s.id(),"dono",false);
        verify(repository).estado(eq(s.id()),eq("DESCONHECIDO"),any(),any());verify(docs,never()).save(any());
    }
    @Test void depoisDeRetiradaCancelamentoDeveEncaminharNovaEntrada() {
        var s=FiscalNfeTestFixture.solicitacao("AUTORIZADA");when(repository.consultar(eq(s.id()),anyBoolean())).thenReturn(s);when(pedido.getSituacao()).thenReturn(SituacaoPedidoSaida.RETIRADO);
        when(xml.conferir(any(),any())).thenReturn(new FiscalNfeXml.Autorizacao(new ExpedicaoDto.Nota(FiscalNfeTestFixture.CNPJ,"1","123",java.time.LocalDate.of(2026,10,23),FiscalNfeTestFixture.CHAVE),"123456789012345",FiscalNfeTestFixture.AGORA.minusSeconds(60)));
        assertThatThrownBy(()->service().adquirir(61L,s.id(),"CANCELAR",new FiscalNfeDto.Comando(UUID.randomUUID(),"Desistência após retorno da carga"),"dono")).hasMessageContaining("nova nota de entrada");verify(repository,never()).ocuparCanal(any(),anyInt(),any(),any(),any());
    }
    @Test void xmlDivergenteNaoChegaAInserirDocumentoOuCobertura() {
        var s=FiscalNfeTestFixture.solicitacao("PROCESSANDO");when(repository.consultar(eq(s.id()),anyBoolean())).thenReturn(s);when(xml.conferir(any(),any())).thenThrow(CadastroSupport.invalido("XML divergente"));
        String b=java.util.Base64.getEncoder().encodeToString(FiscalNfeTestFixture.xml().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var retorno=mapper.readTree("{\"statusProcessamento\":\"sucesso\",\"codigoProcessamento\":\"000\",\"id\":\"remoto\",\"external_id\":\""+s.id()+"\",\"statusNota\":\"Autorizada\",\"xmlBase64\":\""+b+"\"}");
        assertThatThrownBy(()->service().recebido(61L,s.id(),"CONSULTAR",retorno,"dono")).hasMessageContaining("XML divergente");verify(docs,never()).saveAndFlush(any());verify(coberturas,never()).save(any());
    }
}
