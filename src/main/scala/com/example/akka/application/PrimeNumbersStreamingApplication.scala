package com.example.akka.application

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import org.slf4j.LoggerFactory

import scala.collection.immutable
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

object StreamDefinition {

  /**
    *   A stream usually begins at a source; which in case happens to be a range of natural numbers from 1 to 100
    *
	  * The Source type is parameterised with two types: the first one is the type of element that this source emits
    *   and the second one may signal that running the source produces some auxiliary value (e.g. a network source may
    *   provide information about the bound port or the peer’s address). Where no auxiliary information is produced,
    *   the type akka.NotUsed is used—and a simple range of integers surely falls into this category.
    */
  val source: Source[Int, NotUsed] = Source(1 to 100)

  // Define transformation(s) on the source by defining a flow
  val flow = Flow[Int] filter isPrime
  // Consumer function that collects the prime numbers; it holds a future with a Seq[Int]
  val sink = Sink.seq[Int]

  def isPrime(num: Int): Boolean = !((2 until num - 1) exists (num % _ == 0))

}

/**
  * Prints prime numbers between 1 and 1000 using akka stream API
  */
object PrimeNumbersStreamingApplication extends App {

  val logger = LoggerFactory getLogger PrimeNumbersStreamingApplication.getClass

  import actorSystem.dispatcher

  implicit val actorSystem: ActorSystem = ActorSystem("StreamActorSystem")

  /**
    * Factory for stream execution engines that runs the streams.
    * Any 'run' method on a source requires a materialiser for its actual execution.
    *
    * Source is a description of execution like a blueprint;
    * which is intended to be reused and incorporated in a larger system.
    */
  val materialiser: ActorMaterializer = ActorMaterializer()
	
	logger debug "Akka stream: Prime numbers"
	logger debug "Source: Numbers between 1 and 100"
	logger debug "Flow / transformation: Filter for prime numbers"
	logger debug "Sink: Collects result in a sequence."
	
	logger debug "Connecting the source to sink via transformation and running it ..."
  StreamDefinition.source
    .via(StreamDefinition.flow)
    .runWith(StreamDefinition.sink)(materialiser)
	  .onComplete(handleComputationResult)
	
	/**
		* Prints on the console the elements of the sequence containing result
		*
		* @param possiblePrimes
		* @return
		*/
	private def handleComputationResult(possiblePrimes: Try[immutable.Seq[Int]]) = {

    possiblePrimes match {
	    case Success(primeNumbers) =>
		    logger debug "Prime numbers between 1 and 100"
		    primeNumbers.foreach(logger debug _.toString)
      case Failure(err) => logger.error("Failed to generate a list of prime numbers", err)
    }
		
		logger debug "Terminating the actor system as the stream has exhausted ..."
    actorSystem terminate // When the stream finishes, terminate the actor system
  }


}
