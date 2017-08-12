package com.example.akka.application

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, MustMatchers}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps


class PrimeNumbersStreamingApplicationSpec extends TestKit(ActorSystem("test-actor-system"))
  with ImplicitSender with FlatSpecLike with BeforeAndAfterAll with MustMatchers {

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


}
