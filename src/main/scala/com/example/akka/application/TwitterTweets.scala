package com.example.akka.application

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.example.akka.twitter.TwitterClient
import com.example.akka.{Author, Tweet}
import org.slf4j.LoggerFactory
import twitter4j.Status

object TwitterTweets extends App {

  private val searchableHashTag: String = args.find(arg => arg.startsWith("#")).getOrElse("#scala")

  import actorSystem.dispatcher

  val logger = LoggerFactory getLogger TwitterTweets.getClass

  implicit val actorSystem: ActorSystem = ActorSystem("akka-streams-reactive-tweets-actor-system")
  implicit val flowMaterialiser: ActorMaterializer = ActorMaterializer()

  private val statuses: Iterator[Status] = TwitterClient.retrieveTweets(searchableHashTag)
  val source = Source.fromIterator(() => statuses)
  val normalize = Flow[Status].map{ status => Tweet(Author(status.getUser.getName), status.getText) }
  val sink = Sink.foreach[Tweet](println)

  source.via(normalize).runWith(sink).andThen({case _ =>actorSystem.terminate()})
}
