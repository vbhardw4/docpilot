package dev.vishalbhardwaj.docpilot.ticket;

import dev.vishalbhardwaj.docpilot.ticket.dto.CreateTicketRequest;
import dev.vishalbhardwaj.docpilot.ticket.dto.SupportTicketDto;
import dev.vishalbhardwaj.docpilot.ticket.dto.UpdateTicketStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /** Open a ticket — what the widget offers when the bot can't answer from the docs. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupportTicketDto create(@Valid @RequestBody CreateTicketRequest request) {
        return SupportTicketDto.from(ticketService.create(request));
    }

    @GetMapping
    public List<SupportTicketDto> list(@RequestParam(required = false) TicketStatus status) {
        return ticketService.list(status).stream().map(SupportTicketDto::from).toList();
    }

    @PatchMapping("/{id}/status")
    public SupportTicketDto updateStatus(@PathVariable UUID id,
                                         @Valid @RequestBody UpdateTicketStatusRequest request) {
        return SupportTicketDto.from(ticketService.updateStatus(id, request.status()));
    }
}
