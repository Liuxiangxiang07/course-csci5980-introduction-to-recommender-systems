import edu.umn.cs.recsys.uu.SimpleUserUserItemScorer
import org.lenskit.api.ItemScorer
import org.lenskit.data.dao.EventDAO
import org.lenskit.data.dao.ItemNameDAO
import org.grouplens.lenskit.data.text.ItemFile
import org.grouplens.lenskit.data.text.EventFile
import org.grouplens.lenskit.data.text.EventFormat
import org.grouplens.lenskit.data.text.Formats
import org.grouplens.lenskit.data.text.TextEventDAO
import org.grouplens.lenskit.data.text.CSVFileItemNameDAOProvider

// use our item scorer
bind ItemScorer to SimpleUserUserItemScorer

// set up data access
bind ItemNameDAO toProvider CSVFileItemNameDAOProvider
set ItemFile to "data/movie-titles.csv"
bind EventDAO to TextEventDAO
set EventFile to "data/ratings.csv"
bind EventFormat to Formats.csvRatings()
