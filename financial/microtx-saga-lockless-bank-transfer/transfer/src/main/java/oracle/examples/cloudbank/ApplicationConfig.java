package oracle.examples.cloudbank;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    static String accountWithdrawUrl;
    static String accountDepositUrl;
    static String transferCancelURL;
    static String transferCancelProcessURL;
    static String transferConfirmURL;
    static String transferConfirmProcessURL;

    public ApplicationConfig(
                             @Value("${account.withdraw.url}") String accountWithdrawUrl,
                             @Value("${account.deposit.url}") String accountDepositUrl,
                             @Value("${transfer.cancel.url}") String transferCancelURL,
                             @Value("${transfer.cancel.process.url}") String transferCancelProcessURL,
                             @Value("${transfer.confirm.url}") String transferConfirmURL,
                             @Value("${transfer.confirm.process.url}") String transferConfirmProcessURL) {
        this.accountWithdrawUrl = accountWithdrawUrl;
        this.accountDepositUrl = accountDepositUrl;
        this.transferCancelURL = transferCancelURL;
        this.transferCancelProcessURL = transferCancelProcessURL;
        this.transferConfirmURL = transferConfirmURL;
        this.transferConfirmProcessURL = transferConfirmProcessURL;
    }
}
