import edu.umn.cs.recsys.*
import edu.umn.cs.recsys.cbf.LuceneItemItemModel
import edu.umn.cs.recsys.dao.*

import org.lenskit.*
import org.lenskit.baseline.*
import org.lenskit.data.dao.ItemDAO
import org.lenskit.data.dao.MapItemNameDAO
import org.lenskit.data.dao.UserEventDAO
import org.grouplens.lenskit.data.text.CSVFileItemNameDAOProvider
import org.grouplens.lenskit.data.text.ItemFile
import org.lenskit.knn.*
import org.lenskit.knn.item.*
import org.lenskit.knn.item.model.ItemItemModel
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

algorithm("GlobalMean") {
    include tagConfig
    // score items by the global mean
    bind ItemScorer to GlobalMeanRatingItemScorer
    // recommendation is meaningless for this algorithm
    bind ItemRecommender to null
}

algorithm("Popular") {
    include tagConfig
    // score items by their popularity
    bind ItemScorer to PopularityItemScorer
    // rating prediction is meaningless for this algorithm
    bind RatingPredictor to null
}

algorithm("ItemMean") {
    include tagConfig
    // score items by their mean rating
    bind ItemScorer to ItemMeanRatingItemScorer
}

algorithm("PersMean") {
    include tagConfig
    bind ItemScorer to UserMeanItemScorer
    bind (UserMeanBaseline, ItemScorer) to ItemMeanRatingItemScorer
}

for (nnbrs in [5, 10, 15, 20, 25, 30, 40, 50, 75, 100]) {
    algorithm("UserUser") {
        include tagConfig
        // Attributes let you specify additional properties of the algorithm.
        // They go in the output file, so you can do things like plot accuracy
        // by neighborhood size
        attributes["NNbrs"] = nnbrs
        // use the user-user rating predictor
        bind ItemScorer to UserUserItemScorer
        set NeighborhoodSize to nnbrs
        bind VectorSimilarity to PearsonCorrelation
    }
    algorithm("UserUserNorm") {
        include tagConfig
        // Attributes let you specify additional properties of the algorithm.
        // They go in the output file, so you can do things like plot accuracy
        // by neighborhood size
        attributes["NNbrs"] = nnbrs
        // use the user-user rating predictor
        bind ItemScorer to UserUserItemScorer
        set NeighborhoodSize to nnbrs
        bind VectorNormalizer to MeanCenteringVectorNormalizer
        bind VectorSimilarity to PearsonCorrelation
    }
    algorithm("Lucene") {
        attributes["NNbrs"] = nnbrs
        include tagConfig
        bind ItemScorer to ItemItemScorer
        bind ItemItemModel to LuceneItemItemModel
        set NeighborhoodSize to nnbrs
        // consider using all 100 movies as neighbors
        set ModelSize to 100
    }
    algorithm("LuceneNorm") {
        attributes["NNbrs"] = nnbrs
        include tagConfig
        bind ItemScorer to ItemItemScorer
        bind ItemItemModel to LuceneItemItemModel
        set NeighborhoodSize to nnbrs
        // consider using all 100 movies as neighbors
        set ModelSize to 100
        bind UserVectorNormalizer to BaselineSubtractingUserVectorNormalizer
        within (UserVectorNormalizer) {
            bind (BaselineScorer, ItemScorer) to ItemMeanRatingItemScorer
        }
    }
}
