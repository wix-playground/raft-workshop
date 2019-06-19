package com.wixpress.academy.raft

import java.util.concurrent.TimeUnit

import scala.concurrent.Future
import scala.util.{Failure, Success}


trait LeaderFlow {
  this: RaftServiceImpl =>

  def heartbeat(): Unit = if (state.mode == ServerMode.Leader) {
    leaderTimer.reset()

    val calls = state.peers.map {
      conn =>
        conn.withDeadlineAfter(100, TimeUnit.MILLISECONDS).appendEntries(AppendEntries.Request(
          serverId = state.me,
          term = state.currentTerm
        )).map {
          resp => if (resp.term > state.currentTerm) Left(resp.term) else Right(resp.success)
        }.recover {
          case _ => Right(false)
        }
    }

    val zero: Either[TermIndex, Boolean] = Right(true)
    val successF: Future[Either[TermIndex, Boolean]] = Future.foldLeft(calls.toList)(zero) {
      (succ, resp) =>
        resp match {
          case Left(newTerm) => Left(newTerm)
          case Right(value) => succ.map(_ && value)
        }
    }

    successF onComplete {
      case Success(value) => value match {
        case Left(newTerm) => becomeFollower(Some(newTerm))
        case Right(_) => info(s"Node ${state.me} successfully sent heartbeats")
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
