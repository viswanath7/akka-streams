package com.example.akka.application

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Cancellable}
import akka.pattern.pipe
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.example.akka.{Author, Tweet}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpecLike, MustMatchers}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.reflect._
import scala.util.Random

/**
	* As the akka stream implementation is based on actors, one can utilise TestProbe from  the akka-testkit module to test
	*/
class TwitterTweetsSpec extends TestKit(ActorSystem("test-actor-system"))
	with ImplicitSender with FlatSpecLike with BeforeAndAfterAll with BeforeAndAfter with MustMatchers {
	
	val logger = LoggerFactory getLogger "TwitterTweetsSpec"
	
	import system.dispatcher
	
	implicit val flowMaterialiser = ActorMaterializer()
	implicit val tweetClassTag: ClassTag[Tweet] = classTag[Tweet]
	
	val sampleTweets = List(Tweet(Author("Carina Kozmus"), "Any #Scala companies in #Porto on the lookout for developers? Asking for a friend. RTs welcome."),
		Tweet(Author("Sean Glover"), "@scala_sbt: sbt 1.0.0 is now available, ft #Scala 2.12 and Zinc 1! Thanks to all contributors and plugins! @lightbend"),
		Tweet(Author("Rintcius Blok"), "How to integrate Eta into your existing Scala projects? @eta_lang #scala #sbt"))
	
	before {
	}
	
	override def afterAll {
		logger debug "Shutting down actor system ..."
		TestKit.shutdownActorSystem(system)
	}
	
	override def beforeAll {
		logger debug "Testing with akka test-kit ..."
	}
	
	"Source" should "supply a stream of tweets" in {
		logger debug "Testing source to supply a stream of tweets ..."
		val testProbe: TestProbe = TestProbe()
		val atomicSource = Source.fromIterator(() => sampleTweets.iterator)
		
		logger debug "Connecting source with sink and piping the elements from sink to test actor ..."
		atomicSource
			.runWith(Sink.head) // Only the first value received by the sink shall be materialised
			.pipeTo(testProbe.ref)
		
		testProbe.expectMsg(sampleTweets.head)
		testProbe.expectNoMsg()
	}
	
	"Sink" should "receive messages from source" in {
		
		logger debug "Testing sink to receive supplied tweets ..."
		
		// searching for implicit value of ClassTag[Tweet] and assigning it to tweetClassTag
		val clazzTag: ClassTag[Tweet] = implicitly[reflect.ClassTag[Tweet]]
		
		logger debug "Creating source that shall periodically emit tweet"
		val source = Source.tick(initialDelay = 0 milliseconds, interval = 500 milliseconds, tick = (Random shuffle sampleTweets).head)
		
		val testProbe = TestProbe()
		
		logger debug "Creating sink that shall send elements of its stream to test probe"
		logger debug "Upon completion of the stream, the sink shall send 'completed' message to test probe"
		val sink = Sink.actorRef(testProbe.ref, onCompleteMessage = "completed")
		
		logger debug "Connecting source to sink and running it ..."
		val materialisedInstance: Cancellable = source.to(sink).run
		
		testProbe.expectMsgAnyOf(1 second, sampleTweets: _*)
		testProbe expectNoMsg (300 milliseconds)
		testProbe.expectMsgAnyOf(250 milliseconds, sampleTweets: _*)
		materialisedInstance.cancel()
		testProbe.expectMsg[String](300 milliseconds, "completed")
		testProbe.expectNoMsg()
	}
	
	"With source actor, one " should " have control over tweets sent to processing stream" in {
		/*
		* Creating a source materialised as an actor where messages sent to this actor will be emitted to the stream,
		* if there is demand from downstream, otherwise they will be buffered until request for demand is received.
		*/
		logger debug "Defining source materialised as an actor with fixed buffer size of 10."
		logger debug "New elements arriving to source shall be dropped when its buffer is full"
		val source = Source actorRef(bufferSize = 10, overflowStrategy = OverflowStrategy.dropNew)
		
		logger debug "Defining a flow that extracts the body of the tweet ..."
		val flow: Flow[Tweet, String, NotUsed] = Flow[Tweet].map(_.body)
		
		logger debug "Connecting flow to sink that concatenates the elements it receives ..."
		/**
			* Note on usage of combining flow with sink.
			*
			* Use the method 'toMat' to materialise, so that one can specify what to retain as result (via the combine function) when connecting flow to a sink.
			* Materialised value of provided sink shall be materialised value of combined / concatenated process flow
			* The method 'to' retains materialised value of flow as the combined materialised value; which is undesirable in our case.
			*/
		val sink: Sink[Tweet, Future[String]] = flow.toMat(Sink.fold("")((accumulatedOutput, element) => accumulatedOutput + element + " | "))(Keep.right)
		
		val sourceActorResultPair: (ActorRef, Future[String]) = source.toMat(sink)(Keep.both).run()
		
		sourceActorResultPair._1 ! sampleTweets.head
		sourceActorResultPair._1 ! sampleTweets(1)
		sourceActorResultPair._1 ! akka.actor.Status.Success("done")
		
		Await.result(sourceActorResultPair._2, 500 milliseconds) mustBe
			"Any #Scala companies in #Porto on the lookout for developers? Asking for a friend. RTs welcome. | @scala_sbt: sbt 1.0.0 is now available, ft #Scala 2.12 and Zinc 1! Thanks to all contributors and plugins! @lightbend | "
	}
	
}
