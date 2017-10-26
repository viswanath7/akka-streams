package com.example.akka.application

import akka.actor.{ActorSystem, Cancellable}
import akka.pattern.pipe
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.example.akka.{Author, Tweet}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpecLike, MustMatchers}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.reflect._
import scala.util.Random

/**
	* As the akka stream implementation is based on actors,
	* one can utilise Akka test probe from Akka test kit API to test an akka stream set-up
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
		TestKit.shutdownActorSystem(system)
	}
	
	"With TestKit" should "test actor receive elements from the sink" in {
		logger debug "Testing source to supply a stream of tweets ..."
		val testProbe: TestProbe = TestProbe()
		val source = Source.fromIterator(() => sampleTweets.iterator)

		source.runWith(Sink.head)
			.pipeTo(testProbe.ref)
		
		testProbe.expectMsg(sampleTweets.head)
	}
	
	"Sink" should "receive messages from source" in {
		
		logger debug "Testing sink to receive supplied tweets ..."
		
		// searching for implicit value of ClassTag[Tweet] and assigning it to tweetClassTag
		val clazzTag = implicitly[reflect.ClassTag[Tweet]]
		
		logger debug "Creating source that shall periodically emit tweet"
		val source = Source.tick(initialDelay = 0 milliseconds, interval = 500 milliseconds, tick = (Random shuffle sampleTweets).head)
		
		val testProbe = TestProbe()
		
		logger debug "Creating sink that shall send elements of its stream to test probe"
		logger debug "Upon completion of the stream, the sink shall send 'completed' message to test probe"
		val sink = Sink.actorRef(testProbe.ref, onCompleteMessage = "completed")
		
		logger debug "Connecting source to sink and running it ..."
		val materialisedInstance: Cancellable = source.to(sink).run
		
		testProbe.expectMsgAnyOf(1 second, sampleTweets:_*)
		testProbe expectNoMsg (300 milliseconds)
		testProbe.expectMsgAnyOf(250 milliseconds, sampleTweets:_*)
		materialisedInstance.cancel()
		testProbe.expectMsg[String](300 milliseconds, "completed")
		testProbe.expectNoMsg()
	}
	
}
