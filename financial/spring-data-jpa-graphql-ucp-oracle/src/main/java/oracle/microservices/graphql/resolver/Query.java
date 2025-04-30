package oracle.microservices.graphql.resolver;

import oracle.microservices.graphql.repository.BankRepository;
import oracle.microservices.graphql.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import oracle.microservices.graphql.model.Bank;
import oracle.microservices.graphql.model.Account;
import com.coxautodev.graphql.tools.GraphQLQueryResolver;

@Component
public class Query implements GraphQLQueryResolver {
	private BankRepository bankRepository;
	private AccountRepository accountRepository;

	@Autowired
	public Query(BankRepository bankRepository, AccountRepository accountRepository) {
		this.bankRepository = bankRepository;
		this.accountRepository = accountRepository;
	}

	public Iterable<Bank> findAllBanks() {
		return bankRepository.findAll();
	}

	public Iterable<Account> findAllAccounts() {
		return accountRepository.findAll();
	}

	public long countBanks() {
		return bankRepository.count();
	}

	public long countAccounts() {
		return accountRepository.count();
	}

}
