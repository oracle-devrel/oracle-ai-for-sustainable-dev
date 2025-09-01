package oracle.examples.cloudbank.services;

import oracle.examples.cloudbank.model.Account;
import oracle.examples.cloudbank.model.Journal;
import com.oracle.microtx.springboot.lra.annotation.*;
import com.oracle.microtx.springboot.lra.annotation.LRA;
import com.oracle.microtx.springboot.lra.lockfree.MicroTxLockFreeReservation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Connection;
import java.util.logging.Logger;

import static com.oracle.microtx.springboot.lra.annotation.LRA.*;


@RestController
@RequestMapping("/withdraw")
public class AccountsWithdrawService {
    private static final Logger log = Logger.getLogger(AccountsWithdrawService.class.getName());
    public static final String WITHDRAW = "WITHDRAW";
    public static boolean isUseLockFreeReservations; // is set per tx/withdraw request but is static so that it can be used elsewhere without being passed around (so note that it can be overridden mid saga as it is)

    @PersistenceContext
    private EntityManager entityManager; // Inject the EntityManager

    @Autowired
    private MicroTxLockFreeReservation microTxLockFreeReservation;

    /**
     * Reduce account balance by given amount and write journal entry re the same. Both actions in same local tx
     */
    @RequestMapping(value = "/withdraw", method = RequestMethod.POST)
    @LRA(value = LRA.Type.MANDATORY, end = false)
    @Transactional
    public ResponseEntity<?> withdraw(@RequestHeader(LRA_HTTP_CONTEXT_HEADER) String lraId,
                                      @RequestHeader("crashSimulation") String crashSimulation,
                                      @RequestHeader("isUseLockFreeReservations") boolean isUseLockFreeReservations,
                            @RequestParam("accountId") long accountId,
                            @RequestParam("amount") long withdrawAmount)  {
        log.info("withdraw " + withdrawAmount + " in account:" + accountId + " (lraId:" + lraId + ")... + " +
                "crashSimulation " + crashSimulation);
        Account account = AccountTransferDAO.instance().getAccountForAccountId(accountId);
        if (account == null) {
            log.info("withdraw failed: account does not exist"); //could also do leave here
            AccountTransferDAO.instance().saveJournal(new Journal(WITHDRAW, accountId, 0, lraId,
                    AccountTransferDAO.getStatusString(ParticipantStatus.Active)));
            return ResponseEntity.ok("withdraw failed: account does not exist");
        }
        this.isUseLockFreeReservations = isUseLockFreeReservations;
        if (isUseLockFreeReservations) {
            try {
                Connection connection = entityManager.unwrap(Connection.class);
                log.info("microTxLockFreeReservation.join connection: " + connection);
                // https://docs.oracle.com/en/database/oracle/transaction-manager-for-microservices/24.2/tmmma/com/oracle/microtx/lra/lockfree/MicroTxLockFreeReservation.html
                // https://docs.oracle.com/en/database/oracle/transaction-manager-for-microservices/24.4/tmmdg/verify.html
                // https://www.youtube.com/watch?v=mBijkqoAibE
                microTxLockFreeReservation.join(connection);
            } catch (Exception e) {
                log.warning("Failed to retrieve the underlying connection: " + e.getMessage());
            }
        }
        if (account.getAccountBalance() < withdrawAmount) {
                log.info("withdraw failed: insufficient funds");
                AccountTransferDAO.instance().saveJournal(new Journal(WITHDRAW, accountId, 0, lraId,
                        AccountTransferDAO.getStatusString(ParticipantStatus.Active)));
                return ResponseEntity.ok("withdraw failed: insufficient funds");
            }
        log.info("withdraw current balance:" + account.getAccountBalance() +
                    " new balance:" + (account.getAccountBalance() - withdrawAmount));
        updateAccountBalance(lraId, accountId, withdrawAmount, account);
        return ResponseEntity.ok("withdraw succeeded");
    }


    @Transactional
    private void updateAccountBalanceOriginal(String lraId, long accountId, long withdrawAmount, Account account) {
        account.setAccountBalance(account.getAccountBalance() - withdrawAmount);
        AccountTransferDAO.instance().saveAccount(account);
        AccountTransferDAO.instance().saveJournal(new Journal(WITHDRAW, accountId, withdrawAmount, lraId,
                AccountTransferDAO.getStatusString(ParticipantStatus.Active)));
    }


