-- Preparado, NAO EXECUTADO. Exclusivamente WMS_DEV local ficticio previamente migrado.
-- Runner exige loopback, alvo/login/TLS e todas as64 tabelas vazias antes deste script.
-- Testa constraints SQL Server; nao substitui regras dos services/concorrencia/recuperacao.
IF DB_NAME() <> N'WMS_DEV' OR @@TRANCOUNT<>0
    THROW 51031,'Ensaio requer WMS_DEV e sessao sem transacao.',1;
SET ANSI_NULLS ON; SET ANSI_PADDING ON; SET ANSI_WARNINGS ON; SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON; SET QUOTED_IDENTIFIER ON; SET NUMERIC_ROUNDABORT OFF;
-- OFF somente neste ensaio: erros de constraint esperados nao devem inviabilizar o rollback final.
SET XACT_ABORT OFF;
DECLARE @agora datetime2(6)=SYSUTCDATETIME(),@cliente bigint,@armazem bigint,@produto bigint,
    @embalagem bigint,@entrada bigint,@nota bigint,@unidade bigint,@saida bigint,@item bigint;
DECLARE @casos TABLE(caso varchar(80),aprovado bit);
BEGIN TRY
    BEGIN TRANSACTION;
    INSERT wms.cliente(versao,situacao,criado_em,alterado_em,codigo,nome,documento_fiscal)
        VALUES(0,'ATIVO',@agora,@agora,'D20_FICTICIO',N'D20 ficticio','D20_FICTICIO');
    SET @cliente=SCOPE_IDENTITY();
    INSERT wms.armazem(versao,situacao,criado_em,alterado_em,codigo,nome,documento_fiscal,cidade,uf)
        VALUES(0,'ATIVO',@agora,@agora,'D20_FICTICIO',N'D20 ficticio','D20_FICTICIO',N'Ficticia','SP');
    SET @armazem=SCOPE_IDENTITY();
    INSERT wms.produto(versao,situacao,criado_em,alterado_em,cliente_id,sku,descricao,unidade_medida,
        tipo_quantidade,precisao_quantidade,controla_lote,controla_validade)
        VALUES(0,'ATIVO',@agora,@agora,@cliente,'D20_SKU',N'Ficticio','UN','CONTAGEM',0,0,0);
    SET @produto=SCOPE_IDENTITY();
    BEGIN TRY
        UPDATE wms.produto SET precisao_quantidade=1 WHERE id=@produto;
        THROW 51032,'CHECK aceitou precisao de CONTAGEM invalida.',1;
    END TRY BEGIN CATCH
        IF ERROR_NUMBER()<>547 THROW;
        INSERT @casos VALUES('CONTAGEM recusa escala1',1);
    END CATCH;
    INSERT wms.embalagem(versao,situacao,criado_em,alterado_em,produto_id,codigo_dun,descricao,quantidade_produto)
        VALUES(0,'ATIVO',@agora,@agora,@produto,'D20_DUN',N'Ficticia',1);
    SET @embalagem=SCOPE_IDENTITY();
    BEGIN TRY
        INSERT wms.embalagem(versao,situacao,criado_em,alterado_em,produto_id,codigo_dun,descricao,quantidade_produto)
            VALUES(0,'ATIVO',@agora,@agora,-1,'D20_INVALIDO',N'Ficticia',1);
        THROW 51032,'FK aceitou produto ausente.',1;
    END TRY BEGIN CATCH
        IF ERROR_NUMBER()<>547 THROW;
        INSERT @casos VALUES('FK recusa produto ausente',1);
    END CATCH;
    INSERT wms.pedido_entrada(versao,cliente_id,armazem_id,referencia,situacao,criado_em,alterado_em)
        VALUES(0,@cliente,@armazem,'D20_ENTRADA','RASCUNHO',@agora,@agora);
    SET @entrada=SCOPE_IDENTITY();
    INSERT wms.nota_entrada(pedido_id,emitente,serie,numero,emissao)
        VALUES(@entrada,'D20_FICTICIO',0,1,'20261006');
    SET @nota=SCOPE_IDENTITY();
    INSERT wms.nota_entrada(pedido_id,emitente,serie,numero,emissao)
        VALUES(@entrada,'D20_FICTICIO',0,2,'20261006');
    INSERT @casos VALUES('Duas notas sem chave NULL permitidas',1);
    INSERT wms.unidade_logistica(versao,codigo,pedido_id,nota_id,produto_id,embalagem_id,tipo,condicao,
        data_fifo,chegada_real,quantidade,ativa,criada_em,alterada_em)
        VALUES(0,CONVERT(varchar(36),NEWID()),@entrada,@nota,@produto,@embalagem,'PALLET','BOA',@agora,@agora,10,1,@agora,@agora);
    SET @unidade=SCOPE_IDENTITY();
    INSERT wms.pedido_saida(versao,cliente_id,armazem_id,referencia,situacao,criado_em,alterado_em)
        VALUES(0,@cliente,@armazem,'D20_SAIDA','RASCUNHO',@agora,@agora);
    SET @saida=SCOPE_IDENTITY();
    INSERT wms.item_pedido_saida(pedido_id,produto_id,quantidade) VALUES(@saida,@produto,2);
    SET @item=SCOPE_IDENTITY();
    INSERT wms.reserva_saida(pedido_id,item_id,unidade_id,operacao_reserva_id,quantidade,situacao,criada_em)
        VALUES(@saida,@item,@unidade,CONVERT(varchar(36),NEWID()),2,'ATIVA',@agora);
    BEGIN TRY
        INSERT wms.reserva_saida(pedido_id,item_id,unidade_id,operacao_reserva_id,quantidade,situacao,criada_em)
            VALUES(@saida,@item,@unidade,CONVERT(varchar(36),NEWID()),2,'ATIVA',@agora);
        THROW 51032,'Indice filtrado aceitou duas reservas ativas.',1;
    END TRY BEGIN CATCH
        IF ERROR_NUMBER() NOT IN(2601,2627) THROW;
        INSERT @casos VALUES('Uma reserva ATIVA por unidade',1);
    END CATCH;
    UPDATE wms.reserva_saida SET situacao='REVERTIDA',encerrada_em=@agora WHERE unidade_id=@unidade;
    INSERT wms.reserva_saida(pedido_id,item_id,unidade_id,operacao_reserva_id,quantidade,situacao,criada_em)
        VALUES(@saida,@item,@unidade,CONVERT(varchar(36),NEWID()),2,'ATIVA',@agora);
    INSERT @casos VALUES('Reserva nova apos reversao conserva historico',1);
    IF XACT_STATE()<>1 THROW 51032,'Transacao do ensaio invalida.',1;
    -- Table variable nao e desfeita pelo rollback. Nenhum dado ficticio e confirmado.
    ROLLBACK TRANSACTION;
    SELECT caso,aprovado FROM @casos;
END TRY BEGIN CATCH
    IF XACT_STATE()<>0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
