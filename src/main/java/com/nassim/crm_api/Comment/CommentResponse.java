package com.nassim.crm_api.Comment;

import java.time.Instant;

public record CommentResponse(Long id, String text, Instant createdAt, Long ticketId) {
}
