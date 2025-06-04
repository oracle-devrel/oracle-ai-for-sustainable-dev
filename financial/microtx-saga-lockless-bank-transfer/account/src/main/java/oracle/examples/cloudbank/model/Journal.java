package oracle.examples.cloudbank.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

/**
 * A separate blockchain table could be used for the ledger aspects but currently this Journal serves multiple purposes:
 *  - Ledger for transfer operations
 *  - Journal for tracking changes made in an LRA
 *  - Store for LRA state
 */
@Entity
@Table(name = "JOURNAL")
@Data
@NoArgsConstructor
public class Journal  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "JOURNAL_ID")
    private long journalId;

    /**
     * Eg withdraw or deposit
     */
    @Column(name = "JOURNAL_TYPE")
    private String journalType;

    @Column(name = "ACCOUNT_ID")
    private long accountId;

    @Column(name = "LRA_ID")
    private String lraId;

    @Column(name = "LRA_STATE")
    private String lraState;

    @Column(name = "JOURNAL_AMOUNT")
    private long journalAmount;

    public Journal(String journalType, long accountId, long journalAmount, String lraId, String lraState) {
        this.journalType = journalType;
        this.accountId = accountId;
        this.lraId = lraId;
        this.lraState = lraState;
        this.journalAmount = journalAmount;
    }
}
