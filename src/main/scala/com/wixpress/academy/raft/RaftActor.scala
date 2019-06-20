package com.wixpress.academy.raft

import akka.actor.{Actor, Timers}
import com.wixpress.academy.raft.utils.LogFormatterWithNodeColor
import wvlet.log.{LogSupport, Logger}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._


object RaftActor {

  case class AppendEntriesMsg(request: AppendEntries.Request)

  case class RequestVoteMsg(request: RequestVote.Request)

  case class HeartbeatCompleteMsg(result: Either[TermIndex, List[(ServerId, TermIndex)]])

  case class ElectionCompleteMsg(result: Either[TermIndex, Integer])

  case class ElectionMsg()

  case class HeartbeatMsg()

  case object ElectionTickKey

  case object HearbeatTickKey

}

class RaftActor(val state: RaftState) extends Actor with Timers with LogSupport with FollowerFlow with LeaderFlow {
  import RaftActor._

  override lazy val logger = Logger(s"RaftService-${state.me}")
  logger.setFormatter(new LogFormatterWithNodeColor(state.me))

  implicit val ec: ExecutionContext = ExecutionContext.global

  timers.startPeriodicTimer(ElectionTickKey, ElectionMsg, (scala.util.Random.nextInt(500) + 500) millis)
  timers.startPeriodicTimer(HearbeatTickKey, HeartbeatMsg, 100 millis)

  def appendEntries(request: AppendEntries.Request): AppendEntries.Response = {
    if (request.term >= state.currentTerm) {
      timers.cancel(ElectionTickKey)
      timers.startPeriodicTimer(ElectionTickKey, ElectionMsg, (scala.util.Random.nextInt(500) + 500) millis)
    }

    info(s"Node ${state.me} (term=${state.currentTerm}) received AppendEntries from ${request.serverId}")

    if (request.term > state.currentTerm) {
      becomeFollower(Some(request.term))
    }

    val prevLog = state.log.find(e => e.term == request.prevLogTerm && e.index == request.prevLogIndex)

    if (request.prevLogIndex > 0 && prevLog.isEmpty) {
      AppendEntries.Response(term = state.currentTerm, success = false)
    } else {
      val keep = state.log.takeWhile(e => e.term <= request.prevLogTerm && e.index <= request.prevLogIndex)
      state.log = keep ++ request.entries

      AppendEntries.Response(term = state.currentTerm, success = true)
    }
  }

  def requestVote(request: RequestVote.Request): RequestVote.Response = {
    if (request.term > state.currentTerm) {
      becomeFollower(Some(request.term))
    }

    if (request.term < state.currentTerm || state.votedFor.exists(_ != request.serverId) ||
      request.lastLogIndex < state.lastLogIndex || request.lastLogTerm < state.lastLogTerm) {

      RequestVote.Response(term = state.currentTerm, granted = false)
    } else {
      state.votedFor = Some(request.serverId.toInt)

      RequestVote.Response(term = state.currentTerm, granted = true)
    }

  }

  override def receive = {
    case AppendEntriesMsg(request) => sender() ! appendEntries(request)
    case RequestVoteMsg(request) => sender() ! requestVote(request)

    case ElectionMsg => startElection()
    case ElectionCompleteMsg(result) => onElectionComplete(result)
  }
}
