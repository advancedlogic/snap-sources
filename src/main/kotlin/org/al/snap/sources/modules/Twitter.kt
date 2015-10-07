package org.al.snap.sources.modules

import net.minidev.json.JSONArray
import org.al.snap.sources.camel.Camel
import org.al.snap.sources.models.Source
import org.al.snap.sources.models.WebEntity
import org.al.snap.sources.tools.Conf
import twitter4j.Query
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder
import java.io.FileNotFoundException
import java.util.*

/**
 * Created by skywalker on 10/7/15.
 */
public class Twitter: Module() {
    data class Account(val consumerKey:String, val consumerSecret:String, val accessToken:String, val accessSecret:String)
    var accounts = ArrayList<Account>()

    init {
        val jsAccounts = Conf.getOrDefault("api.twitter.accounts", JSONArray())
        for (item in jsAccounts) {
            val jsAccount = item as LinkedHashMap<String, String>
            val account = Account(jsAccount.getOrDefault("consumerKey", ""), jsAccount.getOrDefault("consumerSecret", ""), jsAccount.getOrDefault("accessToken", ""), jsAccount.getOrDefault("accessTokenSecret", ""))
            this.accounts.add(account)
        }
    }

    override fun process(source: Source) {
        val queries = source.params.getOrDefault("urls", "")
        if (queries != "") {
            handleSearch(queries.split(",").map { it.trim() })
        }
    }

    public fun handleSearch(queries: List<String>) {
        val bannedAccount = HashSet<twitter4j.Twitter>()
        for (query in queries) {
            var twitter:twitter4j.Twitter? = null
            try {
                var counter = accounts.size()
                do {
                    twitter = twitterAccount()
                    counter--
                    if (counter == 0) return
                } while(twitter in bannedAccount)

                val twitterQuery = Query("${query.trim()}")
                val result = twitter?.search(twitterQuery)
                for (status in result?.getTweets()!!) {
                    val urls = status.getURLEntities()
                    for (url in urls) {
                        val _url = url.getExpandedURL() ?: url.getURL()
                        if (_url != "") {
                            val webEntity = WebEntity()
                            webEntity.url = _url
                            webEntity.module = "link"

                            info("Send ${webEntity.url} for module ${webEntity.module}}")
                            Camel.send(slowEndpoint, webEntity)
                        }
                    }
                    var author = status.getUser().getName()
                    var image = status.getUser().getBiggerProfileImageURL()
                    var date = status.getCreatedAt()
                    var text = status.getText()
                    val webEntity = WebEntity()
                    webEntity.author = author
                    webEntity.image = image
                    webEntity.date = date
                    webEntity.content = text
                    webEntity.module = "twitter"

                    info("Send ${webEntity.content} for module ${webEntity.module}}")
                    Camel.send(fastEndpoint, webEntity.toJSONString())
                }
            } catch(e: Exception) {
                warn(e.getMessage() ?: "Error with query $query (Waiting 15 mins)")
                if (twitter != null) bannedAccount.add(twitter)
            }
        }
    }

    private fun randomAccount():Account? {
        val l = accounts.size()
        if (l == 0) return null
        val i = (l * Math.random()).toInt()
        return if (i < l) this.accounts.get(i) else this.accounts.get(0)
    }

    public fun twitterAccount(): twitter4j.Twitter? {
        try {
            val account = randomAccount()
            val configurationBuilder = ConfigurationBuilder()
            configurationBuilder.setOAuthConsumerKey(account?.consumerKey)
            configurationBuilder.setOAuthConsumerSecret(account?.consumerSecret)
            configurationBuilder.setOAuthAccessToken(account?.accessToken)
            configurationBuilder.setOAuthAccessTokenSecret(account?.accessSecret)
            val configuration = configurationBuilder.build()
            val twitter = TwitterFactory(configuration)
            return twitter.getInstance()
        } catch(e: FileNotFoundException) {
            warn(e.getMessage()?:"No twitter account available at the moment")
            return null
        }
    }
}