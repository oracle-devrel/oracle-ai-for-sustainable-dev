package oracle.microservices.graphql.resolver;

import java.math.BigDecimal;
import java.util.Optional;

import oracle.microservices.graphql.repository.BankRepository;
import oracle.microservices.graphql.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import oracle.microservices.graphql.model.Bank;
import oracle.microservices.graphql.model.Account;
import com.coxautodev.graphql.tools.GraphQLMutationResolver;

@Component
public class Mutation implements GraphQLMutationResolver {
	private BankRepository bankRepository;
	private AccountRepository accountRepository;

	@Autowired
	public Mutation(BankRepository bankRepository, AccountRepository accountRepository) {
		this.bankRepository = bankRepository;
		this.accountRepository = accountRepository;
	}

	public Bank createBank(String name, Integer routing) {
		Bank bank = new Bank();
		bank.setName(name);
		bank.setRouting(routing);
		bankRepository.save(bank);
		return bank;
	}

	public Account createAccount(BigDecimal balance, String description, Long bankId) {
		Account book = new Account();
		book.setBank(new Bank(bankId));
		book.setBalance(balance);
		book.setDescription(description);
		accountRepository.save(book);
		return book;
	}

	public boolean deleteAccount(Long id) {
		accountRepository.deleteById(id);
		return true;
	}

	public Account updateAccount(Long id, BigDecimal balance, String description) throws Exception {
		Optional<Account> optionalAccount = accountRepository.findById(id);
		if (optionalAccount.isPresent()) {
			Account account = optionalAccount.get();
			if (balance != null)
				account.setBalance(balance);
			if (description != null)
				account.setDescription(description);
			accountRepository.save(account);
			return account;
		}
		throw new Exception("No account found to update.");
	}
}
