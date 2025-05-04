import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity
@Table(name = "TRANSACTION")
public class TransactionEntity {
    @Id
    @Column(name = "REQUEST_ID", nullable = false, unique = true)
    private String requestId;
    @Column(name = "AMOUNT", precision = 19, scale = 4)
    private BigDecimal amount;
    @Column(name = "STATUS")
    private String status;
    // Constructors
    public TransactionEntity() {
    }
    public TransactionEntity(String requestId, BigDecimal amount, String status) {
        this.requestId = requestId;
        this.amount = amount;
        this.status = status;
    }
    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}










