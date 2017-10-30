package com.example.akka.application

import java.nio.file.{Path, Paths}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Flow, Framing, Sink, Source}
import akka.util.ByteString
import org.slf4j.LoggerFactory

object MapReduceGraphApplication extends App {
	
	val logger = LoggerFactory getLogger GraphFlowApplication.getClass
	
	implicit val actorSystem: ActorSystem = ActorSystem("map-reduce-graph-actor-system")
	implicit val flowMaterialiser: ActorMaterializer = ActorMaterializer()
	import actorSystem.dispatcher
	
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
	
	fileSource
		.via(linesTransformer)
		.via(lineToWordsTransformer)
		.runWith(Sink.foreach(println))
		.andThen({ case _ => actorSystem.terminate() })
	
}
