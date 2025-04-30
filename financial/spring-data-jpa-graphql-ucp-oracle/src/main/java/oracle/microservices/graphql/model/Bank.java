package oracle.microservices.graphql.model;

import javax.persistence.*;

@Entity
public class Bank {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "routing")
	private Integer routing;

	public Bank() {
	}

	public Bank(Long id) {
		this.id = id;
	}

	public Bank(String name, Integer routing) {
		this.name = name;
		this.routing = routing;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getRouting() {
		return routing;
	}

	public void setRouting(Integer routing) {
		this.routing = routing;
	}

	@Override
	public String toString() {
		return "User [id=" + id + ", name=" + name + ", routing=" + routing + "]";
	}

}
