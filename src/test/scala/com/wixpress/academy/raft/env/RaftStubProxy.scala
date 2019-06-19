package com.wixpress.academy.raft.env

import com.wixpress.academy.raft.{AppendEntries, RequestVote, ServerId}
import com.wixpress.academy.raft.RaftServiceGrpc.RaftServiceStub
import com.wixpress.academy.raft.utils.LogFormatterWithNodeColor
import io.grpc.{CallOptions, Channel, Status, StatusRuntimeException}
import wvlet.log.{LogSupport, Logger}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class RaftStubProxy(source: ServerId, destination: ServerId, network: VirtualNetwork,
                    channel: Channel, options: CallOptions = CallOptions.DEFAULT)
  extends RaftServiceStub(channel, options) with LogSupport {

  import RaftStubProxy._

  override lazy val logger = Logger(s"ClientProxy-$source")
  logger.setFormatter(new LogFormatterWithNodeColor(source))

  override def appendEntries(request: AppendEntries.Request): Future[AppendEntries.Response] = {
    info(s"Node $source is sending appendEntries to $destination")

    if (network.isConnected(source, destination)) {
      super.appendEntries(request)
    } else {
      Future.failed(new StatusRuntimeException(Status.UNAVAILABLE))
    }

  }

  override def requestVote(request: RequestVote.Request): Future[RequestVote.Response] = {
    //info(s"Node $source is sending requestVote to $destination")

    if (network.isConnected(source, destination)) {
      super.requestVote(request) andThen {
        case Success(resp) =>
          warn(s"Node $source received RequestVote.Response from $destination:" + resp.toString)
      }

    } else {
      Future.failed(new StatusRuntimeException(Status.UNAVAILABLE))
    }

  }

  override def build(channel: Channel, options: CallOptions): RaftServiceStub = new RaftStubProxy(
    source, destination, network, channel, options
  )

}



object RaftStubProxy {
  implicit val ec: ExecutionContext = ExecutionContext.global
}
