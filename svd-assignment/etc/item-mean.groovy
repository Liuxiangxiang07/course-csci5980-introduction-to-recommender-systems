import org.lenskit.api.ItemScorer
import org.lenskit.baseline.BaselineScorer
import org.lenskit.baseline.ItemMeanRatingItemScorer

bind (BaselineScorer,ItemScorer) to ItemMeanRatingItemScorer
