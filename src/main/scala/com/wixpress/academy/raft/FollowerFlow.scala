package com.wixpress.academy.raft

import java.util.concurrent.TimeUnit

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait FollowerFlow {
  this: RaftServiceImpl =>

  def startElection(): Unit = {
    state.currentTerm += 1
    state.mode = ServerMode.Candidate
    state.votedFor = Some(state.me)

    info(s"Node ${state.me} initialized election with term ${state.currentTerm}")

    electionTimer.reset()

    val calls = state.peers.map {
      conn =>
        conn.withDeadlineAfter(100, TimeUnit.MILLISECONDS).requestVote(RequestVote.Request(
          state.me,
          state.currentTerm,
          state.lastLogTerm,
          state.lastLogIndex
        )).map {
          resp =>
            if (resp.term > state.currentTerm) Left(resp.term) else Right(resp.granted)
        }.recover {
          case err =>
            info(s"Error occurred while sending RequestVote $err")
            Right(false)
        }

    }.toList

    info(s"Node ${state.me} waiting responses ${calls.length}")

    val zero: Either[TermIndex, Integer] = Right(0)
    val resultF: Future[Either[TermIndex, Integer]] = Future.foldLeft(calls)(zero) {
      (votes, resp) =>
        resp match {
          case Left(term) => Left(term)
          case Right(granted) => votes.map(_ + (if (granted) 1 else 0))
        }
    }

    resultF onComplete {
      case Success(result) =>
        info(s"Node's ${state.me} election result is: ${result}")

        result match {
          case Left(term) => becomeFollower(Some(term))
          case Right(votes) if (state.peers.length / 2 < votes && state.mode == ServerMode.Candidate) => becomeLeader()
          case _ => None
        }
      case Failure(exception) => error(exception)
    }

  }

  def becomeFollower(newTerm: Option[TermIndex]): Unit = {
    info(s"Node ${state.me} became Follower")

    state.mode = ServerMode.Follower
    state.votedFor = None

    leaderTimer.cancel()
    electionTimer.start()

    newTerm match {
      case Some(term) => state.currentTerm = term
      case _ => None
    }
  }
}
