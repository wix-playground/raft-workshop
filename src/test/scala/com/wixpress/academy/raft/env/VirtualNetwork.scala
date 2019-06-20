package com.wixpress.academy.raft.env

import collection.JavaConverters._
import com.google.common.graph.{MutableNetwork, NetworkBuilder}
import com.wixpress.academy.raft.ServerId

class VirtualNetwork(servers: Array[ServerId]) {
  private val graph: MutableNetwork[ServerId, String] = NetworkBuilder.undirected()
    .asInstanceOf[NetworkBuilder[ServerId, String]].build()

  servers.map(
    current => servers.filter(_ > current).map(other => graph.addEdge(current, other, s"$current-$other"))
  )

  def nodes: scala.collection.mutable.Set[ServerId] = graph.nodes().asScala

  def isConnected(left: ServerId, right: ServerId): Boolean = graph.hasEdgeConnecting(left, right)

  def partitionOne(left: ServerId, right: ServerId): Unit = if (left < right)
    graph.removeEdge(s"$left-$right") else graph.removeEdge(s"$right-$left")
  def reconnectOne(left: ServerId, right: ServerId): Unit =
    graph.addEdge(left, right, if (left < right) s"$left-$right" else s"$right-$left")

  def partitionAll(server: ServerId): Unit = nodes.filter(_ != server).foreach(partitionOne(_, server))
  def reconnectAll(server: ServerId): Unit = nodes.filter(_ != server).foreach(reconnectOne(_, server))
}