    @Transactional
    private void updateAccountBalance(String lraId, long accountId, long withdrawAmount, Account account) {
        long newBalance = account.getAccountBalance() - withdrawAmount;
            log.info("updateAccountBalance with createNativeQuery:" + account.getAccountBalance() +
                    " new balance:" +newBalance);
        entityManager.createNativeQuery(
            "UPDATE accounts SET account_balance = account_balance - ? WHERE account_id = ?"
            // "UPDATE accounts SET account_balance = ? WHERE account_id = ?"
        )
        .setParameter(1, withdrawAmount)
        // .setParameter(1, newBalance)
        .setParameter(2, accountId)
        .executeUpdate();
        AccountTransferDAO.instance().saveJournal(new Journal(WITHDRAW, accountId, withdrawAmount, lraId,
                AccountTransferDAO.getStatusString(ParticipantStatus.Active)));
    }

    /**
     * Update LRA state. Do nothing else.
     */
    @RequestMapping(value = "/complete", method = RequestMethod.PUT)
    @Complete
    public ResponseEntity<?> completeWork(@RequestHeader(LRA_HTTP_CONTEXT_HEADER) String lraId) throws Exception {
        log.info("withdraw complete called for LRA : " + lraId);
        Journal journal = AccountTransferDAO.instance().getJournalForLRAid(lraId, WITHDRAW);
        if (journal != null) {
            journal.setLraState(AccountTransferDAO.getStatusString(ParticipantStatus.Completed));
        } else journal.setLraState(AccountTransferDAO.getStatusString(ParticipantStatus.FailedToComplete));
        AccountTransferDAO.instance().saveJournal(journal);
        return ResponseEntity.ok(ParticipantStatus.Completed.name());
    }

    /**
     * Read the journal and increase the balance by the previous withdraw amount before the LRA
     */
    @RequestMapping(value = "/compensate", method = RequestMethod.PUT)
    @Compensate
    public ResponseEntity<?> compensateWork(@RequestHeader(LRA_HTTP_CONTEXT_HEADER) String lraId) throws Exception {
        log.info("Account withdraw compensate() called for LRA : " + lraId);
        if (isUseLockFreeReservations) {
            try {
                Connection connection = entityManager.unwrap(Connection.class);
                log.info("microTxLockFreeReservation.join connection: " + connection);
                // https://docs.oracle.com/en/database/oracle/transaction-manager-for-microservices/24.2/tmmma/com/oracle/microtx/lra/lockfree/MicroTxLockFreeReservation.html
                // https://docs.oracle.com/en/database/oracle/transaction-manager-for-microservices/24.4/tmmdg/verify.html
                // https://www.youtube.com/watch?v=mBijkqoAibE
                microTxLockFreeReservation.compensate(connection);
            } catch (Exception e) {
                log.warning("Failed to retrieve the underlying connection: " + e.getMessage());
            }
        }
        Journal journal = AccountTransferDAO.instance().getJournalForLRAid(lraId, WITHDRAW);
        String lraState = journal.getLraState();
        if(lraState.equals(ParticipantStatus.Compensating) ||
                lraState.equals(ParticipantStatus.Compensated))
            return ResponseEntity.ok(AccountTransferDAO.getStatusFromString(lraState));
        journal.setLraState(AccountTransferDAO.getStatusString(ParticipantStatus.Compensating));
        AccountTransferDAO.instance().saveJournal(journal);
        doCompensationWork(journal);
        return ResponseEntity.ok(ParticipantStatus.Compensated.name());
    }

    @Transactional
    private void doCompensationWork(Journal journal) {
        Account account = AccountTransferDAO.instance().getAccountForAccountId(journal.getAccountId());
        if (account != null) {
            account.setAccountBalance(account.getAccountBalance() + journal.getJournalAmount());
            AccountTransferDAO.instance().saveAccount(account);
        } else {
            journal.setLraState(AccountTransferDAO.getStatusString(ParticipantStatus.FailedToCompensate));
        }
        journal.setLraState(AccountTransferDAO.getStatusString(ParticipantStatus.Compensated));
        AccountTransferDAO.instance().saveJournal(journal);
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET)
    @Status
    public ResponseEntity<?> status(@RequestHeader(LRA_HTTP_CONTEXT_HEADER) String lraId,
                           @RequestHeader(LRA_HTTP_PARENT_CONTEXT_HEADER) String parentLRA) throws Exception {
        return AccountTransferDAO.instance().status(lraId, WITHDRAW);
    }

    /**
     * Delete journal entry for LRA (or keep for auditing)
     */
    @RequestMapping(value = "/after", method = RequestMethod.PUT)
    @AfterLRA
    public ResponseEntity<?> afterLRA(@RequestHeader(LRA_HTTP_ENDED_CONTEXT_HEADER) String lraId, LRAStatus status) throws Exception {
        log.info("After LRA Called : " + lraId);
        AccountTransferDAO.instance().afterLRA(lraId, status, WITHDRAW);
        return ResponseEntity.ok().build();
    }

}
