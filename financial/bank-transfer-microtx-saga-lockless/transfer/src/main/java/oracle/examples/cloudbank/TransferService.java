package oracle.examples.cloudbank;

import com.oracle.microtx.springboot.lra.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import static com.oracle.microtx.springboot.lra.annotation.LRA.LRA_HTTP_CONTEXT_HEADER;

@RestController
@RequestMapping("/")
public class TransferService {

    private static final Logger log = Logger.getLogger(TransferService.class.getSimpleName());
    public static final String TRANSFER_ID = "TRANSFER_ID";
    private static URI withdrawUri;
    private static URI depositUri;
    private static URI transferCancelUri;
    private static URI transferConfirmUri;
    private static URI transferProcessCancelUri;
    private static URI transferProcessConfirmUri;


   static {
        try {
            withdrawUri = new URI(ApplicationConfig.accountWithdrawUrl);
            depositUri = new URI(ApplicationConfig.accountDepositUrl);
            transferCancelUri = new URI(ApplicationConfig.transferCancelURL);
            transferConfirmUri = new URI(ApplicationConfig.transferConfirmURL);
            transferProcessCancelUri = new URI(ApplicationConfig.transferCancelProcessURL);
            transferProcessConfirmUri = new URI(ApplicationConfig.transferConfirmProcessURL);
        } catch (URISyntaxException ex) {
            throw new IllegalStateException("Failed to initialize " + TransferService.class.getName(), ex);
        }
    }


    @Autowired
    @Qualifier("MicroTxLRA")
    RestTemplate restTemplate;

    @RequestMapping(value = "/transfer", method = RequestMethod.POST)
    @LRA(value = LRA.Type.REQUIRES_NEW, end = false)
    public ResponseEntity<?> transfer(@RequestParam("fromAccount") long fromAccount,
                             @RequestParam("toAccount") long toAccount,
                             @RequestParam("amount") long amount,
                             @RequestParam("sagaAction") String sagaAction,
                             @RequestParam("crashSimulation") String crashSimulation,
                             @RequestParam("useLockFreeReservations") String useLockFreeReservations,
                             @RequestHeader( LRA_HTTP_CONTEXT_HEADER) String lraId) throws Exception
    {
        if (lraId == null) {
            return ResponseEntity.internalServerError().body("Failed to create LRA");
        }
        log.info("Started new LRA/transfer Id: " + lraId + " sagaAction:" + sagaAction +
                " crashSimulation:" + crashSimulation + " useLockFreeReservations:" + useLockFreeReservations);
        boolean isCompensate = false;
        String returnString = "";
        returnString += withdraw(new URI(lraId), fromAccount, amount, crashSimulation, useLockFreeReservations);
        log.info(returnString);
        if (returnString.contains("succeeded") && sagaAction.equalsIgnoreCase("complete") && crashSimulation.equals("noCrash")) {
            returnString += " " + deposit(new URI(lraId), toAccount, amount, crashSimulation, useLockFreeReservations);
            log.info(returnString);
            if (returnString.contains("failed")) isCompensate = true; //deposit failed
        } else isCompensate = true; //withdraw failed

        log.info("LRA/transfer action will be " + (isCompensate?"cancel":"close"));
        HttpHeaders headers = new HttpHeaders();
        headers.add("TRANSFER_ID", lraId);
        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
        URI completionUri = getTarget(isCompensate?transferCancelUri:transferConfirmUri)
                .build()
                .toUri();
        System.out.println("TransferService.transfer transferConfirmUri:" + transferConfirmUri.toASCIIString());
        restTemplate.postForEntity(completionUri, requestEntity, String.class);
        return ResponseEntity.ok("transfer status:" + returnString);
    }

    private String withdraw(URI lraId, long accountId, long amount, String crashSimulation, String useLockFreeReservations) {
        log.info("withdraw accountId = " + accountId + ", amount = " + amount);
        URI accountUri = getTarget(withdrawUri)
                .queryParam("accountId", accountId)
                .queryParam("amount", amount)
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.add("crashSimulation", crashSimulation);
        headers.add("isUseLockFreeReservations", useLockFreeReservations);
        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
        String withdrawOutcome = restTemplate.postForEntity(accountUri, requestEntity, String.class).getBody();
//        String withdrawOutcome = restTemplate.postForEntity(accountUri, null, String.class).getBody();
        log.info("withdraw lraId = " + lraId);
        return withdrawOutcome;
    }

