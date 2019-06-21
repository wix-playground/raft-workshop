package com.wixpress.academy.raft.unit

import com.wixpress.academy.raft.ServerId
import com.wixpress.academy.raft.env.VirtualNetwork
import org.scalatest.FlatSpec

class EnvTest extends FlatSpec{
  val serverIds: Array[ServerId] = (1 to 5).toArray

  val network = new VirtualNetwork(serverIds)

  "In newly created network all nodes" should "be connected" in {
    assert(network.isConnected(1, 2))
    assert(network.isConnected(2, 1))

    assert(network.isConnected(3, 5))
    assert(network.isConnected(2, 4))
  }

  "After partitioning some nodes" should "be unavailable to each other" in {
    network.partitionOne(1, 2)

    assert(!network.isConnected(1, 2))
    assert(!network.isConnected(2, 1))

    assert(network.isConnected(1, 3))
  }
}
