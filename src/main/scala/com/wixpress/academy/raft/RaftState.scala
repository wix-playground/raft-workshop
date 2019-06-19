package com.wixpress.academy.raft

import com.wixpress.academy.raft.ServerMode.ServerMode

object ServerMode extends Enumeration {
  type ServerMode = Value
  val Follower, Candidate, Leader = Value
}

case class RaftState(
                    me: ServerId,
                    peers: Array[RaftServiceGrpc.RaftServiceStub],
                    @volatile var mode: ServerMode = ServerMode.Follower,

                    // persistent
                    @volatile var currentTerm: TermIndex = 0,
                    @volatile var votedFor: Option[ServerId] = None,
                    log: Array[Entry] = Array.empty,

                    // volatile
                    var commitIndex: EntryIndex = 0,
                    var lastApplied: EntryIndex = 0,


                    // leader state
                    var nextIndex: Map[ServerId, EntryIndex] = Map.empty,
                    var matchIndex: Map[ServerId, EntryIndex] = Map.empty
                    ) {
  def lastLogIndex: Long = if (log.length > 0) log.last.index else 0L
  def lastLogTerm: TermIndex = if (log.length > 0) log.last.term else 0L

  def updateTerm(newTerm: TermIndex): Unit = {
    currentTerm = newTerm
    mode = ServerMode.Follower
    votedFor = None
  }

}
