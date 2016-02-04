package edu.umn.cs.recsys.ii;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.grouplens.lenskit.scored.ScoredId;
import org.grouplens.lenskit.vectors.MutableSparseVector;
import org.lenskit.basic.AbstractItemBasedItemScorer;
import org.lenskit.results.Results;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Global item scorer to find similar items.
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */

public class SimpleItemBasedItemScorer extends AbstractItemBasedItemScorer {
    private final SimpleItemItemModel model;

    @Inject
    public SimpleItemBasedItemScorer(SimpleItemItemModel mod) {
        model = mod;
    }

    /**
     * Score items with respect to a set of reference items.
     * @param basket The reference items.
     * @param items The score vector. Its domain is the items to be scored, and the scores should
     *               be stored into this vector.
     */
    @Override
    public ResultMap scoreRelatedItemsWithDetails(@Nonnull Collection<Long> basket, Collection<Long> items) {
        Long2DoubleOpenHashMap scores = new Long2DoubleOpenHashMap();

        // Store your scores for the target items in the scores object.
        for (Long item : items) {
            Long2DoubleMap neighborhoods = model.getNeighbors(item);
            Double score = 0.;
            for (Long referenced_item : basket) {
                score += neighborhoods.get(referenced_item);
            }
            scores.put(item, score);
        }
        
        List<Result> results = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : scores.entrySet()) {
            // Add each of the scores to the results list as a Result object
            // HINT: results.add(Results.create(item, score));
            results.add(Results.create(entry.getKey(), entry.getValue()));
        }
        return Results.newResultMap(results);

    }
}
