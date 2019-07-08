package com.wixpress.academy.raft.unit

import com.wixpress.academy.raft.{AppendEntries, Entry, RaftServer}
import com.wixpress.academy.raft.RaftServiceGrpc.RaftServiceBlockingStub
import io.grpc.ManagedChannelBuilder
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}


class AgreementRestrictionTest extends FlatSpec with Matchers with BeforeAndAfter {
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

  ignore can "Only requests with not less term be accepted" in {
    raft.state.currentTerm = 10

    client.appendEntries(AppendEntries.Request(term = 5)) should have ('success (false))

    client.appendEntries(AppendEntries.Request(term = 10)) should have ('success (true))
  }

  ignore can "Only requests with synced prevLog be processed" in {
    raft.state.log = Array(Entry(term = 2, index = 2), Entry(term = 3, index = 5))
    val term = raft.state.currentTerm

    client.appendEntries(
      AppendEntries.Request(term = term, prevLogTerm = 3, prevLogIndex = 6)) should have ('success (false))
    client.appendEntries(
      AppendEntries.Request(term = term, prevLogTerm = 3, prevLogIndex = 5)) should have ('success (true))
    client.appendEntries(
      AppendEntries.Request(term = term, prevLogTerm = 2, prevLogIndex = 2)) should have ('success (true))
  }

  ignore should "New entries be successfully appended (with possible overwrite)" in {
    val term = raft.state.currentTerm

    client.appendEntries(AppendEntries.Request(
      term = term,
      entries = Seq(Entry(term=2, index=2), Entry(term=2, index=3)))) should have ('success (true))

    raft.state.log should equal(Array(Entry(term=2, index=2), Entry(term=2, index=3)))

    client.appendEntries(AppendEntries.Request(
      term = term,
      prevLogTerm=2, prevLogIndex=2, commitIndex=6,
      entries = Seq(Entry(term=3, index=4), Entry(term=3, index=5)))) should have ('success (true))

    raft.state.log should equal(Array(Entry(term=2, index=2), Entry(term=3, index=4), Entry(term=3, index=5)))
    raft.state.commitIndex should be(5)
  }
}
