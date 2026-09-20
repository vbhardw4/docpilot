package dev.vishalbhardwaj.docpilot.analytics.dto;

import java.util.List;

public record AnalyticsResponse(long totalQueries, long answeredFromDocs, long escalated,
                                double escalationRate, long avgLatencyMs,
                                long positiveFeedback, long negativeFeedback,
                                List<TopQuestionDto> topQuestions) {

    public record TopQuestionDto(String question, long count) {}
}
