package data.network.top

import android.support.annotation.VisibleForTesting
import com.nytimes.android.external.store.base.impl.Store
import com.nytimes.android.external.store.base.impl.StoreBuilder
import com.nytimes.android.external.store.middleware.GsonParserFactory
import data.network.common.ApiService
import okio.BufferedSource
import org.jorge.ms.data.BuildConfig
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory

/**
 * Contains the data source for top requests.
 */
internal object TopRequestSource {
    private val retrofit: ApiService = Retrofit.Builder()
            .baseUrl(BuildConfig.API_URL)
            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
            .validateEagerly(true)
            .build()
            .create(ApiService::class.java)
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var delegate: Store<TopRequestDataContainer, TopRequestParameters>

    /**
     * Initializes the actual store.
     */
    init {
        // We want to have long-term caching , since it is about all-time tops, which does not
        // change very frequently. Therefore we are fine using the default memory cache
        // implementation which expires items in 24h after acquisition.
        // We will, however, not use disk-level caching because we always want fresh data on
        // entering the app (you would expect when you open your Reddit reader to see what is
        // actually live, wouldn't you?)
        delegate = StoreBuilder.parsedWithKey<TopRequestParameters, BufferedSource, TopRequestDataContainer>()
                .fetcher({ this.topFetcher(it) })
                .parser(GsonParserFactory.createSourceParser<TopRequestDataContainer>(TopRequestDataContainer::class.java))
                // Never try to refresh from network on stale since it will very likely not be worth
                // and it is not required because we do it on app launch anyway
                .refreshOnStale()
                .open()
    }

    /**
     * Delegates to its internal responsible for the request.
     * @param topRequestParameters The parameters of the request.
     * @see Store
     */
    internal fun fetch(topRequestParameters: TopRequestParameters) = delegate.fetch(topRequestParameters)

    /**
     * Delegates to its internal responsible for the request.
     * @param topRequestParameters The parameters of the request.
     * @see Store
     */
    internal fun get(topRequestParameters: TopRequestParameters) = delegate.get(topRequestParameters)

    /**
     * Provides a Fetcher for the top store.
     * @param topRequestParameters The parameters for the request.
     * @see com.nytimes.android.external.store.base.Fetcher
     */
    private fun topFetcher(topRequestParameters: TopRequestParameters) = retrofit
            .top(topRequestParameters.subreddit, topRequestParameters.time, topRequestParameters.after,
                    topRequestParameters.limit)
            .map { it.source() }
}