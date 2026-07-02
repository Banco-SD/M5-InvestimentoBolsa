package br.ufrpe.investimento.repository;

import br.ufrpe.investimento.model.PosicaoCarteira;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PosicaoCarteiraRepository extends JpaRepository<PosicaoCarteira, UUID> {

    Optional<PosicaoCarteira> findByUsuarioIdAndTicker(UUID usuarioId, String ticker);

    List<PosicaoCarteira> findByUsuarioId(UUID usuarioId);

    // Lock pessimista: evita condição de corrida quando duas ordens de venda
    // do mesmo ativo chegam quase ao mesmo tempo e tentam reservar quantidade
    // da mesma posição (o @Version otimista sozinho aceitaria as duas leituras
    // e uma delas falharia tarde demais, depois de já ter feito outras coisas).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PosicaoCarteira p where p.usuarioId = :usuarioId and p.ticker = :ticker")
    Optional<PosicaoCarteira> buscarComLock(@Param("usuarioId") UUID usuarioId, @Param("ticker") String ticker);
}
