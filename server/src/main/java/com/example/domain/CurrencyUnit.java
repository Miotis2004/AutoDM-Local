package com.example.domain;

/**
 * The standard coin denominations used for a campaign's shared treasury.
 *
 * <p>Stored as a string in {@link CurrencyRecord} so that the value survives
 * round-trips through the persistence layer while still carrying an explicit,
 * typed vocabulary of the coins a campaign may use.</p>
 */
public enum CurrencyUnit {
    CP,
    SP,
    EP,
    GP,
    PP
}
