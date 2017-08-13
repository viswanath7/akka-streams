package com.example.akka.application

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.example.akka.twitter.TwitterClient
import com.example.akka.{Author, Tweet}
import org.slf4j.LoggerFactory
import twitter4j.Status

object TwitterStreamComponents {

  val normalize = Flow[Status].map { status => Tweet(Author(status.getUser.getName), status.getText) }
  val sink = Sink.foreach[Tweet](println)

  def source(programArguments: Array[String]) = Source.fromIterator(() => statuses(programArguments))

  def statuses(arguments: Array[String]): Iterator[Status] = {

    def searchableHashTag: String = arguments.find(arg => arg.startsWith("#")).getOrElse("#scala")

    TwitterClient.retrieveTweets(searchableHashTag)
  }

}

object TwitterTweets extends App {

  import actorSystem.dispatcher
  import com.example.akka.application.TwitterStreamComponents.{normalize, sink, source}

  val logger = LoggerFactory getLogger TwitterTweets.getClass

  implicit val actorSystem: ActorSystem = ActorSystem("akka-streams-reactive-tweets-actor-system")

  implicit val flowMaterialiser: ActorMaterializer = ActorMaterializer()

  source(args)
    .via(normalize)
    .runWith(sink)
    .andThen({ case _ => actorSystem.terminate() })
}
