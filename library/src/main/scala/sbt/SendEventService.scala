package sbt

import sbt.serialization._

sealed trait SendEventService {
  /** Sends an event out to all registered event listeners. */
  def sendEvent[T: Pickler](event: T): Unit
}

object SendEventServiceKeys {
  // TODO create a separate kind of key to lookup services separately from tasks
  val sendEventService = taskKey[SendEventService]("Service used to send events to the current user interface(s).")
}

private[sbt] trait SbtPrivateSendEventService extends SendEventService
