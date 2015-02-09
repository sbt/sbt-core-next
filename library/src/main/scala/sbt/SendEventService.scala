package sbt

import sbt.serialization._

sealed trait SendEventService {
  /** Sends an event out to all registered event listeners. */
  def sendEvent[T: Pickler](event: T): Unit
}

object SendEventService {
  private val detachedKey = AttributeKey[SendEventService]("SendEventService that sends DetachedEvent")

  /**
   * To send a detached plugin event, do getDetached(state).foreach(_.sendEvent(event)).
   * This should never be done from inside a task or background job where a better
   * SendEventService is available already. "Detached" means not associated with a task
   * or job.
   */
  def getDetached(state: State): Option[SendEventService] = state get detachedKey
  /** To be called only by sbt server */
  private[sbt] def putDetached(state: State, service: SendEventService): State = state.put(detachedKey, service)
}

object SendEventServiceKeys {
  // TODO create a separate kind of key to lookup services separately from tasks
  val sendEventService = taskKey[SendEventService]("Service used to send events to the current user interface(s).")
}

private[sbt] trait SbtPrivateSendEventService extends SendEventService
