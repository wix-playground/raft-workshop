package com.wixpress.academy.raft

import org.scalatest.concurrent.Eventually
import org.scalatest.{FeatureSpec, GivenWhenThen, Matchers}
import org.scalatest.time._

class ElectionTest extends FeatureSpec with ClusterFixtures with GivenWhenThen with Eventually with Matchers {

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
