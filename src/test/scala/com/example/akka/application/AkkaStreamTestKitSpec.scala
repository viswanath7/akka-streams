package com.example.akka.application

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, MustMatchers}
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, Future}
import scala.language.postfixOps

/** *
	* Akka Stream has a separate akka-stream-testkit module that provides tools specifically for writing stream tests.
	* This module comes with two main components that are TestSource and TestSink which provide sources and sinks
	* that materialize to probes that allow fluent API.
	*/
class AkkaStreamTestKitSpec extends TestKit(ActorSystem("test-actor-system"))
	with ImplicitSender with FlatSpecLike with BeforeAndAfterAll with MustMatchers {
	
	val logger = LoggerFactory getLogger "AkkaStreamTestKitSpec"
	implicit val flowMaterialiser = ActorMaterializer()
	
	override def afterAll {
		logger debug "Shutting down actor system ..."
		TestKit.shutdownActorSystem(system)
	}
	
	override def beforeAll {
		logger debug "Testing with akka-stream-testkit module ..."
	}
	
	"Prime number source" should "emit correct elements" in {
		logger debug "Configuring a source with stream of prime numbers ..."
		val source = StreamDefinition.source.via(StreamDefinition.flow)
		
		logger debug "Using TestSink to verify the elements from prime number source ..."
		source.filter(_ < 10)
			.runWith(TestSink.probe[Int])
			.request(5)
			.expectNext(1, 2, 3, 5, 7)
	}
	
	"Prime numbers counter sink" should "contain correct count" in {
		import scala.concurrent.duration._
		
		logger debug "Defining a sink that shall hold a count of prime numbers in the stream ..."
		val primesCounterSink: Sink[Int, Future[Int]] = StreamDefinition.flow
			.toMat(Sink.fold(0)((acc, _) => acc + 1))(Keep.right)
		
		logger debug "Connecting the TestSource with sink to verify sink's behaviour via its materialised value ..."
		val (publisher, eventualResult) = TestSource.probe[Int]
			.toMat(primesCounterSink)(Keep.both)
			.run()
		
		logger debug "Sending numbers 1 to 10 to test publisher so that the sink shall eventually have a materialised value ..."
		(1 to 10) foreach (number => {
			logger debug s"Sending $number to source probe ..."
			publisher.sendNext(number)
		})
		publisher.sendComplete()
		logger debug "Verifying if the count of prime numbers from 1 to 10 is 5 ..."
		Await.result(eventualResult, 10 seconds) mustBe 5
	}
	
}
