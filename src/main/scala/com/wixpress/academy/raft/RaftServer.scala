package com.wixpress.academy.raft

import java.util.logging.Logger

import io.grpc.{ServerBuilder, Server}

import scala.concurrent.ExecutionContext


object RaftServer {
  private val logger = Logger.getLogger(classOf[RaftServer].getName)
  def main(args: Array[String]): Unit = {
    val server = new RaftServer(
      current=0,
      peers=Array.empty[RaftServiceGrpc.RaftServiceStub],
      port=5001,
      ExecutionContext.global)
    server.start()
    server.blockUntilShutdown()
  }
}

class RaftServer(current: ServerId,
                 peers: Array[RaftServiceGrpc.RaftServiceStub],
                 port: Int,
                 executionContext: ExecutionContext = ExecutionContext.global) {
  private[this] var server: Server = null

  val state = RaftState(
    me = current,
    peers = peers
  )

  private def start(): Unit = {
    val r = RaftServiceGrpc.bindService(new RaftServiceImpl(state), executionContext)

    server = ServerBuilder.forPort(port).addService(r).build.start

    RaftServer.logger.info("Server started, listening on " + port)

  }

  private def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }
}
