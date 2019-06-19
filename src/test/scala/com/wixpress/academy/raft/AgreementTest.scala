package com.wixpress.academy.raft

import com.google.protobuf.ByteString
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

  feature("Entries broadcasting") {
    scenario("When entry added to Leader it's being sent to Followers") {
      withCluster(5) {
        (servers, _) =>

          eventually(timeout(Span(5, Seconds))) {
            servers.count(_.state.mode == ServerMode.Leader) should be(1)
          }

          val leader = servers.find(_.state.mode == ServerMode.Leader).get
          val entry = push(leader, "First Message")

          eventually(timeout(Span(5, Seconds))) {
            servers.count(s => s.state.lastLogIndex == entry.index) should equal (servers.length)
          }

          println("test")
      }
    }
  }
}
