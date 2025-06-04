package oracle.examples.cloudbank.repository;

import oracle.examples.cloudbank.model.Journal;
import org.springframework.data.jpa.repository.JpaRepository;


public interface JournalRepository extends JpaRepository<Journal, Long> {
    Journal findJournalByLraIdAndJournalType(String lraId, String journalType);
}
