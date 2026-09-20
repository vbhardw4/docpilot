package dev.vishalbhardwaj.docpilot.chat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Per-session conversation memory: stores turns, serves the recent window as LLM context. */
@Service
public class ConversationService {

    private final ConversationMessageRepository repository;

    public ConversationService(ConversationMessageRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ConversationMessage> recent(String sessionId, int limit) {
        List<ConversationMessage> all = repository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return all.size() <= limit ? all : all.subList(all.size() - limit, all.size());
    }

    @Transactional(readOnly = true)
    public List<ConversationMessage> history(String sessionId) {
        return repository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    @Transactional
    public void saveTurn(String sessionId, String question, String answer) {
        repository.save(new ConversationMessage(sessionId, MessageRole.USER, question));
        repository.save(new ConversationMessage(sessionId, MessageRole.ASSISTANT, answer));
    }
}
