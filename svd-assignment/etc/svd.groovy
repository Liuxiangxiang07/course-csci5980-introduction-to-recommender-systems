import edu.umn.cs.recsys.dao.CSVItemTagDAO
import edu.umn.cs.recsys.dao.ItemTagDAO
import edu.umn.cs.recsys.dao.TagFile
import edu.umn.cs.recsys.svd.LatentFeatureCount
import edu.umn.cs.recsys.svd.SVDItemScorer
import org.lenskit.api.ItemScorer
import org.lenskit.data.dao.EventDAO
import org.lenskit.data.dao.ItemNameDAO
import org.grouplens.lenskit.data.text.*

// Set up item scorer
bind ItemScorer to SVDItemScorer
set LatentFeatureCount to 10

// set up data access
bind ItemNameDAO toProvider CSVFileItemNameDAOProvider
set ItemFile to "data/movie-titles.csv"
bind EventDAO to TextEventDAO
set EventFile to "data/ratings.csv"
bind EventFormat to Formats.csvRatings()
bind ItemTagDAO to CSVItemTagDAO
set TagFile to "data/movie-tags.csv"
