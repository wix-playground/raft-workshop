package com.wixpress.academy.raft

import com.wixpress.academy.raft.ServerMode.ServerMode

object ServerMode extends Enumeration {
  type ServerMode = Value
  val Follower, Candidate, Leader = Value
}

case class RaftState(
                    me: ServerId,
                    peers: Array[RaftServiceGrpc.RaftServiceStub],
                    var mode: ServerMode = ServerMode.Follower,

                    // persistent
                    var currentTerm: TermIndex = 0,
                    var votedFor: Option[ServerId] = None,
                    var log: Array[Entry] = Array.empty,

                    // volatile
                    var commitIndex: EntryIndex = 0,
                    var lastApplied: EntryIndex = 0,

                    // leader state
                    var nextIndex: Map[ServerId, EntryIndex] = Map.empty,
                    var matchIndex: Map[ServerId, EntryIndex] = Map.empty
                    ) {
  def lastLogIndex: Long = if (log.nonEmpty) log.last.index else 0L
  def lastLogTerm: TermIndex = if (log.nonEmpty) log.last.term else 0L

}
