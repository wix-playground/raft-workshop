package com.wixpress.academy.raft

import akka.actor.ActorRef
import akka.util.Timeout
import akka.pattern.ask


import scala.concurrent.{Future}
import scala.concurrent.duration._


class RaftServiceImpl(actor: ActorRef) extends RaftServiceGrpc.RaftService {
  implicit val timeout = Timeout(5 seconds)

  override def appendEntries(request: AppendEntries.Request): Future[AppendEntries.Response] = {
    val f = actor ? RaftActor.AppendEntriesMsg(request)
    f.asInstanceOf[Future[AppendEntries.Response]]
  }

  override def requestVote(request: RequestVote.Request): Future[RequestVote.Response] = {
    val f = actor ? RaftActor.RequestVoteMsg(request)
    f.asInstanceOf[Future[RequestVote.Response]]
  }
}
