package com.nassim.crm_api.Comment;

import com.nassim.crm_api.config.TestSecurityConfig;
import com.nassim.crm_api.exception.TicketNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResultAssert;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(CommentController.class)
@Import(TestSecurityConfig.class)
class CommentControllerTest {

    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private CommentService commentService;

    private CommentResponse commentResponse() {
        return new CommentResponse(5L, "Let me check the printer", Instant.parse("2026-01-01T10:00:00Z"), 10L);
    }

    @Test
    void findAll_returnsComments() {
        when(commentService.getCommentsByTicketId(10L)).thenReturn(List.of(commentResponse()));

        MvcTestResultAssert result = mockMvc.get().uri("/api/tickets/10/comments").exchange().assertThat();

        result.hasStatusOk();
        result.bodyJson().extractingPath("$[0].text").asString().isEqualTo("Let me check the printer");
        result.bodyJson().extractingPath("$[0].ticketId").asNumber().isEqualTo(10);
    }

    @Test
    void create_returns201WithLocation() {
        when(commentService.createComment(any(), any(CommentRequest.class))).thenReturn(commentResponse());

        MvcTestResultAssert result = mockMvc.post().uri("/api/tickets/10/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\": \"Let me check the printer\"}")
                .exchange().assertThat();

        result.hasStatus(201);
        result.headers().hasHeaderSatisfying("Location",
                values -> assertThat(values).anyMatch(v -> v.endsWith("/api/tickets/10/comments/5")));
        result.bodyJson().extractingPath("$.id").asNumber().isEqualTo(5);
    }

    @Test
    void create_returns400WhenTextBlank() {
        MvcTestResultAssert result = mockMvc.post().uri("/api/tickets/10/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\": \"\"}")
                .exchange().assertThat();

        result.hasStatus(400);
        result.bodyJson().extractingPath("$.errors.text").asString().isEqualTo("text is required");
    }

    @Test
    void create_returns404WhenTicketNotFound() {
        when(commentService.createComment(any(), any(CommentRequest.class)))
                .thenThrow(new TicketNotFoundException(99L));

        MvcTestResultAssert result = mockMvc.post().uri("/api/tickets/99/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\": \"hello\"}")
                .exchange().assertThat();

        result.hasStatus(404);
        result.bodyJson().extractingPath("$.message").asString().isEqualTo("Ticket not found with id: 99");
    }
}
