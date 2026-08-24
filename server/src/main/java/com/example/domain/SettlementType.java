package com.example.domain;

/**
 * The size and status of a {@link Settlement}, used to distinguish a small hamlet
 * from a large city without storing free-form text.
 */
public enum SettlementType {
    HAMLET,
    VILLAGE,
    TOWN,
    CITY,
    FORTRESS,
    RUINS
}
