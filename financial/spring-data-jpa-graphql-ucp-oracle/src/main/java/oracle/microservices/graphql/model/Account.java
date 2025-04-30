package oracle.microservices.graphql.model;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
public class Account {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Column(name = "balance", nullable = false)
	private BigDecimal balance;

	@Column(name = "description")
	private String description;

	@ManyToOne
	@JoinColumn(name = "bank_id", nullable = false, updatable = false)
	private Bank bank;

	public Account() {
	}

	public Account(BigDecimal balance, String description, Bank bank) {
		this.balance = balance;
		this.description = description;
		this.bank = bank;
	}

	public Long getId() {
		return id;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Bank getBank() {
		return bank;
	}

	public void setBank(Bank bank) {
		this.bank = bank;
	}

	@Override
	public String toString() {
		return "Account [id=" + id + ", balance=" + balance + ", description=" + description + ", bank=" + bank + "]";
	}

}
