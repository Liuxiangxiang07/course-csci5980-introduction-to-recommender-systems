package edu.umn.cs.recsys.ii;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import org.lenskit.util.collections.LongUtils;
import org.lenskit.inject.Transient;
import org.lenskit.data.dao.ItemDAO;
import org.lenskit.data.dao.ItemEventDAO;
import org.lenskit.data.ratings.Rating;
import org.lenskit.data.ratings.Ratings;
import org.lenskit.data.history.ItemEventCollection;
import org.lenskit.util.math.Vectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lenskit.util.io.ObjectStream;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SimpleItemItemModelBuilder implements Provider<SimpleItemItemModel> {
    private static final Logger logger = LoggerFactory.getLogger(SimpleItemItemModelBuilder.class);;

    private final ItemDAO itemDao;
    private final ItemEventDAO itemEventDAO;

    @Inject
    public SimpleItemItemModelBuilder(@Transient ItemDAO idao,
                                      @Transient ItemEventDAO iedao) {
        itemDao = idao;
        itemEventDAO = iedao;
    }

    @Override
    public SimpleItemItemModel get() {
        Map<Long,Long2DoubleMap> itemVectors = Maps.newHashMap();
        Long2DoubleMap itemMeans = new Long2DoubleOpenHashMap();

        ObjectStream<ItemEventCollection<Rating>> stream = itemEventDAO.streamEventsByItem(Rating.class);
        try {
            for (ItemEventCollection<Rating> item: stream) {
                Long2DoubleOpenHashMap ratings = new Long2DoubleOpenHashMap(Ratings.itemRatingVector(item));
                
                // TODO: Store the item's mean rating in itemMeans
                // TODO: Normalize the item vector before putting it in itemVectors

                itemVectors.put(item.getItemId(), LongUtils.frozenMap(ratings));
            }
        } finally {
            stream.close();
        }

        LongSortedSet items = LongUtils.packedSet(itemVectors.keySet());

        // Create an object to store the similarity matrix
        Map<Long,Long2DoubleMap> itemSimilarities = Maps.newHashMap();
        for (long i: itemVectors.keySet()) {
            itemSimilarities.put(i, new Long2DoubleOpenHashMap());
        }
        
        // TODO: Compute similarities between each pair of items and
        // store those values in itemSimilarities object.
        // HINT: itemSimilarities.get(i1).put(i2, sim);

        return new SimpleItemItemModel(LongUtils.frozenMap(itemMeans), itemSimilarities);
    }

    /**
     * Load the data into memory, indexed by item.
     * @return A map from item IDs to item rating vectors. Each vector contains users' ratings for
     * the item, keyed by user ID.
     */
    public Map<Long,Long2DoubleMap> getItemVectors() {
        Map<Long,Long2DoubleMap> itemData = Maps.newHashMap();

        ObjectStream<ItemEventCollection<Rating>> stream = itemEventDAO.streamEventsByItem(Rating.class);
        try {
            for (ItemEventCollection<Rating> item: stream) {
                Long2DoubleMap vector = Ratings.itemRatingVector(item);

                // Compute and store the item's mean.
                double mean = Vectors.mean(vector);

                // Mean center the ratings.
                for (Map.Entry<Long, Double> entry : vector.entrySet()) {
                    entry.setValue(entry.getValue() - mean);
                }

                itemData.put(item.getItemId(), LongUtils.frozenMap(vector));
            }
        } finally {
            stream.close();
        }

        return itemData;
    }
}
