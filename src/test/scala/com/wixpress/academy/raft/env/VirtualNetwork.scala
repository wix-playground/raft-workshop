package com.wixpress.academy.raft.env

import com.google.common.graph.{MutableNetwork, NetworkBuilder}
import com.wixpress.academy.raft.ServerId

class VirtualNetwork(servers: Array[ServerId]) {
  private val graph: MutableNetwork[ServerId, String] = NetworkBuilder.undirected()
    .asInstanceOf[NetworkBuilder[ServerId, String]].build()

  servers.map(
    current => servers.filter(_ > current).map(other => graph.addEdge(current, other, s"$current-$other"))
  )

  def isConnected(left: ServerId, right: ServerId): Boolean = graph.hasEdgeConnecting(left, right)
}
