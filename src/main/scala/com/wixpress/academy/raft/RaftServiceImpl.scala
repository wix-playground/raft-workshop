package com.wixpress.academy.raft

import akka.actor.ActorRef
import akka.util.Timeout
import akka.pattern.ask
import com.wixpress.academy.raft.RaftActor.{AppendEntriesMsg, RequestVoteMsg}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


class RaftServiceImpl(val raft: ActorRef) extends RaftServiceGrpc.RaftService {
  implicit val timeout = Timeout(1 seconds)

  override def appendEntries(request: AppendEntries.Request): Future[AppendEntries.Response] = {
    (raft ? AppendEntriesMsg(request)).map {
      case v: AppendEntries.Response => v
    }
  }
  override def requestVote(request: RequestVote.Request): Future[RequestVote.Response] = {
    (raft ? RequestVoteMsg(request)).map {
      case v: RequestVote.Response => v
    }
  }
}
