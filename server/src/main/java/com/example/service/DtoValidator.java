package com.example.service;

import org.springframework.stereotype.Service;

/**
 * Centralized DTO validation for the back-end.
 *
 * <p>Every service and controller that accepts caller-supplied data runs the
 * caller's data through this validator before it reaches the persistence layer.
 * It owns the structural rules that make a request unsound on its own, so the
 * checks are written once and applied consistently:
 *
 * <ul>
 *   <li><strong>Invalid identifiers</strong> are rejected. A persisted entity is
 *   always assigned a positive, auto-generated id, so a {@code null}, zero, or
 *   negative identifier can never refer to a real row and is refused up front.</li>
 *   <li><strong>Impossible quantities</strong> are rejected. A count of items,
 *   progress points, population, travel time, and so on can never be negative; a
 *   quantity that must describe at least one thing (an item added, a transfer)
 *   cannot be zero either.</li>
 *   <li><strong>Missing field values</strong> such as a required name or title
 *   that is blank are rejected.</li>
 * </ul>
 *
 * <p>This validator is deliberately structural: it checks the shape of the data,
 * not what the data refers to. Referential integrity — that a referenced id
 * actually belongs to the owning campaign — stays where it can inspect the
 * repositories, in the services' {@code requireX} lookups. Together the two form
 * the complete "DTO validation rejects invalid identifiers and invalid entity
 * references" guarantee. Game-action shape (a recognized type, a living target,
 * a named skill) is validated separately by {@link ActionValidator}.</p>
 *
 * <p>The validator holds no state and no dependencies; it is a pure set of checks
 * that any bean may inject.</p>
 */
@Service
public class DtoValidator {

    /**
     * Accepts a positive, auto-generated entity identifier, rejecting anything
     * that could not refer to a real row.
     *
     * @param entity the entity the identifier names, used in the error message
     * @param id     the identifier to accept
     * @throws ValidationException when {@code id} is {@code null}, zero, or negative
     */
    public void requirePositiveIdentifier(String entity, Long id) {
        if (id == null) {
            throw new ValidationException("A " + entity + " identifier is required.");
        }
        if (id <= 0) {
            throw new ValidationException(
                    "A " + entity + " identifier must be a positive number, was " + id + ".");
        }
    }

    /**
     * Accepts a quantity that must describe at least one thing. Used for counts
     * that are meaningless at zero, such as items added to a holding or items
     * transferred between owners.
     *
     * @param context a short description of the quantity, used in the error message
     * @param quantity the quantity to accept
     * @throws ValidationException when {@code quantity} is less than one
     */
    public void requirePositiveQuantity(String context, int quantity) {
        if (quantity < 1) {
            throw new ValidationException(context + " must be at least 1, was " + quantity + ".");
        }
    }

    /**
     * Accepts a quantity that may legitimately be zero but can never be negative.
     * Used for counts such as population, travel time, or a raw progress total.
     *
     * <p>A negative value is impossible: a population, a travel time, or a count
     * of progress points can never drop below zero.</p>
     *
     * @param context a short description of the quantity, used in the error message
     * @param quantity the quantity to accept
     * @throws ValidationException when {@code quantity} is negative
     */
    public void requireNonNegativeQuantity(String context, int quantity) {
        if (quantity < 0) {
            throw new ValidationException(context + " must not be negative, was " + quantity + ".");
        }
    }

    /**
     * Accepts a non-negative numeric value that may be supplied as a {@link Double}.
     *
     * @param context a short description of the value, used in the error message
     * @param value    the value to accept
     * @throws ValidationException when {@code value} is negative
     */
    public void requireNonNegativeQuantity(String context, Double value) {
        if (value != null && value < 0) {
            throw new ValidationException(context + " must not be negative, was " + value + ".");
        }
    }

    /**
     * Accepts a field value that must be present, treating {@code null} and
     * blank strings as absent.
     *
     * @param field the name of the field, used in the error message
     * @param value the value to accept
     * @throws ValidationException when {@code value} is {@code null} or blank
     */
    public void requireNonBlank(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " must not be blank.");
        }
    }
}
