import org.lenskit.api.ItemScorer
import org.lenskit.baseline.BaselineScorer
import org.lenskit.baseline.GlobalMeanRatingItemScorer

bind (BaselineScorer,ItemScorer) to GlobalMeanRatingItemScorer
