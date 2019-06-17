package com.wixpress.academy.raft.env

import com.wixpress.academy.raft.{AppendEntries, RequestVote, ServerId}
import com.wixpress.academy.raft.RaftServiceGrpc.RaftServiceStub
import io.grpc.{Channel, Status, StatusRuntimeException}

import scala.concurrent.Future

class RaftStubProxy(source: ServerId, destination: ServerId, network: VirtualNetwork, channel: Channel) extends RaftServiceStub(channel) {
  override def appendEntries(request: AppendEntries.Request): Future[AppendEntries.Response] = {
    if (network.isConnected(source, destination)) {
      super.appendEntries(request)
    } else {
      Future.failed(new StatusRuntimeException(Status.UNAVAILABLE))
    }

  }

  override def requestVote(request: RequestVote.Request): Future[RequestVote.Response] = {
    if (network.isConnected(source, destination)) {
      super.requestVote(request)
    } else {
      Future.failed(new StatusRuntimeException(Status.UNAVAILABLE))
    }

  }

}
