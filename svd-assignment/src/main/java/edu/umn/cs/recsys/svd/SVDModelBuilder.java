package edu.umn.cs.recsys.svd;

import it.unimi.dsi.fastutil.longs.*;
import org.apache.commons.math3.linear.*;
import org.lenskit.api.ItemScorer;
import org.lenskit.baseline.BaselineScorer;
import org.lenskit.inject.Transient;
import org.lenskit.util.io.ObjectStream;
import org.lenskit.data.dao.ItemDAO;
import org.lenskit.data.dao.UserDAO;
import org.lenskit.data.dao.UserEventDAO;
import org.lenskit.data.events.Event;
import org.lenskit.data.ratings.Rating;
import org.lenskit.data.ratings.Ratings;
import org.lenskit.data.history.UserHistory;
import org.grouplens.lenskit.vectors.MutableSparseVector;
import org.grouplens.lenskit.vectors.VectorEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

/**
 * Model builder that computes the SVD model.
 */
public class SVDModelBuilder implements Provider<SVDModel> {
    private static final Logger logger = LoggerFactory.getLogger(SVDModelBuilder.class);

    private final UserEventDAO userEventDAO;
    private final UserDAO userDAO;
    private final ItemDAO itemDAO;
    private final ItemScorer baselineScorer;
    private final int featureCount;

    /**
     * Construct the model builder.
     * @param uedao The user event DAO.
     * @param udao The user DAO.
     * @param idao The item DAO.
     * @param baseline The baseline scorer (this will be used to compute means).
     * @param nfeatures The number of latent features to train.
     */
    @Inject
    public SVDModelBuilder(@Transient UserEventDAO uedao,
                           @Transient UserDAO udao,
                           @Transient ItemDAO idao,
                           @Transient @BaselineScorer ItemScorer baseline,
                           @LatentFeatureCount int nfeatures) {
        logger.debug("user DAO: {}", udao);
        userEventDAO = uedao;
        userDAO = udao;
        itemDAO = idao;
        baselineScorer = baseline;
        featureCount = nfeatures;
    }

    /**
     * Build the SVD model.
     *
     * @return A singular value decomposition recommender model.
     */
    @Override
    public SVDModel get() {
        // Create index mappings of user and item IDs.
        // You can use these to find row and columns in the matrix based on user/item IDs.
        Long2LongMap userMapping = new Long2LongOpenHashMap();
        int i = 0;
        for (Long userId : userDAO.getUserIds()) {
            userMapping.put(userId, i);
            i++;
        }

        Long2LongMap itemMapping = new Long2LongOpenHashMap();
        int j = 0;
        for (Long itemId : itemDAO.getItemIds()) {
            itemMapping.put(itemId, j);
            j++;
        }
        // We have to do 2 things:
        // First, prepare a matrix containing the rating data.
        RealMatrix matrix = createRatingMatrix(userMapping, itemMapping);

        // Second, compute its factorization
        SingularValueDecomposition svd = new SingularValueDecomposition(matrix);

        // Set LatentFeatureCount to 0 to turn off truncation
        int numFeatures = featureCount;
        if (numFeatures == 0) {
            numFeatures = Math.min(itemDAO.getItemIds().size(), userDAO.getUserIds().size());
        }
        
        // Third, truncate the decomposed matrix
        RealMatrix userMatrix = svd.getU().getSubMatrix(0, userMapping.size() - 1,
                                                        0, numFeatures - 1);
        RealMatrix itemMatrix = svd.getV().getSubMatrix(0, itemMapping.size() - 1,
                                                        0, numFeatures - 1);

        RealVector weights = new ArrayRealVector(svd.getSingularValues(), 0, numFeatures);

        return new SVDModel(userMapping, itemMapping,
                            userMatrix, itemMatrix, weights);
    }

    /**
     * Build a rating residual matrix from the rating data.  Each user's ratings are
     * normalized by subtracting a baseline score (usually a mean).
     *
     * @param userMapping The index mapping of user IDs to column numbers.
     * @param itemMapping The index mapping of item IDs to row numbers.
     * @return A matrix storing the <i>normalized</i> user ratings.
     */
    private RealMatrix createRatingMatrix(Long2LongMap userMapping, Long2LongMap itemMapping) {
        final int nusers = userMapping.size();
        final int nitems = itemMapping.size();

        // Create a matrix with users on rows and items on columns
        logger.info("creating {} by {} rating matrix", nusers, nitems);
        RealMatrix matrix = MatrixUtils.createRealMatrix(nusers, nitems);

        // populate it with data
        ObjectStream<UserHistory<Event>> users = userEventDAO.streamEventsByUser();
        try {
            for (UserHistory<Event> user: users) {
                int u = (int) userMapping.get(user.getUserId());

                Long2DoubleMap ratings = new Long2DoubleOpenHashMap(Ratings.userRatingVector(user.filter(Rating.class)));

                Map<Long, Double> baselines = baselineScorer.score(user.getUserId(), ratings.keySet());

                // Populate this user's row wiht their ratings, minus the baseline scores
                for (Map.Entry<Long, Double> e : ratings.entrySet()) {
                    long item = e.getKey();
                    int i = (int)itemMapping.get(item);
                    double rating = e.getValue() - baselines.get(item);
                    matrix.setEntry(u, i, rating);
                }
            }
        } finally {
            users.close();
        }

        return matrix;
    }
}
