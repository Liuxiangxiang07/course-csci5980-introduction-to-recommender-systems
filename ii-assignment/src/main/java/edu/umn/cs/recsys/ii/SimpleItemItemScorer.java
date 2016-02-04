package edu.umn.cs.recsys.ii;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.grouplens.lenskit.scored.ScoredId;
import org.grouplens.lenskit.util.ScoredItemAccumulator;
import org.grouplens.lenskit.util.TopNScoredItemAccumulator;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.data.dao.UserEventDAO;
import org.lenskit.data.ratings.Rating;
import org.lenskit.data.history.History;
import org.lenskit.data.history.UserHistory;
import org.lenskit.knn.NeighborhoodSize;
import org.lenskit.results.Results;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SimpleItemItemScorer extends AbstractItemScorer {
    private final SimpleItemItemModel model;
    private final UserEventDAO userEvents;
    private final int neighborhoodSize;

    @Inject
    public SimpleItemItemScorer(SimpleItemItemModel m, UserEventDAO dao,
                                @NeighborhoodSize int nnbrs) {
        model = m;
        userEvents = dao;
        neighborhoodSize = nnbrs;
    }

    /**
     * Score items for a user.
     * @param user The user ID.
     * @param items The score vector.  Its key domain is the items to score, and the scores
     *               (rating predictions) should be written back to this vector.
     */
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
        Long2DoubleMap itemMeans = model.getItemMeans();
        Long2DoubleMap ratings = getUserRatingVector(user);
        
        // Normalize the user's ratings by subtracting the item mean.
        for (Map.Entry<Long, Double> e : ratings.entrySet()) {
            e.setValue(e.getValue() - itemMeans.get(e.getKey()));
        }

        Long2DoubleMap scores = new Long2DoubleOpenHashMap();

        for (Long item: items) {
            Long2DoubleMap allNeighbors = model.getNeighbors(item);
            
            // Score this item and save the score into scores.
            // HINT: You might want to use the TopNScoredItemAccumulator class.
            TopNScoredItemAccumulator topNScoredItemAccumulator = new TopNScoredItemAccumulator(neighborhoodSize);
            for (Map.Entry<Long, Double> e : allNeighbors.entrySet()) {
                if (ratings.containsKey(e.getKey())) {
                    topNScoredItemAccumulator.put(e.getKey(), e.getValue());
                }
            }
            Double score = 0.;
            double denom = 0;
            for (ScoredId neighbor : topNScoredItemAccumulator.finish()) {
                score += ratings.get(neighbor.getId()) * neighbor.getScore();
                denom += Math.abs(neighbor.getScore());
            }
            score /= denom;
            score += itemMeans.get(item);
            scores.put(item, score);
        }

        List<Result> results = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : scores.entrySet()) {
            // Add each of the scores to the results list as a Result object
            // HINT: Don't forget to add the item mean back into the score.
            // HINT: results.add(Results.create(item, score));
            results.add(Results.create(entry.getKey(), entry.getValue()));
        }
        return Results.newResultMap(results);

    }

    /**
     * Get a user's ratings.
     * @param user The user ID.
     * @return The ratings to retrieve.
     */
    private Long2DoubleOpenHashMap getUserRatingVector(long user) {
        UserHistory<Rating> history = userEvents.getEventsForUser(user, Rating.class);
        if (history == null) {
            history = History.forUser(user);
        }

        Long2DoubleOpenHashMap ratings = new Long2DoubleOpenHashMap();
        for (Rating r: history) {
            if (r.hasValue()) {
                ratings.put(r.getItemId(), r.getValue());
            }
        }

        return ratings;
    }


}
