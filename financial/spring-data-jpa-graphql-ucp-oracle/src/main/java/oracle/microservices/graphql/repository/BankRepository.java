package oracle.microservices.graphql.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import oracle.microservices.graphql.model.Bank;

public interface BankRepository extends JpaRepository<Bank, Long> {

}