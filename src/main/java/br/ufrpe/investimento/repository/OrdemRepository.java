package br.ufrpe.investimento.repository;

import br.ufrpe.investimento.model.Ordem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrdemRepository extends JpaRepository<Ordem, UUID> {

    List<Ordem> findByUsuarioIdOrderByDataCriacaoDesc(UUID usuarioId);
}
