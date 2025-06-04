package oracle.examples.cloudbank.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;
//import org.hibernate.annotations.Generated;
//import org.hibernate.annotations.GenerationTime;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(
    name = "ACCOUNTS",
    uniqueConstraints = @UniqueConstraint(name = "pk_accountid", columnNames = "ACCOUNT_ID") // Define the primary key constraint
)
@Data
@NoArgsConstructor
public class Account {

    @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ACCOUNT_ID", nullable = false, unique = true) // Ensure the column is unique and not null
    @JsonProperty("_id")
    private long accountId;

    @Column(name = "ACCOUNT_NAME")
    private String accountName;

    @Column(name = "ACCOUNT_TYPE")
    private String accountType;

    @Column(name = "CUSTOMER_ID")
    private String accountCustomerId;

//    @Generated(GenerationTime.INSERT)
//    @Column(name = "ACCOUNT_OPENED_DATE", updatable = false, insertable = false)
    @Column(name = "ACCOUNT_OPENED_DATE")
    private Date accountOpenedDate;

    @Column(name = "ACCOUNT_OTHER_DETAILS")
    private String accountOtherDetails;

    @Column(name = "ACCOUNT_BALANCE")
    private long accountBalance;

    public Account(String accountName, String accountType, String accountOtherDetails, String accountCustomerId) {
        this.accountName = accountName;
        this.accountType = accountType;
        this.accountOtherDetails = accountOtherDetails;
        this.accountCustomerId = accountCustomerId;
    }
}
