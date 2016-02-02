package edu.umn.cs.recsys.uu;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.grouplens.lenskit.vectors.MutableSparseVector;
import org.grouplens.lenskit.vectors.similarity.CosineVectorSimilarity;
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
import java.util.*;

/**
 * User-user item scorer.
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SimpleUserUserItemScorer extends AbstractItemScorer {
    private final UserEventDAO userDao;
    private final ItemEventDAO itemDao;

    private static final double NEIGHBOR_SIZE = 30;

    @Inject
    public SimpleUserUserItemScorer(UserEventDAO udao, ItemEventDAO idao) {
        userDao = udao;
        itemDao = idao;
    }

    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
        Long2DoubleMap userVector = getUserRatingVector(user);
        MutableSparseVector userVectorSv = MutableSparseVector.create(userVector);
        double userMean = userVectorSv.mean();

        // This is a list where you can store the results of your user-user computations
        List<Result> results = new ArrayList<>();

        // Score items for this user using user-user collaborative filtering

        // This is the loop structure to iterate over the items to score.
        for (Long item: items) {
            double score = 0;
            Long2DoubleMap userSimilarities = getUserSimilarities(userVectorSv, item);
            List<UserSimilarity> neighbors = getNeighbors(userSimilarities, user);

            double sum = 0;
            for (UserSimilarity userSimilarity : neighbors) {
                MutableSparseVector sv = getMeanCentered(getUserRatingVector(userSimilarity.user));
                score += userSimilarity.similarity * sv.get(item);
                sum += Math.abs(userSimilarity.similarity);
            }
            score = userMean + score / sum;

            // HINT You can add Result objects to the results List using the following line:
            // results.add(Results.create(item, score));
            results.add(Results.create(item, score));
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

    /**
     * Compute vector of similarities for the input user's ratings vector.
     * In this vector for each element the key is the user and the value is the similarity.
     * Similarity implemented as a cosine similarity between mean-centered ratings vector.
     * @param userVector vector of ratings for input user.
     * @return vector of similarities.
     */
    private Long2DoubleMap getUserSimilarities(MutableSparseVector userVector, Long item) {
        Long2DoubleMap result = new Long2DoubleOpenHashMap();
        for (Long user: itemDao.getUsersForItem(item)) {
            MutableSparseVector sv = getMeanCentered(getUserRatingVector(user));
            CosineVectorSimilarity cosineVectorSimilarity = new CosineVectorSimilarity();
            Double sim = cosineVectorSimilarity.similarity(userVector, sv);
            result.put(user, sim);
        }
        return result;
    }

    /**
     * Create a new vector where the mean of the vector is subtracted from each element.
     * @param vec input vector.
     * @return mean-centered vector.
     */
    private MutableSparseVector getMeanCentered(Long2DoubleMap userVector) {
        MutableSparseVector userVectorSv = MutableSparseVector.create(userVector);
        double userMean = userVectorSv.mean();
        for (Map.Entry<Long, Double> e: userVector.entrySet()) {
            userVectorSv.set((long)e.getKey(), e.getValue() - userMean);
        }
        return userVectorSv;
    }

    class UserSimilarity {
        public long user;
        public double similarity;

        public UserSimilarity(long user, double similarity) {
            this.user = user;
            this.similarity = similarity;
        }

        @Override
        public String toString() {
            return String.format("%d: %.4f", user, similarity);
        }
    }

    /**
     * Retrieve the nearest neighbors who have rated the item.
     * @param userSimilarities vector of similarities between the input user
     * and all other users.
     * @param user the input user ID.
     * @return list of UserSimilarity objects (a pair of user ID and similarity value).
     */
    private List<UserSimilarity> getNeighbors(Long2DoubleMap userSimilarities, Long user) {
        List<UserSimilarity> allResult = new ArrayList<>();

        for (Map.Entry<Long, Double> e : userSimilarities.entrySet()) {
            UserSimilarity userSimilarity = new UserSimilarity(e.getKey(), e.getValue());
            allResult.add(userSimilarity);
        }

        Collections.sort(allResult, new Comparator<UserSimilarity>() {
            @Override
            public int compare(UserSimilarity o1, UserSimilarity o2) {
                return (int)Math.signum(o2.similarity - o1.similarity);
            }
        });

        List<UserSimilarity> result = new ArrayList<>();
        for (UserSimilarity userSimilarity : allResult) {
            if (result.size() >= NEIGHBOR_SIZE) {
                break;
            }
            if (userSimilarity.user == user) {
                continue;
            }
            result.add(userSimilarity);
        }

        return result;
    }
}
