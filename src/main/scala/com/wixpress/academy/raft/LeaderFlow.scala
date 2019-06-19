package com.wixpress.academy.raft

import java.util.concurrent.TimeUnit

import scala.concurrent.Future
import scala.util.{Failure, Success}


trait LeaderFlow {
  this: RaftServiceImpl =>

  def heartbeat(): Unit = if (state.mode == ServerMode.Leader) {
    info(s"Node ${state.me} is about to sent hearbeats ${state.mode} - ${state.currentTerm}")

    leaderTimer.reset()

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
          prevLogTerm = prevLogTerm
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

    val zero: Either[TermIndex, List[(ServerId, EntryIndex)]] = Right(List.empty)
    val successF: Future[Either[TermIndex, List[(ServerId, EntryIndex)]]] = Future.foldLeft(calls.toList)(zero) {
      (acc, resp) =>
        resp.flatMap(value => acc.map(_ :+ value))
    }

    successF onComplete {
      case Success(value) => value match {
        case Left(newTerm) => becomeFollower(Some(newTerm))
        case Right(nextIndexes) =>
          info(s"Node ${state.me} successfully sent heartbeats (term=${state.currentTerm})")
          state.nextIndex = state.nextIndex ++ nextIndexes
      }
      case Failure(exception) => error(exception)
    }
  }


  def becomeLeader(): Unit = {
    info(s"Node ${state.me} became Leader")

    state.mode = ServerMode.Leader
    electionTimer.cancel()
    leaderTimer.start()

    heartbeat()
  }
}
