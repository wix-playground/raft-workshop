package com.wixpress.academy.raft.unit

import com.wixpress.academy.raft.{Entry, RaftServer, RequestVote}
import com.wixpress.academy.raft.RaftServiceGrpc.RaftServiceBlockingStub
import io.grpc.ManagedChannelBuilder
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

class ElectionRestrictionTest extends FlatSpec with Matchers with BeforeAndAfter {
  val port = 5000
  var raft: RaftServer = null

  before {
    raft = new RaftServer(0, Array.empty, port)
    raft.start()
  }

  after {
    raft.stop()
  }


  val client = new RaftServiceBlockingStub(
    ManagedChannelBuilder.forAddress("localhost", port).usePlaintext.build
  )

  "Only RequestVote with not less term" can "be accepted" in {
    raft.state.currentTerm = 10
    client.requestVote(RequestVote.Request(term = 5)) should have ('granted (false))

    client.requestVote(RequestVote.Request(term = raft.state.currentTerm)) should have ('granted (true))
  }

  "Only RequestVote with up-to-date log" can "be accepted" in {
    raft.state.log = Array(Entry(index = 5, term = 3))

    val term = raft.state.currentTerm

    client.requestVote(
      RequestVote.Request(term = term, lastLogTerm = 2)) should have ('granted (false))
    client.requestVote(
      RequestVote.Request(term = term, lastLogTerm = 3, lastLogIndex=4)) should have ('granted (false))

    client.requestVote(
      RequestVote.Request(term = term, lastLogTerm = 3, lastLogIndex=5)) should have ('granted (true))
  }

  "Only node that haven't voted yet in this term" can "vote" in {
    raft.state.votedFor = Some(3)

    val term = raft.state.currentTerm

    client.requestVote(RequestVote.Request(term = term)) should have ('granted (false))

    client.requestVote(RequestVote.Request(term = term + 1, serverId = 3)) should have ('granted (true))

    client.requestVote(RequestVote.Request(term = term + 1, serverId = 2)) should have ('granted (false))
    client.requestVote(RequestVote.Request(term = term + 1, serverId = 3)) should have ('granted (true))
  }
}
