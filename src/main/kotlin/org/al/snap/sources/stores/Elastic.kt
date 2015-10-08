package org.al.snap.sources.stores

import org.al.snap.sources.tools.Conf
import org.al.snap.sources.tools.Log
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.query.QueryBuilders
import kotlin.properties.Delegates

/**
 * Created by skywalker on 9/17/15.
 */

public object Elastic: Log {

    var client: TransportClient by Delegates.notNull()
    var esIndex = "snap"
    var esType = "source"

    fun init() {
        val settings = ImmutableSettings.settingsBuilder()
                ?.put("client.transport.ping_timeout", "10s")
                ?.put("client.transport.sniff", false)
                ?.build()
        client = TransportClient(settings)
        esIndex = Conf.getOrDefault("elasticsearch.index", "snap")
        esType = Conf.getOrDefault("elasticsearch.type", "source")
        client.addTransportAddress(InetSocketTransportAddress(Conf.getOrDefault("elasticsearch.host", "localhost"), Conf.getOrDefault("elasticsearch.port", 9300)))

        try {
            client.admin()?.indices()?.prepareCreate(esIndex)?.setTimeout(TimeValue.timeValueSeconds(30))?.execute()?.actionGet()
        } catch(e:Exception) {
            warn(e.getMessage()?:"Index $esIndex already exists")
        }
    }

    fun close() {
        try {
            client.close()
        } catch(e:Exception) {
            error(e.getMessage()?:"Something happened closing Elastic Search connection. Sorry")
        }
    }

    fun check(field:String, value:String):Boolean {
        try {
            val response = client.prepareSearch(esIndex)
                    ?.setTypes(esType)
                    ?.setSearchType(SearchType.DFS_QUERY_AND_FETCH)
                    ?.setQuery(QueryBuilders.termQuery(field, value))
                    ?.execute()
                    ?.actionGet()
            val len = response?.getHits()?.getHits()?.size()
            return len != 0
        } catch(e:Exception) {
            warn(e.getMessage()?:"Error checking $field:$value on Elastic Search")
            return false
        }
    }
}