package com.example.akka.twitter

import twitter4j.conf.ConfigurationBuilder
import twitter4j.{Query, Status, Twitter, TwitterFactory}

import scala.collection.JavaConverters._

object TwitterClient {

  def getInstance: Twitter = {

    val configurationBuilder = new ConfigurationBuilder()
    configurationBuilder.setDebugEnabled(false)
      .setOAuthConsumerKey(TwitterConfiguration.apiKey)
      .setOAuthConsumerSecret(TwitterConfiguration.apiSecret)
      .setOAuthAccessToken(TwitterConfiguration.accessToken)
      .setOAuthAccessTokenSecret(TwitterConfiguration.accessTokenSecret)

    new TwitterFactory(configurationBuilder.build()).getInstance()
  }

  def retrieveTweets(term: String): Iterator[Status] = {
    val query = new Query(term)
    query setCount 100
    getInstance.search(query).getTweets.asScala.iterator
  }
}
