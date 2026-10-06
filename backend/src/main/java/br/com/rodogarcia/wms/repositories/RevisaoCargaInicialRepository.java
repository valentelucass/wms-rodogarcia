package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.RevisaoCargaInicial;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevisaoCargaInicialRepository extends JpaRepository<RevisaoCargaInicial, Long> {
    Optional<RevisaoCargaInicial> findByCargaIdAndNumero(Long cargaId, int numero);

    Page<RevisaoCargaInicial> findByCargaId(Long cargaId, Pageable pagina);
}
