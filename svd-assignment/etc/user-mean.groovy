import org.lenskit.api.ItemScorer
import org.lenskit.baseline.BaselineScorer
import org.lenskit.baseline.UserMeanItemScorer

bind (BaselineScorer,ItemScorer) to UserMeanItemScorer
