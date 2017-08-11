package com.example.akka.application

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.language.postfixOps

/**
  * Prints prime numbers between 1 and 1000 using akka stream API
  */
object SampleStreamingApplication extends App {

  import actorSystem.dispatcher
  implicit val actorSystem = ActorSystem("StreamActorSystem")

  /**
    * Factory for stream execution engines that runs the streams.
    * Any 'run' method on a source requires a materialiser for its actual execution.
    *
    * Source is a description of execution like a blueprint;
    * which is intended to be reused and incorporated in a larger system.
    */
  implicit val materialiser = ActorMaterializer()

  /**
    *   A stream usually begins at a source; which in case happens to be a range of natural numbers from 1 to 100
    *
    *   The Source type is parametesized with two types: the first one is the type of element that this source emits
    *   and the second one may signal that running the source produces some auxiliary value (e.g. a network source may
    *   provide information about the bound port or the peer’s address). Where no auxiliary information is produced,
    *   the type akka.NotUsed is used—and a simple range of integers surely falls into this category.
    */
  val source: Source[Int, NotUsed] = Source(1 to 1000)

  // Define transformation(s) on the source by defining a flow
  val flow = Flow[Int] filter isPrime

  // Consumer function that prints numbers in the console
  val sink = Sink.foreach[Int](println)

  println("Prime numbers")
  source
    .via(flow)
    .runWith(sink)(materialiser)
    .onComplete(_ => actorSystem terminate) // When the stream finishes, terminate the actor system

  def isPrime(num: Int): Boolean = ! ((2 until num-1) exists (num % _ == 0))

}
