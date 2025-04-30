package oracle.microservices.graphql.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import oracle.microservices.graphql.model.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {

}
