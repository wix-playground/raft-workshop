package com.wixpress.academy.raft

import akka.actor.{Actor, Timers}
import com.wixpress.academy.raft.utils.LogFormatterWithNodeColor
import wvlet.log.{LogSupport, Logger}

import scala.concurrent.ExecutionContext.Implicits.global


object RaftActor {

  case class AppendEntriesMsg(request: AppendEntries.Request)

  case class RequestVoteMsg(request: RequestVote.Request)

  case class ElectionMsg()
  case class HeartBeatMsg()

  case object ElectionTickKey
  case object HeartBeatKey

  type ElectionResult = Either[TermIndex, Integer]
  type HeartbeatResult = Either[TermIndex, List[(ServerId, EntryIndex)]]

  case class ElectionCompleteMsg(result: ElectionResult)
  case class HeartbeatCompleteMsg(result: HeartbeatResult)
}

class RaftActor(val state: RaftState) extends Actor with Timers with LogSupport {

  override lazy val logger = Logger(s"RaftActor-${state.me}")
  logger.setFormatter(new LogFormatterWithNodeColor(state.me))
  
  override def receive: Receive = ???
}
