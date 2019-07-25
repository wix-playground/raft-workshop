package com.wixpress.academy.raft

import akka.actor.{ActorRef, ActorSystem, Props}
import com.wixpress.academy.raft.utils.LogFormatterWithNodeColor
import io.grpc.{Server, ServerBuilder}
import wvlet.log.{LogSupport, Logger}

import scala.concurrent.ExecutionContext


class RaftServer(current: ServerId,
                 peers: Array[RaftServiceGrpc.RaftServiceStub],
                 port: Int) extends LogSupport{

  private[this] var server: Server = null
  private[this] var actor: ActorRef = null

  override lazy val logger = Logger(s"RaftServer-$current")
  logger.setFormatter(new LogFormatterWithNodeColor(current))

  var state = RaftState(
    me = current,
    peers = peers,
    nextIndex = (1 to peers.length).map(_ -> 0L).toMap,
    matchIndex = (1 to peers.length).map(_ -> 0L).toMap
  )

  def start(): Unit = {
    actor = RaftServer.actorSystem.actorOf(Props(classOf[RaftActor], state))
    val service = new RaftServiceImpl(actor)

    server = ServerBuilder.forPort(port).addService(RaftServiceGrpc.bindService(service, RaftServer.ec)).build.start

    info("Server started, listening on " + port)
  }

  def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }

  def stop(): Unit = {
    RaftServer.actorSystem.stop(actor)

    server.shutdownNow()
    server.awaitTermination()
  }
}


object RaftServer {
  implicit val ec: ExecutionContext = ExecutionContext.global

  val actorSystem = ActorSystem("RaftActorSystem")

  def main(args: Array[String]): Unit = {
    val server = new RaftServer(
      current = 0,
      peers = Array.empty[RaftServiceGrpc.RaftServiceStub],
      port = 5001)
    server.start()
    server.blockUntilShutdown()
  }
}
