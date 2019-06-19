package com.wixpress.academy.raft

import com.wixpress.academy.raft.utils.{LogFormatterWithNodeColor, Timer}
import wvlet.log.{LogSupport, Logger}

import scala.concurrent.{ExecutionContext, Future}


class RaftServiceImpl(val state: RaftState) extends RaftServiceGrpc.RaftService
  with LogSupport with FollowerFlow with LeaderFlow {

  override lazy val logger = Logger(s"RaftService-${state.me}")
  logger.setFormatter(new LogFormatterWithNodeColor(state.me))

  val electionTimer = new Timer(startElection, scala.util.Random.nextInt(500) + 500)
  val leaderTimer = new Timer(heartbeat, 100)

  electionTimer.start()

  implicit val ec: ExecutionContext = ExecutionContext.global

  override def appendEntries(request: AppendEntries.Request): Future[AppendEntries.Response] = {
    if (request.term >= state.currentTerm) {
      electionTimer.reset()
    }

    info(s"Node ${state.me} (term=${state.currentTerm}) received AppendEntries from ${request.serverId}")

    if (request.term >= state.currentTerm) {
      becomeFollower(Some(request.term))
    }

    val prevLog = state.log.find(e => e.term == request.prevLogTerm && e.index == request.prevLogIndex)

    if (request.prevLogIndex > 0 && prevLog.isEmpty) {
      Future.successful(AppendEntries.Response(term = state.currentTerm, success = false))
    }else{
      val keep = state.log.takeWhile(e => e.term <= request.prevLogTerm && e.index <= request.prevLogIndex)
      state.log = keep ++ request.entries

      Future.successful(AppendEntries.Response(term = state.currentTerm, success = true))
    }


  }

  override def requestVote(request: RequestVote.Request): Future[RequestVote.Response] = {
    if (request.term > state.currentTerm) {
      becomeFollower(Some(request.term))
    }

    this.synchronized {
      if (request.term < state.currentTerm || state.votedFor.exists(_ != request.serverId) ||
        request.lastLogIndex < state.lastLogIndex || request.lastLogTerm < state.lastLogTerm) {

        Future.successful(RequestVote.Response(term = state.currentTerm, granted = false))
      } else {
        state.votedFor = Some(request.serverId.toInt)

        Future.successful(RequestVote.Response(term = state.currentTerm, granted = true))
      }
    }
  }


  def stopTimers(): Unit = {
    electionTimer.cancel(kill=true)
    leaderTimer.cancel(kill=true)
  }

}
