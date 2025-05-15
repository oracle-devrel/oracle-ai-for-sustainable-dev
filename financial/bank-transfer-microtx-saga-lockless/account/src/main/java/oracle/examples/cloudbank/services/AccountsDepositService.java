package oracle.examples.cloudbank.services;

import com.oracle.microtx.springboot.lra.annotation.*;
import com.oracle.microtx.springboot.lra.lockfree.MicroTxLockFreeReservation;
import oracle.examples.cloudbank.model.Account;
import oracle.examples.cloudbank.model.Journal;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

import static com.oracle.microtx.springboot.lra.annotation.LRA.*;

@RestController
@RequestMapping("/deposit")
public class AccountsDepositService {
    private static final Logger log = Logger.getLogger(AccountsDepositService.class.getName());

    private final static String DEPOSIT = "DEPOSIT";

    /**
     * Write journal entry re deposit amount.
     * Do not increase actual bank account amount
     */
    @RequestMapping(value = "/deposit", method = RequestMethod.POST)
    @LRA(value = LRA.Type.MANDATORY, end = false)
    @Transactional
    public ResponseEntity<?> deposit(@RequestHeader(LRA_HTTP_CONTEXT_HEADER) String lraId,
                                     @RequestParam("accountId") long accountId,
                                     @RequestParam("amount") long depositAmount) {
        log.info("...deposit " + depositAmount + " in account:" + accountId +
                " (lraId:" + lraId + ") finished (in pending state)");
        Account account = AccountTransferDAO.instance().getAccountForAccountId(accountId);
        if (account==null) {
            log.info("withdraw failed: account does not exist");
            AccountTransferDAO.instance().saveJournal(new Journal(DEPOSIT, accountId, 0, lraId,
                    AccountTransferDAO.getStatusString(ParticipantStatus.Active)));
            return ResponseEntity.ok("deposit failed: account does not exist");
        }
        AccountTransferDAO.instance().saveJournal(new Journal(DEPOSIT, accountId, depositAmount, lraId,
                AccountTransferDAO.getStatusString(ParticipantStatus.Active)));
        return ResponseEntity.ok("deposit succeeded");
    }

    /**
     * Increase balance amount as recorded in journal during deposit call.
     * Update LRA state to ParticipantStatus.Completed.
     */
    @RequestMapping(value = "/complete", method = RequestMethod.PUT)
    @Complete
    @Transactional
    public ResponseEntity<?> completeWork(@RequestHeader(LRA_HTTP_CONTEXT_HEADER) String lraId) throws Exception {
        log.info("deposit complete called for LRA : " + lraId);
        Journal journal = AccountTransferDAO.instance().getJournalForLRAid(lraId, DEPOSIT);
        String lraState = journal.getLraState();
        if(lraState.equals(ParticipantStatus.Compensating) ||
                lraState.equals(ParticipantStatus.Compensated))
            return ResponseEntity.ok(AccountTransferDAO.getStatusFromString(lraState));
        journal.setLraState(AccountTransferDAO.getStatusString(ParticipantStatus.Completing));
        AccountTransferDAO.instance().saveJournal(journal);
        doCompleteWork(journal);
        return ResponseEntity.ok(ParticipantStatus.Completed.name());
    }

    @Transactional
    private void doCompleteWork(Journal journal) throws Exception {
        Account account = AccountTransferDAO.instance().getAccountForAccountId(journal.getAccountId());
        if (account != null) {
            account.setAccountBalance(account.getAccountBalance() + journal.getJournalAmount());
            AccountTransferDAO.instance().saveAccount(account);
        } else {
            journal.setLraState(AccountTransferDAO.getStatusString(ParticipantStatus.FailedToComplete));
        }
        journal.setLraState(AccountTransferDAO.getStatusString(ParticipantStatus.Completed));
        AccountTransferDAO.instance().saveJournal(journal);
    }

    /**
     * Update LRA state to ParticipantStatus.Compensated.
     */
    @RequestMapping(value = "/compensate", method = RequestMethod.PUT)
    @Compensate
    public ResponseEntity<?> compensateWork(@RequestHeader(LRA_HTTP_CONTEXT_HEADER) String lraId) throws Exception {
        log.info("deposit compensate called for LRA : " + lraId);
        Journal journal = AccountTransferDAO.instance().getJournalForLRAid(lraId, DEPOSIT);
        journal.setLraState(AccountTransferDAO.getStatusString(ParticipantStatus.Compensated));
        AccountTransferDAO.instance().saveJournal(journal);
        return ResponseEntity.ok(ParticipantStatus.Compensated.name());
    }

    /**
     * Return status
     */
    @RequestMapping(value = "/status", method = RequestMethod.GET)
    @Status
    public ResponseEntity<?> status(@RequestHeader(LRA_HTTP_CONTEXT_HEADER) String lraId,
                           @RequestHeader(LRA_HTTP_PARENT_CONTEXT_HEADER) String parentLRA) throws Exception {
        return AccountTransferDAO.instance().status(lraId, DEPOSIT);
    }

    /**
     * Delete journal entry for LRA (or keep for auditing)
     */
    @RequestMapping(value = "/after", method = RequestMethod.PUT)
    @AfterLRA
    public ResponseEntity<?> afterLRA(@RequestHeader(LRA_HTTP_ENDED_CONTEXT_HEADER) String lraId, LRAStatus status) throws Exception {
        log.info("After LRA Called : " + lraId);
        AccountTransferDAO.instance().afterLRA(lraId, status, DEPOSIT);
        return ResponseEntity.ok().build();
    }

}
