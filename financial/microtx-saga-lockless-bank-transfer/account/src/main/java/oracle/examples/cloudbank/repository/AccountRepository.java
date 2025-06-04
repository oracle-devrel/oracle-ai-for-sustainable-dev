package oracle.examples.cloudbank.repository;

import oracle.examples.cloudbank.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository <Account, Long> {
    List<Account> findAccountsByAccountNameContains (String accountName);

    List<Account> findByAccountCustomerId(String customerId);

    Account findByAccountId(long accountId);

}
