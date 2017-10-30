package com.example.akka.application

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.example.akka.twitter.TwitterClient
import com.example.akka.{Author, Tweet}
import org.slf4j.LoggerFactory
import twitter4j.Status

import scala.language.postfixOps

object TwitterStreamComponents {
  
  val logger = LoggerFactory getLogger TwitterStreamComponents.getClass
  
  val normalize = Flow[Status].map { status => Tweet(Author(status.getUser.getName), status.getText) }
  val sink = Sink.foreach[Tweet](logger debug _.toString)
  
  //_* is type annotation for repeated parameters
  def source(programArguments: String*) = Source.fromIterator(() => statuses(programArguments: _*))
  
  def statuses(arguments: String*): Iterator[Status] = {
    
    logger debug "Finding the first program argument starting with #. Hash tag #scala shall be used as the fallback value"
    def searchableHashTag: String = arguments.find(arg => arg.startsWith("#")).getOrElse("#scala")
    
    logger debug s"Retrieving tweets for the hash-tag $searchableHashTag ..."
    TwitterClient retrieveTweets searchableHashTag
  }

}

object TwitterTweets extends App {

  val logger = LoggerFactory getLogger TwitterTweets.getClass

  implicit val actorSystem: ActorSystem = ActorSystem("akka-streams-reactive-tweets-actor-system")

  implicit val flowMaterialiser: ActorMaterializer = ActorMaterializer()
  
  if (args isEmpty) {
    logger warn "Incorrect usage!"
    logger warn "Please supply hash tag to search in twitter as program argument."
    logger warn "Fallback value #scala shall be utilised for demonstration purpose ..."
  }
  
  import actorSystem.dispatcher
  import com.example.akka.application.TwitterStreamComponents.{normalize, sink, source}
  
  logger debug "Source: Tweets for first hash-tag supplied as program argument"
  logger debug "Flow: Transforms twitter's status object to our custom object Tweet"
  logger debug "Sink: Logs each Tweet"
  logger debug "Connecting the source to sink via transformation and running it ..."
  
  /*
  * A Flow that has both ends “attached” to a Source and Sink respectively, and is ready to be run() and materialised.
  */
  
  source(args: _*) //_* is type annotation for repeated parameters
    .via(normalize)
    .runWith(sink)
    .andThen({ case _ => actorSystem.terminate() })
}
