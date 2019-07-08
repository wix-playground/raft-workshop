package com.wixpress.academy.raft

import com.wixpress.academy.raft.env.ClusterFixtures
import org.scalatest.concurrent.Eventually
import org.scalatest.{FeatureSpec, GivenWhenThen, Matchers}
import org.scalatest.time._

class ElectionTest extends FeatureSpec with ClusterFixtures with GivenWhenThen with Eventually with Matchers {

  feature("Leader Election ") {
    ignore("After start at least one node become candidate") {
      withCluster(2) {
        (servers, _) =>
          eventually(timeout(Span(1, Seconds))) {
            servers.count(s => s.state.mode == ServerMode.Candidate && s.state.currentTerm > 0) should be >= 1
          }
      }
    }


    ignore("Exact One Leader get majority") {
      withCluster(5) {
        (servers, _) =>
          eventually(timeout(Span(5, Seconds))) {
            servers.count(_.state.mode == ServerMode.Leader) should be(1)
          }
      }
    }

    ignore("After successful election leader is not changing") {
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

    ignore("Leader was partitioned from cluster - new Leader elected") {
      withCluster(5) {
        (servers, network) =>
          eventually(timeout(Span(3, Seconds))) {
            servers.count(_.state.mode == ServerMode.Leader) should be(1)
          }

          val oldLeader = servers.find(_.state.mode == ServerMode.Leader).get

          Thread.sleep(3000)

          Given("Leader is fall out of cluster")

          val newNetwork = network.nodes.filter(_ != oldLeader.state.me)
          newNetwork.foreach(network.partitionOne(_, oldLeader.state.me))

          When("Cluster elected new Leader")

          eventually(timeout(Span(3, Seconds))) {
            servers.filter(s => newNetwork.contains(s.state.me))
              .count(_.state.mode == ServerMode.Leader) should be(1)
          }

          Then("After reconnection old leader should become follower")

          network.reconnectOne(oldLeader.state.me, newNetwork.toList.head)

          eventually(timeout(Span(2, Seconds))) {
            assert(oldLeader.state.mode == ServerMode.Follower)
          }
      }
    }

    ignore("Only node with majority of entries can be elected as Leader") {
      withCluster(5, autoStart = false) {
        (servers, _) =>
          servers(0).state.log = Array(Entry(term=1, index=1), Entry(term=3, index=3))
          servers(1).state.log = Array(Entry(term=1, index=1))
          servers(2).state.log = Array(Entry(term=1, index=1), Entry(term=3, index=3), Entry(term=5, index=5))
          servers(3).state.log = Array(Entry(term=1, index=1), Entry(term=2, index=2))
          servers(4).state.log = Array(Entry(term=1, index=1), Entry(term=4, index=4))

          servers.foreach(_.start())

          eventually(timeout(Span(5, Seconds))) {
            val leader = servers.find(_.state.mode == ServerMode.Leader)
            leader.map(_.state.me) should contain oneOf (servers(2).state.me, servers(4).state.me)
          }
      }
    }
  }
}
