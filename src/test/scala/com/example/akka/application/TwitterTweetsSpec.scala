package com.example.akka.application

import akka.actor.ActorSystem
import akka.pattern.pipe
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.example.akka.{Author, Tweet}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpecLike, MustMatchers}
import org.slf4j.LoggerFactory

class TwitterTweetsSpec extends TestKit(ActorSystem("test-actor-system"))
	with ImplicitSender with FlatSpecLike with BeforeAndAfterAll with BeforeAndAfter with MustMatchers {
	
	val logger = LoggerFactory getLogger "TwitterTweetsSpec"
	
	import system.dispatcher
	
	implicit val flowMaterialiser = ActorMaterializer()
	
	val sampleTweets = List(Tweet(Author("Carina Kozmus"), "Any #Scala companies in #Porto on the lookout for developers? Asking for a friend. RTs welcome."),
		Tweet(Author("Sean Glover"), "@scala_sbt: sbt 1.0.0 is now available, ft #Scala 2.12 and Zinc 1! Thanks to all contributors and plugins! @lightbend"),
		Tweet(Author("Rintcius Blok"), "How to integrate Eta into your existing Scala projects? @eta_lang #scala #sbt"))
	
	before {
	}
	
	override def afterAll {
		TestKit.shutdownActorSystem(system)
	}
	
	
	"With TestKit" should "test actor receive elements from the sink" in {
		val testProbe: TestProbe = TestProbe()
		val source = Source.fromIterator(() => sampleTweets.iterator)
		source.runWith(Sink.head).pipeTo(testProbe.ref)
		testProbe.expectMsg(sampleTweets.head)
	}
	
}
