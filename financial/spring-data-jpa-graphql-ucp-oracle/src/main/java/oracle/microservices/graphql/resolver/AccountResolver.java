package oracle.microservices.graphql.resolver;

import oracle.microservices.graphql.repository.BankRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import oracle.microservices.graphql.model.Bank;
import oracle.microservices.graphql.model.Account;
import com.coxautodev.graphql.tools.GraphQLResolver;

@Component
public class AccountResolver implements GraphQLResolver<Account> {
	@Autowired
	private BankRepository bankRepository;

	public AccountResolver(BankRepository bankRepository) {
		this.bankRepository = bankRepository;
	}

	public Bank getBank(Account account) {
		return bankRepository.findById(account.getBank().getId()).orElseThrow(null);
	}
}
