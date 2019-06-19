package com.wixpress.academy.raft.utils

import wvlet.log.LogFormatter.{appendStackTrace, withColor}
import wvlet.log.{LogFormatter, LogRecord, LogTimestampFormatter}

class LogFormatterWithNodeColor(node: Int) extends LogFormatter {
  val colors = Array(
    Console.RED,
    Console.YELLOW,
    Console.CYAN,
    Console.GREEN,
    Console.MAGENTA,
    Console.RESET
  )

  override def formatLog(r: LogRecord): String = {
    val color = colors(node % colors.length)

    val log = s"[${LogTimestampFormatter.formatTimestampWithNoSpaace(r.getMillis)}][${withColor(color, r.leafLoggerName)}] ${withColor(color, r.getMessage)}"
    appendStackTrace(log, r)
  }
}
