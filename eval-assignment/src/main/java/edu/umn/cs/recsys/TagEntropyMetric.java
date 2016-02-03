package edu.umn.cs.recsys;

import com.google.common.collect.Sets;
import edu.umn.cs.recsys.dao.ItemTagDAO;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.lenskit.LenskitRecommender;
import org.lenskit.api.Recommender;
import org.lenskit.api.Result;
import org.lenskit.api.ResultList;
import org.lenskit.eval.traintest.AlgorithmInstance;
import org.lenskit.eval.traintest.DataSet;
import org.lenskit.eval.traintest.TestUser;
import org.lenskit.eval.traintest.metrics.MetricColumn;
import org.lenskit.eval.traintest.metrics.MetricResult;
import org.lenskit.eval.traintest.metrics.TypedMetricResult;
import org.lenskit.eval.traintest.recommend.TopNMetric;
import org.lenskit.util.math.Vectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

/**
 * A metric that measures the tag entropy of the recommended items.
 */
public class TagEntropyMetric extends TopNMetric<TagEntropyMetric.Context> {
    /**
     * Construct a new tag entropy metric.
     */
    public TagEntropyMetric() {
        super(TagEntropyResult.class, TagEntropyResult.class);
    }

    /**
     * Actually measure the performance or other value over a user's results.
     * @param user The user.
     * @param context The context or accumulator.
     * @return The results for this user, to be written to the per-user output file.
     */
    @Nonnull
    @Override
    public MetricResult measureUser(TestUser user, ResultList recommendations, Context context) {
        int n = recommendations.size();


        if (recommendations == null || recommendations.isEmpty()) {
            return MetricResult.empty();
            // no results for this user.
        }
        // get tag data from the context so we can use it
        ItemTagDAO tagDAO = context.getItemTagDAO();
        TagVocabulary vocab = context.getTagVocabulary();

        double entropy = 0;
        
        // Implement tag entropy metric
        // Calculate probabilities
        Long2DoubleMap probability = new Long2DoubleOpenHashMap();
        for (Long item : recommendations.idList()) {
            List<String> tags = tagDAO.getItemTags(item);
            Set<String> seenTags = new HashSet<String>();
            for (String tag : tags) {
                seenTags.add(tag);
            }
            int m = seenTags.size();
            for (String tag : seenTags) {
                Long tagId = vocab.getTagId(tag);
                Double newValue = 1.0 / (m * n);
                if (probability.containsKey(tagId)) {
                    newValue += probability.get(tagId);
                }
                probability.put(tagId, newValue);
            }
        }

        // Calculate entropy
        for (Map.Entry<Long, Double> e : probability.entrySet()) {
            double p = e.getValue();
            entropy -= p * Math.log(p) / Math.log(2);
        }
        
        // record the entropy in the context for aggregation
        context.addUser(entropy);
        
        // and finally return this user's evaluation results
        return new TagEntropyResult(entropy);
    }

    /**
     * Make a metric accumulator.  Metrics operate with <em>contexts</em>, which are created
     * for each algorithm and data set.  The context is provided when computing each user's output,
     * and is usually used to accumulate the results into a global statistic for the whole
     * evaluation.
     *
     * @param algorithm The algorithm being tested.
     * @param data The data set being tested with.
     * @param rec The recommender instance for the current algorithm (if applicable).
     * @return An accumulator for analyzing this algorithm and data set.
     */
    @Nullable
    @Override
    public Context createContext(AlgorithmInstance algorithm, DataSet dataSet, Recommender recommender) {
        return new Context((LenskitRecommender) recommender);
    }

    /**
     * Get the aggregate results for an experimental run.
     * @param context The context for the experimental run.
     * @return The aggregate results for the experimental run.
     */
    @Nonnull
    @Override
    public MetricResult getAggregateMeasurements(Context context) {
        return new TagEntropyResult(context.getMeanEntropy());
    }

    /**
     * Result type for the entropy metric. This encapsulates the entropy and gives it a column name.
     */
    public static class TagEntropyResult extends TypedMetricResult {
        @MetricColumn("TopN.TagEntropy")
        public final double entropy;

        public TagEntropyResult(double ent) {
            entropy = ent;
        }

    }

    /**
     * Context class for accumulating the total entropy across users.  This context also remembers
     * the recommender, so we can get the tag data.
     */
    public static class Context {
        private LenskitRecommender recommender;
        private double totalEntropy;
        private int userCount;

        /**
         * Create a new context for evaluating a particular recommender.
         *
         * @param rec The recommender being evaluated.
         */
        public Context(LenskitRecommender rec) {
            recommender = rec;
        }

        /**
         * Get the recommender being evaluated.
         *
         * @return The recommender being evaluated.
         */
        public LenskitRecommender getRecommender() {
            return recommender;
        }

        /**
         * Get the item tag DAO for this evaluation context.
         *
         * @return A DAO providing access to the tag lists of items.
         */
        public ItemTagDAO getItemTagDAO() {
            return recommender.get(ItemTagDAO.class);
        }

        /**
         * Get the tag vocabulary for the current recommender evaluation.
         *
         * @return The tag vocabulary for this evaluation context.
         */
        public TagVocabulary getTagVocabulary() {
            return recommender.get(TagVocabulary.class);
        }

        /**
         * Add the entropy for a user to this context.
         *
         * @param entropy The entropy for one user.
         */
        public void addUser(double entropy) {
            totalEntropy += entropy;
            userCount += 1;
        }

        /**
         * Get the average entropy over all users.
         *
         * @return The average entropy over all users.
         */
        public double getMeanEntropy() {
            return totalEntropy / userCount;
        }
    }
}
