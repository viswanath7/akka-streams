package com.example.akka.application

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, MustMatchers}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}


class PrimeNumbersStreamingApplicationSpec extends TestKit(ActorSystem("test-actor-system"))
  with ImplicitSender with FlatSpecLike with BeforeAndAfterAll with MustMatchers {
  
  val logger = LoggerFactory getLogger "PrimeNumbersStreamingApplicationSpec"
  
  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

  implicit val flowMaterialiser = ActorMaterializer()

  "Sink" should "return the correct results" in {
    val source = StreamDefinition.source
    val flow = StreamDefinition.flow
    val sink = StreamDefinition.sink

    logger debug "Testing sink containing a collection of prime numbers ..."
    
    val resultFuture = source.via(flow).runWith(sink)
    val computationResult = Await.result(resultFuture, 100 millis)

    computationResult must have size 26
    computationResult must contain allElementsOf List(1, 2, 3, 5, 7, 11)
  }


  "Source" should "contain correct elements" in {
    val source = StreamDefinition.source
    import system.dispatcher
  
    logger debug "Testing source generating numbers from 1 to 100 ..."
  
    /**
      * Sink that invokes the supplied fold function for every element it receives
      * Here the fold function counts the number of elements in the source stream
      */
    val counterSink: Sink[Int, Future[Int]] = Sink.fold(0)((a, b) => a + 1)
    
    source.runWith(counterSink).onComplete {
      case Success(numOfElements) => numOfElements mustBe 100
      case Failure(err) => fail(err)
    }
  }

  "Flow" should "do right the right filtering transformation" in {
    val flow = StreamDefinition.flow
    
    logger debug "Testing flow that filters for prime numbers ..."
  
    /**
      * Sink that invokes the supplied fold function for every element it receives
      * Here the fold function collects the elements into a sequence
      */
    val elementsCollectorSink: Sink[Int, Future[Seq[Int]]] = Sink.fold(Seq.empty[Int])(_ :+ _)
    
    val result = Source(1 to 10)
      .via(flow)
      .runWith(elementsCollectorSink)
    
    Await.result(result, 100.millis) must contain theSameElementsInOrderAs List(1, 2, 3, 5, 7)
  }


}
