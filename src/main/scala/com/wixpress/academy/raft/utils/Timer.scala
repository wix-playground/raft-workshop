package com.wixpress.academy.raft.utils

import java.util.concurrent._

class Timer(target: () => Unit, delay: Long) {
  @volatile private var t: ScheduledFuture[_] = null
  @volatile private var version = 0L
  private var isCancelled: Boolean = false

  def start(): Unit = {
    val timerCycle = version

    t = Timer.ex.schedule(new Runnable() {
      def run() = if (timerCycle == version && !isCancelled) target()
    }, delay, TimeUnit.MILLISECONDS)
  }

  def cancel(kill: Boolean = false): Unit = {
    if (kill)
      isCancelled = true

    if (t != null)
      t.cancel(true)

  }

  def reset(): Unit = {
    version += 1

    cancel()
    start()
  }
}

object Timer {
  val ex = new ScheduledThreadPoolExecutor(8)
}