    private String deposit(URI lraId, long accountId, long amount, String crashSimulation, String useLockFreeReservations) {
        log.info("deposit accountId = " + accountId + ", amount = " + amount);
        URI accountUri = getTarget(depositUri)
                .queryParam("accountId", accountId)
                .queryParam("amount", amount)
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.add("crashSimulation", crashSimulation);
        headers.add("isUseLockFreeReservations", useLockFreeReservations);
        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
        String depositOutcome = restTemplate.postForEntity(accountUri, requestEntity, String.class).getBody();
//        String depositOutcome = restTemplate.postForEntity(accountUri, null, String.class).getBody();
        log.info("deposit lraId = " + lraId);
        return depositOutcome;
    }


    @RequestMapping(value = "/processclose", method = RequestMethod.POST)
    @LRA(value = LRA.Type.MANDATORY)
    public ResponseEntity<?> processClose(@RequestHeader(LRA_HTTP_CONTEXT_HEADER) String lraId) {
        log.info("Process close for transfer : " + lraId);
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/processcancel", method = RequestMethod.POST)
    @LRA(value = LRA.Type.MANDATORY, cancelOn = HttpStatus.OK)
    public ResponseEntity<?> processCancel(@RequestHeader(LRA_HTTP_CONTEXT_HEADER) String lraId) {
        log.info("Process cancel for transfer : " + lraId);
        return ResponseEntity.ok("Process cancel for transfer : " + lraId);
    }


    // The following two methods could be in an external client.
    // They are included here for convenience.
    // The transfer method makes a Rest call to close or commit.
    // The close or commit method suspends the LRA (via NOT_SUPPORTED)
    // The close or commit method then proceeds to make a Rest call to the "processclose" or "processcommit" method
    // The "processclose" and "processcommit" methods import the LRA (via MANDATORY)
    //  and end the LRA implicitly accordingly upon return.
    @RequestMapping(value = "/close", method = RequestMethod.POST)
    @LRA(value = LRA.Type.NOT_SUPPORTED)
    public ResponseEntity<?> close(@RequestHeader(TRANSFER_ID) String transferId)  {
        log.info("Received close for transfer : " + transferId);

        HttpHeaders headers = new HttpHeaders();
        headers.add(LRA_HTTP_CONTEXT_HEADER, transferId);
        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
        URI transferUri = getTarget(transferProcessConfirmUri)
                .build()
                .toUri();
        String closeOutcome = restTemplate.postForEntity(transferUri, requestEntity, String.class).getBody();
        return ResponseEntity.ok(closeOutcome);
    }

    @RequestMapping(value = "/cancel", method = RequestMethod.POST)
    @LRA(value = LRA.Type.NOT_SUPPORTED, cancelOn = HttpStatus.OK)
    public ResponseEntity<?> cancel(@RequestHeader(TRANSFER_ID) String transferId) {
        log.info("Received cancel for transfer : " + transferId);
        HttpHeaders headers = new HttpHeaders();
        headers.add(LRA_HTTP_CONTEXT_HEADER, transferId);
        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
        URI transferUri = getTarget(transferProcessCancelUri)
                .build()
                .toUri();
        String closeOutcome = restTemplate.postForEntity(transferUri, requestEntity, String.class).getBody();
        return ResponseEntity.ok(closeOutcome);
    }

    private UriComponentsBuilder getTarget(URI serviceUri){
        return UriComponentsBuilder.fromUri(serviceUri);
    }

    @RequestMapping(value = "/afterLra", method = RequestMethod.PUT)
    @AfterLRA
    public ResponseEntity<?> afterLra(@RequestHeader(LRA_HTTP_CONTEXT_HEADER) String lraId)  {
        log.info("Received afterLra for transfer : " + lraId);
        return ResponseEntity.ok().build();
    }

}
