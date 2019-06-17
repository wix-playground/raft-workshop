package com.wixpress.academy.raft
import scala.concurrent.Future

class RaftServiceImpl(state: RaftState) extends RaftServiceGrpc.RaftService {
  override def appendEntries(request: AppendEntries.Request): Future[AppendEntries.Response] = ???

  override def requestVote(request: RequestVote.Request): Future[RequestVote.Response] = ???
}
