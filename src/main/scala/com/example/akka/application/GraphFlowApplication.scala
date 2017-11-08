package com.example.akka.application

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, ClosedShape, IOResult}
import akka.util.ByteString
import akka.{Done, NotUsed}
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.language.{implicitConversions, postfixOps}

/**
	* Computation graphs model advanced processing structures;
	* typically when operations involving multiple inputs or outputs are involved.
	*
	* Each part of the graph is a GraphStage with a given Shape
	* where the most basic shapes being: SourceShape, FlowShape and SinkShape.
	*
	* This application demonstrates graph flow by broadcasting prime numbers output stream to log and file
	*/
object GraphFlowApplication extends App {
	
	val logger = LoggerFactory getLogger GraphFlowApplication.getClass
	
	implicit val actorSystem: ActorSystem = ActorSystem("graph-flow-actor-system")
	// For performance optimisation, Akka streams introduces a buffer for every asynchronous processing stage
	implicit val flowMaterialiser: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(actorSystem)
		.withInputBuffer(initialSize = 16, maxSize = 8192))
	
	private val outputFilename = "target/primes.txt"
	
	logger debug "This application demonstrates graph flow by broadcasting prime numbers output stream to log and file"
	
	logger debug "Defining a source that streams numbers from 1 to 1000 ..."
	val source = Source(1 to 1000)
	
	logger debug "Defining a flow that filters to retain only prime numbers ..."
	val primeNumbersFilteringFlow: Flow[Int, String, NotUsed] = Flow[Int].filter(StreamDefinition.isPrime).map(_.toString)
	
	logger debug "Defining a sink that prints elements streamed to it ..."
	val consoleLoggingSink: Sink[String, Future[Done]] = Flow[String].toMat(Sink foreach (logger debug))(Keep.right)
	
	logger debug s"Defining a sink that writes elements to file $outputFilename ..."
	val fileSink: Sink[String, Future[IOResult]] = Flow[String].map(prime =>ByteString(prime+"\n"))
																						.toMat(FileIO.toPath(Paths.get(outputFilename)))(Keep.right)
	
	logger debug "Defining a graph that shall broadcast prime numbers between 1 and 1000 ..."
	val graph = GraphDSL.create() { implicit builder =>
		import GraphDSL.Implicits._
		val broadcast = builder add Broadcast[String](2)
		
		// Mutable object GraphDSL.Builder is used implicitly by ~> operator
		
		source ~> primeNumbersFilteringFlow ~> broadcast ~> fileSink
		broadcast ~> consoleLoggingSink
		
		//  If all inputs and outputs are connected, the graph is no longer considered partial but a ClosedShape;
		// that can be used as an isolated component
		ClosedShape
	}
	
	
	logger debug "Executing the graph ..."
	RunnableGraph.fromGraph(graph).run()
	
	Thread sleep 1000
	logger debug "Terminating the actor system"
	
	actorSystem.terminate
}
