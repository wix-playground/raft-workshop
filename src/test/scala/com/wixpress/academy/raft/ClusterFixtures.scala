package com.wixpress.academy.raft

import com.wixpress.academy.raft.env.{RaftStubProxy, VirtualNetwork}
import io.grpc.{ManagedChannel, ManagedChannelBuilder}

trait ClusterFixtures {
  def withCluster(N: Int, startPort: Int = 5000)(testCode: (Array[RaftServer], VirtualNetwork) => Any): Unit = {
    val serverIds: Array[ServerId] = (1 to N).toArray
    val network = new VirtualNetwork(serverIds)

    def connectNodes(): Array[RaftServer] = {
      val ports = serverIds.map(idx => idx -> (startPort + idx)).toMap

      def getPeers(current: ServerId): Array[RaftServiceGrpc.RaftServiceStub] = {
        serverIds.filter(_ != current).map(id => new RaftStubProxy(
          current, id, network, ManagedChannelBuilder.forAddress("localhost", ports(id)).usePlaintext.build
        ))
      }

      serverIds.map { idx =>
        val server = new RaftServer(idx, getPeers(idx), ports(idx))
        server.start()
        server
      }
    }

    val servers: Array[RaftServer] = connectNodes()

    try {
      testCode(servers, network)
    } finally {
      servers.map(_.state.peers.map(_.getChannel.asInstanceOf[ManagedChannel].shutdownNow()))
      servers.map(_.stop())
    }
  }
}
