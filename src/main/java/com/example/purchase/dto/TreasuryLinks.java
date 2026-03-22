package com.example.purchase.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pagination links for Treasury API response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TreasuryLinks {
    private String self;
    private String first;
    private String prev;
    private String next;
    private String last;
}
