package com.wixpress.academy.raft

import com.wixpress.academy.raft.ServerMode.ServerMode

object ServerMode extends Enumeration {
  type ServerMode = Value
  val Follower, Candidate, Leader = Value
}

case class RaftState(
                    me: ServerId,
                    peers: Array[RaftServiceGrpc.RaftServiceStub],
                    mode: ServerMode = ServerMode.Follower,

                    // persistent
                    currentTerm: Int = 0,
                    votedFor: Option[ServerId] = None,
                    log: Array[Entry] = Array.empty,

                    // volatile
                    commitIndex: EntryIndex = 0,
                    lastApplied: EntryIndex = 0,


                    // leader state
                    nextIndex: Map[ServerId, EntryIndex] = Map.empty,
                    matchIndex: Map[ServerId, EntryIndex] = Map.empty
                    )
