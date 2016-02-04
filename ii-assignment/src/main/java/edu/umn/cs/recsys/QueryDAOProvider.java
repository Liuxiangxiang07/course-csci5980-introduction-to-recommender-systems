package edu.umn.cs.recsys;

import org.lenskit.data.dao.EventDAO;
import org.lenskit.data.dao.PrefetchingUserEventDAO;
import org.lenskit.data.dao.UserEventDAO;
import org.lenskit.eval.traintest.QueryData;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * DAO shim to let scorers use the query data.  This is a helper class for use in the evaluation.
 */
public class QueryDAOProvider implements Provider<UserEventDAO> {
    private final EventDAO queryEvents;

    @Inject
    public QueryDAOProvider(@QueryData EventDAO qEvents) {
        queryEvents = qEvents;
    }
    @Override
    public UserEventDAO get() {
        return new PrefetchingUserEventDAO(queryEvents);
    }
}
