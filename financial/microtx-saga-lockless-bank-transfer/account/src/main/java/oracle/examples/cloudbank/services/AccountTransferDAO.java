package oracle.examples.cloudbank.services;

import oracle.examples.cloudbank.model.Account;
import oracle.examples.cloudbank.model.Journal;
import oracle.examples.cloudbank.repository.AccountRepository;
import oracle.examples.cloudbank.repository.JournalRepository;
import com.oracle.microtx.springboot.lra.annotation.LRAStatus;
import com.oracle.microtx.springboot.lra.annotation.ParticipantStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;


@Component
public class AccountTransferDAO {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static AccountTransferDAO singleton;
    final AccountRepository accountRepository;
    final JournalRepository journalRepository;
    public AccountTransferDAO(AccountRepository accountRepository, JournalRepository journalRepository) {
        this.accountRepository = accountRepository;
        this.journalRepository = journalRepository;
        singleton = this;
    }

    public static AccountTransferDAO instance() {
        return singleton;
    }

    public static String getStatusString(ParticipantStatus status) {
        switch (status) {
            case Compensated:
                return "Compensated";
            case Completed:
                return "Completed";
            case FailedToCompensate:
                return "Failed to Compensate";
            case FailedToComplete:
                return "Failed to Complete";
            case Active:
                return "Active";
            case Compensating:
                return "Compensating";
            case Completing:
                return "Completing";
            default:
                return "Unknown";
        }
    }
    public static boolean isLRASuccessfullyEnded(LRAStatus status) {
        return status.equals(LRAStatus.Cancelled) || status.equals(LRAStatus.Closed);
    }
    public static String getStatusStringForLRAStatus(LRAStatus status) {
        switch (status) {
            case Active:
                return "Active";
            case Cancelled:
                return "Cancelled";
            case Closed:
                return "Closed";
            case Cancelling:
                return "Cancelling";
            case Closing:
                return "Closing";
            case FailedToCancel:
                return "FailedToCancel";
            case FailedToClose:
                return "FailedToClose";
            default:
                return "Unknown";
        }
    }

    public static ParticipantStatus getStatusFromString(String statusString) {
        switch (statusString) {
            case "Compensated":
                return ParticipantStatus.Compensated;
            case "Completed":
                return ParticipantStatus.Completed;
            case "Failed to Compensate":
                return ParticipantStatus.FailedToCompensate;
            case "Failed to Complete":
                return ParticipantStatus.FailedToComplete;
            case "Active":
                return ParticipantStatus.Active;
            case "Compensating":
                return ParticipantStatus.Compensating;
            case "Completing":
                return ParticipantStatus.Completing;
            default:
                return null;
        }
    }

    public void saveAccount(Account account) {
        log.info("saveAccount account" + account.getAccountId() + " account" + account.getAccountBalance());
        accountRepository.save(account);
    }

    public ResponseEntity<?> status(String lraId, String journalType) throws Exception {
        Journal journal = getJournalForLRAid(lraId, journalType);
        if (AccountTransferDAO.getStatusFromString(journal.getLraState()).equals(ParticipantStatus.Compensated))
            return ResponseEntity.ok(ParticipantStatus.Compensated);
        else return ResponseEntity.ok(ParticipantStatus.Completed);
    }

    public void afterLRA(String lraId, LRAStatus status, String journalType) throws Exception {
        //In the case of successful ending status of the LRA, we could purge the journal of the LRA here by doing...
        //if (isLRASuccessfullyEnded(status)) journalRepository.delete(getJournalForLRAid(lraId, journalType));
        //However, we keep the entry for analysis/auditing after the fact.
    }

    Account getAccountForJournal(Journal journal) throws Exception {
        Account account = accountRepository.findByAccountId(journal.getAccountId());
        if (account == null) throw new Exception("Invalid accountName:" + journal.getAccountId());
        return account;
    }
     Account getAccountForAccountId(long accountId)  {
         Account account = accountRepository.findByAccountId(accountId);
         if (account == null) return null;
         return account;
    }

     Journal getJournalForLRAid(String lraId, String journalType) throws Exception {
        Journal journal = journalRepository.findJournalByLraIdAndJournalType(lraId, journalType);
        if (journal == null) {
            journalRepository.save(new Journal("unknown", -1, 0, lraId,
                    AccountTransferDAO.getStatusString(ParticipantStatus.FailedToComplete)));
            throw new Exception("Journal entry does not exist for lraId:" + lraId);
        }
        return journal;
    }

    public void saveJournal(Journal journal) {
        journalRepository.save(journal);
    }
}
