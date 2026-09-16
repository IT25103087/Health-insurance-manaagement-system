package com.healthinsurance.claimsreview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReviewNoteDto {

    @NotBlank(message = "Updated notes cannot be empty")
    private String reviewerNotes;
}
