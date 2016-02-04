package edu.umn.cs.recsys.svd;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;
import org.lenskit.api.ItemScorer;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.baseline.BaselineScorer;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.data.dao.UserEventDAO;
import org.lenskit.data.ratings.Rating;
import org.lenskit.data.history.History;
import org.lenskit.data.history.UserHistory;
import org.lenskit.results.Results;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * SVD-based item scorer.
 */
public class SVDItemScorer extends AbstractItemScorer {
    private static final Logger logger = LoggerFactory.getLogger(SVDItemScorer.class);
    private final SVDModel model;
    private final ItemScorer baselineScorer;
    private final UserEventDAO userEvents;

    /**
     * Construct an SVD item scorer using a model.
     * @param m The model to use when generating scores.
     * @param uedao A DAO to get user rating profiles.
     * @param baseline The baseline scorer (providing means).
     */
    @Inject
    public SVDItemScorer(SVDModel m, UserEventDAO uedao,
                         @BaselineScorer ItemScorer baseline) {
        model = m;
        baselineScorer = baseline;
        userEvents = uedao;
    }

    /**
     * Score items in a vector. The key domain of the provided vector is the
     * items to score, and the score method sets the values for each item to
     * its score (or unsets it, if no score can be provided). The previous
     * values are discarded.
     *
     * @param user   The user ID.
     * @param items The items to score
     */
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
        RealVector userFeatures = model.getUserVector(user);
        if (userFeatures == null) {
            logger.debug("unknown user {}", user);
            return Results.newResultMap();
        }

        // TODO rescale the user features into this vector
        // the ebeMultiply method will help you
        RealVector nfeats;

        // TODO Compute the predictions
        

        // Start with the baseline score for each user
        Long2DoubleMap scores = new Long2DoubleOpenHashMap(baselineScorer.score(user, items));

        // add predicted difference to baseline
        for (Map.Entry<Long, Double> e: scores.entrySet()) {
            long item = e.getKey();
            int row = model.getItemRow(item);
            if (row >= 0) {
                // TODO Set the scores
                // Since score vector has baselines, *add* the score to each value
            }
        }

        // Construct ResultMap to return
        List<Result> results = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : scores.entrySet()) {
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
