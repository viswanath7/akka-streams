package com.example.akka.application

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, MustMatchers}
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._
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

    val resultFuture = source.via(flow).runWith(sink)
    val computationResult = Await.result(resultFuture, 100 millis)

    computationResult must have size 26
    computationResult must contain allElementsOf List(1, 2, 3, 5, 7, 11)
  }


  "Source" should "contain correct elements" in {
    val source = StreamDefinition.source
    import system.dispatcher

    source.runWith(Sink.fold(0)((a, b) => a + 1)).onComplete {
      case Success(numOfElements) => numOfElements mustBe 100
      case Failure(err) => fail(err)
    }
  }

  "Flow" should "do right the right filtering transformation" in {
    val flow = StreamDefinition.flow
    val result = Source(1 to 10).via(flow).runWith(Sink.fold(Seq.empty[Int])(_ :+ _))
    Await.result(result, 100.millis) must contain theSameElementsInOrderAs List(1, 2, 3, 5, 7)
  }


}
