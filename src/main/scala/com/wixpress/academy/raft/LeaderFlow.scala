package com.wixpress.academy.raft

import java.util.concurrent.TimeUnit

import com.wixpress.academy.raft.RaftActor.{AppendEntriesMsg, HeartbeatCompleteMsg, HeartbeatMsg}

import scala.concurrent.Future
import scala.util.{Failure, Success}


trait LeaderFlow {
  this: RaftActor =>

  type HeartbeatResult = Either[TermIndex, List[(ServerId, EntryIndex)]]

  def heartbeat(): Unit = if (state.mode == ServerMode.Leader) {
    // info(s"Node ${state.me} is about to sent hearbeats ${state.mode} - ${state.currentTerm}")

    val calls = state.peers.zipWithIndex.map {
      case (conn, serverId: Int) =>
        val nextIndex: EntryIndex = state.nextIndex getOrElse(serverId, 0)
        val (prevLog, entries) = state.log.span(_.index < nextIndex)

        val prevLogIndex = if (prevLog.nonEmpty) prevLog.last.index else 0
        val prevLogTerm = if (prevLog.nonEmpty) prevLog.last.term else 0

        conn.withDeadlineAfter(100, TimeUnit.MILLISECONDS).appendEntries(AppendEntries.Request(
          serverId = state.me,
          term = state.currentTerm,
          entries = entries,
          prevLogIndex = prevLogIndex,
          prevLogTerm = prevLogTerm,
          commitIndex = state.commitIndex
        )).map {
          resp => {
            val nextIndexUpdate = if (entries.nonEmpty) {
              if (resp.success) entries.last.index else nextIndex - 1
            } else nextIndex

            if (resp.term > state.currentTerm) Left(resp.term) else Right(serverId -> nextIndexUpdate)
          }
        }.recover {
          case err =>
            info(s"Error occurred while sending heartbeat $err")
            Right(serverId -> nextIndex)
        }
    }

    val zero: HeartbeatResult = Right(List.empty)
    val successF: Future[HeartbeatResult] = Future.foldLeft(calls.toList)(zero) {
      (acc, resp) =>
        resp.flatMap(value => acc.map(_ :+ value))
    }

    successF onComplete {
      case Success(value) => self ! RaftActor.HeartbeatCompleteMsg(value)
      case Failure(exception) => error(exception)
    }
  }

  def onHertbeatComplete(result: HeartbeatResult): Unit = result match {
    case Left(newTerm) => becomeFollower(Some(newTerm))
    case Right(nextIndexes) =>
      info(s"Node ${state.me} successfully sent heartbeats (term=${state.currentTerm})")
      state.nextIndex = state.nextIndex ++ nextIndexes
      state.matchIndex = state.matchIndex ++ nextIndexes.filter {
        case (serverId: ServerId, index: TermIndex) => index > state.matchIndex.getOrElse(serverId, 0L) // increase only
      }

      val majorityIndex = majorityMin(state.matchIndex.values.toArray)
      val candidateTerm = state.log.find(_.index == majorityIndex).map(_.term)
      if (candidateTerm.contains(state.currentTerm) && majorityIndex > state.commitIndex) {
        info(s"Majority agreed - new commit $majorityIndex")
        state.commitIndex = majorityIndex
      }
  }

  def majorityMin(a: Array[Long]): Long = {
    a.sorted.drop(a.length / 2).head
  }

  def becomeLeader(): Unit = {
    info(s"Node ${state.me} became Leader")
    state.mode = ServerMode.Leader

    heartbeat()
    context.become(receiveLeader)
  }

  def receiveLeader: Receive = {
    case HeartbeatMsg => heartbeat()
    case HeartbeatCompleteMsg(result) => onHertbeatComplete(result)

    case AppendEntriesMsg(request) => sender() ! appendEntries(request)
  }
}
