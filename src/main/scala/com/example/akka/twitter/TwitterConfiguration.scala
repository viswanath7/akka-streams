package com.example.akka.twitter

import com.typesafe.config.ConfigFactory

object TwitterConfiguration {
  val configuration = ConfigFactory.load.getConfig("twitter")
  val apiKey = configuration.getString("apiKey")
  val apiSecret = configuration.getString("apiSecret")
  val accessToken = configuration.getString("accessToken")
  val accessTokenSecret = configuration.getString("accessTokenSecret")
}
