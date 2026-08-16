package com.nassim.crm_api.Comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "text is required")
        @Size(max = 1000, message = "text must be at most 1000 characters")
        String text
) {
}
