package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.EventoAcesso;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoAcessoRepository extends JpaRepository<EventoAcesso, String> {}
