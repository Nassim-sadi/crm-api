package com.nassim.crm_api.Comment;

import com.nassim.crm_api.Ticket.Ticket;
import com.nassim.crm_api.Ticket.TicketRepository;
import com.nassim.crm_api.exception.TicketNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;

    public CommentService(CommentRepository commentRepository, TicketRepository ticketRepository) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
    }

    public List<CommentResponse> getCommentsByTicketId(Long ticketId) {
        ensureTicketExists(ticketId);
        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(this::toResponse)
                .toList();
    }

    public CommentResponse createComment(Long ticketId, CommentRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        Comment comment = new Comment();
        comment.setText(request.text());
        comment.setTicket(ticket);
        return toResponse(commentRepository.save(comment));
    }

    private void ensureTicketExists(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new TicketNotFoundException(ticketId);
        }
    }

    private CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getText(),
                comment.getCreatedAt(),
                comment.getTicket().getId()
        );
    }
}
