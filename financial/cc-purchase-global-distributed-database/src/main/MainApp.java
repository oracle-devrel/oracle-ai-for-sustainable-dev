import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import java.math.BigDecimal;
public class MainApp {
    public static void main(String[] args) {
        SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
        // Insert a new transaction record
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            TransactionEntity transactionEntity = new TransactionEntity("REQ123", new BigDecimal("1000.00"), "PENDING");
            session.persist(transactionEntity);
            tx.commit();
            System.out.println("Transaction record saved successfully!");
        }
        // Retrieve the transaction record
        try (Session session = sessionFactory.openSession()) {
            TransactionEntity txEntity = session.get(TransactionEntity.class, "REQ123");
            if (txEntity != null) {
                System.out.println("Request ID: " + txEntity.getRequestId() +
                                   ", Amount: " + txEntity.getAmount() +
                                   ", Status: " + txEntity.getStatus());
            }
        }
        sessionFactory.close();
    }
}









