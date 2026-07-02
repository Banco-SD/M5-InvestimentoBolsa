package br.ufrpe.investimento.repository;

import br.ufrpe.investimento.model.Ativo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtivoRepository extends JpaRepository<Ativo, String> {
}
