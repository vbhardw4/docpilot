package dev.vishalbhardwaj.docpilot.chat;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatInteractionRepository extends JpaRepository<ChatInteraction, Long> {

    long countByAnswered(boolean answered);

    @Query("select avg(c.latencyMs) from ChatInteraction c")
    Double averageLatencyMs();

    @Query("select c.question as question, count(c) as interactions from ChatInteraction c " +
            "group by c.question order by count(c) desc")
    List<TopQuestion> findTopQuestions(Pageable pageable);

    long countByHelpful(Boolean helpful);

    interface TopQuestion {
        String getQuestion();

        long getInteractions();
    }
}
