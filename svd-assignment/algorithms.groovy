import edu.umn.cs.recsys.dao.*
import edu.umn.cs.recsys.*
import edu.umn.cs.recsys.svd.*
import org.lenskit.data.dao.MapItemNameDAO
import org.grouplens.lenskit.data.text.CSVFileItemNameDAOProvider

import org.lenskit.api.ItemScorer
import org.lenskit.baseline.*
import org.lenskit.data.dao.ItemDAO
import org.grouplens.lenskit.data.text.ItemFile
import org.lenskit.knn.NeighborhoodSize
import org.lenskit.knn.item.*
import org.lenskit.*
import org.lenskit.data.dao.ItemDAO
import org.lenskit.data.dao.MapItemNameDAO
import org.grouplens.lenskit.data.text.CSVFileItemNameDAOProvider
import org.lenskit.baseline.*
import org.lenskit.data.dao.UserEventDAO
import org.grouplens.lenskit.data.text.ItemFile
import org.lenskit.knn.*
import org.lenskit.knn.user.*
import org.grouplens.lenskit.transform.normalize.*
import org.grouplens.lenskit.vectors.similarity.*

// common configuration to make tags available
// needed for both some algorithms and for metrics
// this defines a variable containing a Groovy closure, if you care about that kind of thing
tagConfig = {
    // use our tag data
    bind ItemTagDAO to CSVItemTagDAO
    // and our movie titles
    bind MapItemNameDAO toProvider CSVFileItemNameDAOProvider
    // configure input files for both of those
    set TagFile to new File("data/movie-tags.csv")
    set ItemFile to new File("data/movie-titles.csv")
    // need tag vocab & DAO to be roots for diversity metric to use them
    config.addRoot ItemTagDAO
    config.addRoot ItemDAO
    config.addRoot TagVocabulary
    config.addRoot UserEventDAO
}


algorithm("PersMean") {
    include tagConfig
    bind ItemScorer to UserMeanItemScorer
    bind (UserMeanBaseline, ItemScorer) to ItemMeanRatingItemScorer
}

algorithm("ItemItem") {
    include tagConfig
    bind ItemScorer to ItemItemScorer
    bind VectorNormalizer to MeanCenteringVectorNormalizer
    set NeighborhoodSize to 15
}

// test different SVD sizes
for (size in [1, 5, 10, 15, 20, 25, 30, 40, 50]) {
    algorithm("SVDGlobalMean") {
        include tagConfig
        attributes["FeatureCount"] = size
        bind ItemScorer to SVDItemScorer
        set LatentFeatureCount to size
        // compute SVD of offsets from user mean
        bind (BaselineScorer, ItemScorer) to GlobalMeanRatingItemScorer
    }

    algorithm("SVDUserMean") {
        include tagConfig
        attributes["FeatureCount"] = size
        bind ItemScorer to SVDItemScorer
        set LatentFeatureCount to size
        // compute SVD of offsets from user mean
        bind (BaselineScorer, ItemScorer) to UserMeanItemScorer
    }

    algorithm("SVDItemMean") {
        include tagConfig
        attributes["FeatureCount"] = size
        bind ItemScorer to SVDItemScorer
        set LatentFeatureCount to size
        // compute SVD of offsets from user mean
        bind (BaselineScorer, ItemScorer) to ItemMeanRatingItemScorer
    }

    algorithm("SVDPersMean") {
        include tagConfig
        attributes["FeatureCount"] = size
        bind ItemScorer to SVDItemScorer
        set LatentFeatureCount to size
        // compute SVD of offsets from item-user mean
        bind (BaselineScorer, ItemScorer) to UserMeanItemScorer
        bind (UserMeanBaseline, ItemScorer) to ItemMeanRatingItemScorer
    }
}

