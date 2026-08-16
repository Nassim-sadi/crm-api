package com.nassim.crm_api.Comment;

import com.nassim.crm_api.Ticket.Ticket;
import com.nassim.crm_api.Ticket.TicketRepository;
import com.nassim.crm_api.exception.TicketNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private CommentService commentService;

    @Test
    void createComment_savesCommentTiedToTicket() {
        Ticket ticket = new Ticket();
        ticket.setId(10L);
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentResponse result = commentService.createComment(10L, new CommentRequest("Let me check the printer"));

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getText()).isEqualTo("Let me check the printer");
        assertThat(captor.getValue().getTicket().getId()).isEqualTo(10L);
        assertThat(result.ticketId()).isEqualTo(10L);
    }

    @Test
    void createComment_throwsWhenTicketNotFound() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.createComment(99L, new CommentRequest("hello")))
                .isInstanceOf(TicketNotFoundException.class);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void getCommentsByTicketId_returnsOrderedComments() {
        Ticket ticket = new Ticket();
        ticket.setId(10L);
        Comment comment = new Comment();
        comment.setId(5L);
        comment.setText("Let me check the printer");
        comment.setTicket(ticket);

        when(ticketRepository.existsById(10L)).thenReturn(true);
        when(commentRepository.findByTicketIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(comment));

        List<CommentResponse> comments = commentService.getCommentsByTicketId(10L);

        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).id()).isEqualTo(5L);
        assertThat(comments.get(0).ticketId()).isEqualTo(10L);
        verify(commentRepository).findByTicketIdOrderByCreatedAtAsc(10L);
    }

    @Test
    void getCommentsByTicketId_throwsWhenTicketNotFound() {
        when(ticketRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> commentService.getCommentsByTicketId(99L))
                .isInstanceOf(TicketNotFoundException.class);
        verify(commentRepository, never()).findByTicketIdOrderByCreatedAtAsc(any());
    }
}
