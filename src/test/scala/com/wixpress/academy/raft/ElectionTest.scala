package com.wixpress.academy.raft

import com.wixpress.academy.raft.env.{RaftStubProxy, VirtualNetwork}
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import org.scalatest.concurrent.Eventually
import org.scalatest.{FeatureSpec, GivenWhenThen, Matchers}
import org.scalatest.time._

class ElectionTest extends FeatureSpec with GivenWhenThen with Eventually with Matchers {

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


  feature("Leader Election ") {
    scenario("After start at least one node become candidate") {
      withCluster(2) {
        (servers, _) =>
          eventually(timeout(Span(1, Seconds))) {
            servers.count(s => s.state.mode == ServerMode.Candidate && s.state.currentTerm > 0) should be >= 1
          }
      }
    }


    scenario("Exact One Leader get majority") {
      withCluster(5) {
        (servers, _) =>
          eventually(timeout(Span(5, Seconds))) {
            servers.count(_.state.mode == ServerMode.Leader) should be(1)
          }
      }
    }

    scenario("After successful election leader is not changing") {
      withCluster(5) {
        (servers, _) =>
          eventually(timeout(Span(3, Seconds))) {
            servers.count(_.state.mode == ServerMode.Leader) should be(1)
          }

          val leader = servers.find(_.state.mode == ServerMode.Leader).get

          Thread.sleep(3000)

          assert(leader.state.mode == ServerMode.Leader)
      }
    }

    scenario("Leader was partitioned from cluster - new Leader elected") {
      withCluster(5) {
        (servers, network) =>
          eventually(timeout(Span(3, Seconds))) {
            servers.count(_.state.mode == ServerMode.Leader) should be(1)
          }

          val oldLeader = servers.find(_.state.mode == ServerMode.Leader).get

          Thread.sleep(3000)

          Given("Leader is fall out of cluster")

          val newNetwork = network.nodes.filter(_ != oldLeader.state.me)
          newNetwork.foreach(network.partition(_, oldLeader.state.me))

          When("Cluster elected new Leader")

          eventually(timeout(Span(3, Seconds))) {
            servers.filter(s => newNetwork.contains(s.state.me))
              .count(_.state.mode == ServerMode.Leader) should be(1)
          }

          Then("After reconnection old leader should become follower")

          network.reconnect(oldLeader.state.me, newNetwork.toList.head)

          eventually(timeout(Span(1, Seconds))) {
            assert(oldLeader.state.mode == ServerMode.Follower)
          }
      }
    }
  }
}
