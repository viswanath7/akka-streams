package com.example.akka.application

import java.nio.file.{Path, Paths}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Balance, FileIO, Flow, Framing, GraphDSL, Keep, Merge, Sink, Source}
import akka.stream.{ActorMaterializer, Attributes, FlowShape, OverflowStrategy}
import akka.util.ByteString
import org.slf4j.LoggerFactory

import scala.collection.immutable.ListMap
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}


object MapReduceWorkersPoolGraphApplication extends App {
	
	val logger = LoggerFactory getLogger MapReduceWorkersPoolGraphApplication.getClass
	
	implicit val actorSystem: ActorSystem = ActorSystem("map-reduce-graph-actor-system")
	implicit val flowMaterialiser: ActorMaterializer = ActorMaterializer()
	
	private val inputFile = "src/main/resources/input.txt"
	
	logger debug "Defining a source from input file ..."
	val inputFilePath: Path = Paths get inputFile
	val fileSource = FileIO fromPath inputFilePath
	// 1024 elements are de-queued from source and stored locally in memory
	fileSource.buffer(1024, OverflowStrategy.backpressure)
	
	logger debug "Defining a flow to retrieve lines of the file ..."
	val linesTransformer: Flow[ByteString, String, NotUsed] = Framing.delimiter( ByteString("\n"),
		maximumFrameLength = 4096).map(_.utf8String.trim.toLowerCase)
	
	logger debug "Defining a flow to retrieve words of a line ..."
	val lineToWordsTransformer: Flow[String, String, NotUsed] = Flow[String]
		.flatMapConcat(line => Source.fromIterator[String](() => line.split("\\W+").toIterator))
		.filter(x => x.matches("[A-Za-z]+"))
	
	logger debug "Defining a flow that works in fork-join fashion to detect first alphabet of supplied words ..."
	val forkJoinTransformer: Flow[String, (Char, Int), NotUsed] = {
		logger debug "Defining a flow for worker nodes ..."
		def alphabetDetector = Flow[String].map(word => word.head -> 1)
			.addAttributes(Attributes.inputBuffer(initial = 1, max = 1024))
		// Set the internal buffer size for this flow segment
		balancer(3, alphabetDetector)
	}
	
	logger debug "Defining a flow to aggregate the results from worker nodes ..."
	val resultAggregator: Flow[(Char, Int), Map[Char, Int], NotUsed] = Flow[(Char,Int)]
		.fold( Map[Char, Int]() ) ((m, pair) => m + (pair._1 ->(m.getOrElse(pair._1, 0)+pair._2)) )
		// Set the internal buffer size for this flow segment
		.addAttributes(Attributes.inputBuffer(initial = 1, max = 8192))
		// Ensure that subscribers / consumers that block the channel for more than 2 seconds
		// are forcefully removed and their stream failed.
		.backpressureTimeout(2 seconds)
	
	logger debug "Defining a flow to sort the elements of result map by key ..."
	val sortResultFlow = Flow[Map[Char, Int]].map(immutableMap => ListMap(immutableMap.toSeq.sortBy(_._1):_*))
	
	logger debug "Defining a sink that shall retain single result"
	val sink = Flow[ListMap[Char, Int]].toMat(Sink.head)(Keep.right)
	
	import actorSystem.dispatcher
	
	fileSource
		.via(linesTransformer)
		.via(lineToWordsTransformer)
		.via(forkJoinTransformer)
		.via(resultAggregator)
		.via(sortResultFlow)
		.runWith(sink)(materializer = flowMaterialiser)
		.onComplete {
			case Success(sortedResult) =>
				println("Total number words in the file, listed by their first character")
				println(s"Number of unique alphabetical characters: ${sortedResult.size}")
				sortedResult.foreach( pair => println(s"${pair._1} -> ${pair._2}" ))
				logger debug "Terminating the actor system"
				actorSystem.terminate
			case Failure(f) => logger error s"Failed to process $inputFile to retrieve results"
				logger debug "Terminating the actor system"
				actorSystem.terminate
		}
	
	/**
		* Balances incoming stream of elements to a fixed pool of workers and merges the result from workers
		*
		*	@param workerCount  size of workers' pool to balance and handle the incoming load automatically
		* @param workerProcess  transformation that shall be performed by workers
		* @tparam In  worker flow input type
		* @tparam Out worker flow output type
		* @return flow that internally contains a pool of workers
		*/
	def balancer[In, Out](workerCount: Int, workerProcess: Flow[In, Out, Any]): Flow[In, Out, NotUsed] = {
		import GraphDSL.Implicits._
		Flow.fromGraph(GraphDSL.create() { implicit builder =>
			val balancer = builder.add(Balance[In](workerCount, waitForAllDownstreams = true))
			val merge = builder.add(Merge[Out](workerCount))
			for (_ <- 1 to workerCount) {
				// For each worker, add an edge from the balancer to the worker and then, wire it to the merge element
				// To make the worker stages run in parallel, we mark them as asynchronous with async.
				balancer ~> workerProcess.async ~> merge
			}
			FlowShape(balancer.in, merge.out)
		})
	}
	
}
