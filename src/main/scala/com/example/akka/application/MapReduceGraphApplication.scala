package com.example.akka.application

import java.nio.file.{Path, Paths}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Balance, FileIO, Flow, Framing, GraphDSL, Keep, Merge, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.util.ByteString
import org.slf4j.LoggerFactory

import scala.collection.immutable.ListMap

object MapReduceGraphApplication extends App {
	
	val logger = LoggerFactory getLogger GraphFlowApplication.getClass
	
	implicit val actorSystem: ActorSystem = ActorSystem("map-reduce-graph-actor-system")
	implicit val flowMaterialiser: ActorMaterializer = ActorMaterializer()
	
	private val inputFile = "src/main/resources/input.txt"
	
	logger debug "Defining a source from input file ..."
	val inputFilePath: Path = Paths get inputFile
	val fileSource = FileIO fromPath inputFilePath
	
	logger debug "Defining a flow to retrieve lines of the file ..."
	val linesTransformer: Flow[ByteString, String, NotUsed] = Framing.delimiter( ByteString("\n"),
		maximumFrameLength = 4096).map(_.utf8String.trim.toLowerCase)
	
	logger debug "Defining a flow to retrieve words of a line ..."
	val lineToWordsTransformer: Flow[String, String, NotUsed] = Flow[String]
		.flatMapConcat(line => Source.fromIterator[String](() => line.split("\\W+").toIterator))
		.filter(x => x.matches("[A-Za-z]+"))
	
	logger debug "Defining a flow to sort the elements of result map by key ..."
	val sortResultFlow = Flow[Map[Char, Int]].map(immutableMap => ListMap(immutableMap.toSeq.sortBy(_._1):_*))
	
	val printSink = Flow[ListMap[Char, Int]].map({ sortedResult =>
		println (s"Total number words in the file, listed by their first character:")
		println(s"Number of unique alphabetical characters: ${sortedResult.size}")
		sortedResult.foreach( pair => println(s"${pair._1} -> ${pair._2}" ))
		sortedResult
	}).toMat(Sink.head)(Keep.right)
	
	logger debug "Defining a graph that shall do map reduce ..."
	
	val graph = GraphDSL.create() { implicit builder =>
		import GraphDSL.Implicits._
		val balance = builder add Balance[String](2)
		val merge = builder add Merge[(Char, Int)](2)
		
		val alphabetDetector1, alphabetDetector2 = Flow[String].map(word => word.head -> 1)
		
		val aggregator: Flow[(Char, Int), Map[Char, Int], NotUsed] = Flow[(Char,Int)]
			.fold( Map[Char, Int]() ) ((m, pair) => m + (pair._1 ->(m.getOrElse(pair._1, 0)+pair._2)) )
		
		fileSource ~> linesTransformer ~> lineToWordsTransformer ~> balance ~> alphabetDetector1 ~> merge ~> aggregator ~> sortResultFlow ~> printSink
																																balance ~> alphabetDetector2 ~> merge
		ClosedShape
	}
	
	logger debug "Executing the graph ..."
	RunnableGraph.fromGraph(graph).run()
	
	Thread sleep 1000
	logger debug "Terminating the actor system"
	actorSystem.terminate
}
