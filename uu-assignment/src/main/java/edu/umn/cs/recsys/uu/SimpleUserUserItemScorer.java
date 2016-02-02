package edu.umn.cs.recsys.uu;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.data.dao.ItemEventDAO;
import org.lenskit.data.dao.UserEventDAO;
import org.lenskit.data.ratings.Rating;
import org.lenskit.data.history.History;
import org.lenskit.data.history.UserHistory;
import org.lenskit.results.Results;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User-user item scorer.
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SimpleUserUserItemScorer extends AbstractItemScorer {
    private final UserEventDAO userDao;
    private final ItemEventDAO itemDao;

    @Inject
    public SimpleUserUserItemScorer(UserEventDAO udao, ItemEventDAO idao) {
        userDao = udao;
        itemDao = idao;
    }

    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
        Long2DoubleMap userVector = getUserRatingVector(user);

        // This is a list where you can store the results of your user-user computations
        List<Result> results = new ArrayList<>();

        // TODO Score items for this user using user-user collaborative filtering

        // This is the loop structure to iterate over the items to score.
        for (Long item: items) {

            // HINT You can add Result objects to the results List using the following line:
            // results.add(Results.create(item, score));

        }

        return Results.newResultMap(results);
    }

    /**
     * Get a user's rating vector.
     * @param user The user ID.
     * @return The rating vector.
     */
    private Long2DoubleMap getUserRatingVector(long user) {
        UserHistory<Rating> history = userDao.getEventsForUser(user, Rating.class);
        if (history == null) {
            history = History.forUser(user);
        }

        Long2DoubleMap ratings = new Long2DoubleOpenHashMap();
        for (Rating r: history) {
            if (r.hasValue()) {
                ratings.put(r.getItemId(), r.getValue());
            }
        }

        return ratings;
    }

}
