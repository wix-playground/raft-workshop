package com.wixpress.academy.raft

import com.google.protobuf.ByteString
import com.wixpress.academy.raft.env.ClusterFixtures
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{FeatureSpec, GivenWhenThen, Matchers}

class AgreementTest extends FeatureSpec with ClusterFixtures with GivenWhenThen with Eventually with Matchers {
  def push(server: RaftServer, message: String): Entry = {
    val entry = Entry(
      `type` = EntryType.DATA,
      term = server.state.currentTerm,
      index = server.state.lastLogIndex + 1,
      data = ByteString.copyFromUtf8(message)
    )

    server.state.log = server.state.log :+ entry
    entry
  }

  feature("Entries broadcasting & committing") {
    ignore("When entry's added to Leader it's being propagated to Followers") {
      withCluster(5) {
        (servers, _) =>

          eventually(timeout(Span(3, Seconds))) {
            servers.count(_.state.mode == ServerMode.Leader) should be(1)
          }

          val leader = servers.find(_.state.mode == ServerMode.Leader).get
          val entry = push(leader, "First Message")

          eventually(timeout(Span(3, Seconds))) {
            servers.count(s => s.state.lastLogIndex == entry.index) should equal (servers.length)
          }
      }
    }

    ignore("When node is partitioned & unsync with Leader it's being recovered after reconnect") {
      withCluster(5) {
        (servers, network) =>
          eventually(timeout(Span(3, Seconds))) {
            servers.count(_.state.mode == ServerMode.Leader) should be(1)
          }

          val leader = servers.find(_.state.mode == ServerMode.Leader).get

          push(leader, "1")
          push(leader, "2")

          val follower = servers.find(_.state.mode == ServerMode.Follower).get

          eventually(timeout(Span(2, Seconds))) {
            follower.state.log.map(_.data.toStringUtf8) should be(Array("1", "2"))
          }

          network.partitionAll(follower.state.me)

          push(leader, "3")
          push(leader, "4")

          network.reconnectAll(follower.state.me)

          eventually(timeout(Span(2, Seconds))) {
            follower.state.log.map(_.data.toStringUtf8) should be(Array("1", "2", "3", "4"))
          }
      }
    }

    ignore("When majority agrees on entries - they're being committed") {
      withCluster(5) {
        (servers, network) =>
          eventually(timeout(Span(3, Seconds))) {
            servers.count(_.state.mode == ServerMode.Leader) should be(1)
          }

          val leader = servers.find(_.state.mode == ServerMode.Leader).get

          push(leader, "1")
          push(leader, "2")

          Thread.sleep(1000)

          val last = push(leader, message = "3")

          eventually(timeout(Span(3, Seconds))) {
            servers.count(_.state.commitIndex == last.index) should be (5)
          }
      }
    }

    ignore("No agreement without majority") {
      withCluster(5) {
        (servers, network) =>
          eventually(timeout(Span(3, Seconds))) {
            servers.count(_.state.mode == ServerMode.Leader) should be(1)
          }

          val leader = servers.find(_.state.mode == ServerMode.Leader).get

          val followers = servers.filter(_.state.mode == ServerMode.Follower).take(3)
          followers.foreach(s => network.partitionAll(s.state.me))

          push(leader, "1")
          push(leader, "2")

          Thread.sleep(1000)

          servers.count(_.state.commitIndex == 0) should be (5)
          servers.count(_.state.lastLogIndex == 2) should be (2)
      }
    }

    ignore("After cluster healed - agreement recovered") {
      withCluster(5) {
        (servers, network) =>
          eventually(timeout(Span(3, Seconds))) {
            servers.count(_.state.mode == ServerMode.Leader) should be(1)
          }

          Given("Working cluster & some committed messages")

          val leader = servers.find(_.state.mode == ServerMode.Leader).get
          val followers: Array[RaftServer] = servers.filter(_.state.mode == ServerMode.Follower).take(3)

          push(leader, "1")
          push(leader, "2")

          eventually(timeout(Span(3, Seconds))) {
            servers.count(_.state.commitIndex == 2) should be(5)
          }

          When("Majority is lost")

          followers.foreach(_.stop())
          Thread.sleep(1000)

          And("New messages cannot be committed")

          push(leader, message = "3")
          push(leader, message = "4")

          servers.count(_.state.commitIndex == 2) should be(5)

          followers.foreach(_.start())

          Thread.sleep(1000)

          Then("After reconnecting and (possible) Leader re-election new agreement must be obtained")

          eventually(timeout(Span(3, Seconds))) {
            servers.count(_.state.mode == ServerMode.Leader) should be(1)
          }
          val leader2 = servers.find(_.state.mode == ServerMode.Leader).get
          push(leader2, message = "5")

          eventually(timeout(Span(3, Seconds))) {
            servers.count(_.state.commitIndex == 5) should be(5)
          }
      }
    }

  }
}
