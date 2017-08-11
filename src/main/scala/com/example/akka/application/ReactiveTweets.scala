package com.example.akka.application

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.example.akka.{Author, Tweet}
import com.example.akka.twitter.TwitterClient
import org.slf4j.LoggerFactory

import twitter4j.Status

object ReactiveTweets extends App {

  private val searchableHashTag: String = args.find(arg => arg.startsWith("#")).getOrElse("#Akka")

  import actorSystem.dispatcher
  val logger = LoggerFactory getLogger ReactiveTweets.getClass

  implicit val actorSystem = ActorSystem("akka-streams-reactive-tweets-actor-system")
  implicit val flowMaterialiser = ActorMaterializer()

  private val statuses: Iterator[Status] = TwitterClient.retrieveTweets(searchableHashTag)
  val source = Source.fromIterator(() => statuses)
  val normalize = Flow[Status].map{ status => Tweet(Author(status.getUser.getName), status.getText) }
  val sink = Sink.foreach[Tweet](println)

  source.via(normalize).runWith(sink).andThen({case _ =>actorSystem.terminate()})
}
