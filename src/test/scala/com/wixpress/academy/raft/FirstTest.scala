package com.wixpress.academy.raft

import com.wixpress.academy.raft.env.{RaftStubProxy, VirtualNetwork}
import io.grpc.ManagedChannelBuilder
import org.scalatest.concurrent.Eventually
import org.scalatest.{FeatureSpec, GivenWhenThen, Matchers}
import org.scalatest.time._

class FirstTest extends FeatureSpec with GivenWhenThen with Eventually with Matchers {
  def initCluster(N: Int, startPort: Int = 5000): Array[RaftServer] = {
    val ports = (1 to N).map(idx => idx -> (startPort + idx)).toMap

    def getPeers(current: ServerId): Array[RaftServiceGrpc.RaftServiceStub] = {
      serverIds.map(id => new RaftStubProxy(
        current, id, network, ManagedChannelBuilder.forAddress("localhost", ports(id)).usePlaintext.build
      ))
    }

    (1 to N).map(idx => new RaftServer(idx, getPeers(idx), ports(idx))).toArray
  }

  val N = 5
  val serverIds: Array[ServerId] = (1 to N).toArray
  val network = new VirtualNetwork(serverIds)
  val servers: Array[RaftServer] = initCluster(N)

  feature("Leader Election "){
    scenario("Exact One Leader After Start") {
      eventually (timeout(Span(10, Seconds))) {
        servers.count(_.state.mode == ServerMode.Leader) should be (1)
      }
    }
  }
}
