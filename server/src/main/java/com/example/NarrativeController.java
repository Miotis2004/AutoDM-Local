package com.example;

import com.example.domain.DiceRollResult;
import com.example.domain.NarrativeCategory;
import com.example.domain.NarrativeContext;
import com.example.domain.NarrativeEntry;
import com.example.domain.NarrativeRenderRequest;
import com.example.service.DiceService;
import com.example.service.NarrativeTemplates;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST surface for narrative templates.
 *
 * <p>Every endpoint here is thin: it maps an HTTP request onto a single {@link
 * NarrativeTemplates} call. Template registration and rendering all live in the service, so the
 * surface never owns any of the narrative rules.</p>
 *
 * <p>The {@code /api/narrative/dice} endpoint is the most direct: it rolls the dice on the
 * back-end through {@link DiceService} and immediately renders the result as a {@link
 * NarrativeCategory#DICE_RESULT} entry. The {@code /api/narrative/render} endpoint is the general
 * one: a client sends a {@link NarrativeCategory} plus the free-form structured data for a
 * moment, and the service renders it through the template registered for that category. Both
 * return a {@link NarrativeEntry}, the structured line the frontend game log consumes.</p>
 */
@RestController
@RequestMapping("/api/narrative")
public class NarrativeController {

    private final NarrativeTemplates templates;
    private final DiceService dice;

    public NarrativeController(NarrativeTemplates templates, DiceService dice) {
        this.templates = templates;
        this.dice = dice;
    }

    /**
     * @return the categories that currently have a template, in registration order
     */
    @GetMapping("/categories")
    public List<NarrativeCategory> categories() {
        return templates.categories();
    }

    /**
     * Rolls the requested dice on the back-end and renders the roll as a {@link
     * NarrativeCategory#DICE_RESULT} entry.
     *
     * <p>Pass one {@code sides} per die to roll (repeatable), for example
     * {@code POST /api/narrative/dice?sides=20&sides=20&modifier=+3}.</p>
     *
     * @param sides    the number of faces for each die to roll (repeatable; at least one required)
     * @param modifier the value added to the sum of the dice (may be negative; defaults to 0)
     * @return the dice-result entry
     */
    @PostMapping("/dice")
    public NarrativeEntry dice(
            @RequestParam("sides") List<Integer> sides,
            @RequestParam(defaultValue = "0") int modifier) {
        DiceRollResult roll = dice.roll(sides, modifier);
        return templates.render(
                NarrativeCategory.DICE_RESULT,
                NarrativeContext.forRoll(roll));
    }

    /**
     * Renders a single narrative entry for a category from free-form structured data.
     *
     * <p>The client sends a {@link NarrativeCategory} plus a data map describing the moment - a
     * dice roll, an attack, a campaign event, and so on - and the service renders it through the
     * template registered for that category. To render a whole context at once, send the data and
     * let the caller iterate categories.</p>
     *
     * @param request the category and structured data to render
     * @return the rendered entry
     */
    @PostMapping("/render")
    public NarrativeEntry render(@RequestBody NarrativeRenderRequest request) {
        Map<String, Object> data = request.data() == null ? Map.of() : request.data();
        NarrativeContext context = NarrativeContext.fromCategoryData(request.category(), data);
        return templates.render(request.category(), context);
    }
}
