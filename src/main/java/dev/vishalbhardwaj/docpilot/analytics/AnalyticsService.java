package dev.vishalbhardwaj.docpilot.analytics;

import dev.vishalbhardwaj.docpilot.analytics.dto.AnalyticsResponse;
import dev.vishalbhardwaj.docpilot.analytics.dto.AnalyticsResponse.TopQuestionDto;
import dev.vishalbhardwaj.docpilot.chat.ChatInteractionRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Admin analytics: how many questions the bot answers from docs vs. escalates,
 * latency, feedback, and the questions buyers ask most (the "top questions" view
 * that demos well — it shows where the docs have gaps).
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final ChatInteractionRepository interactions;

    public AnalyticsService(ChatInteractionRepository interactions) {
        this.interactions = interactions;
    }

    public AnalyticsResponse snapshot() {
        long total = interactions.count();
        long answered = interactions.countByAnswered(true);
        long escalated = total - answered;
        Double avgLatency = interactions.averageLatencyMs();
        List<TopQuestionDto> topQuestions = interactions.findTopQuestions(Pageable.ofSize(5)).stream()
                .map(t -> new TopQuestionDto(t.getQuestion(), t.getInteractions()))
                .toList();
        return new AnalyticsResponse(
                total,
                answered,
                escalated,
                total == 0 ? 0.0 : (double) escalated / total,
                avgLatency == null ? 0 : avgLatency.longValue(),
                interactions.countByHelpful(true),
                interactions.countByHelpful(false),
                topQuestions);
    }
}
