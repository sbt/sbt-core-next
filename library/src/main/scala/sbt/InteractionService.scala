package sbt

sealed trait InteractionService {
  /** Prompts the user for input, optionally with a mask for characters. */
  def readLine(prompt: String, mask: Boolean): Option[String]
  /** Ask the user to confirm something (yes or no) before continuing. */
  def confirm(msg: String): Boolean

  // TODO - Ask for input with autocomplete?
}

object InteractionServiceKeys {
  // TODO create a separate kind of key to lookup services separately from tasks
  val interactionService = taskKey[InteractionService]("Service used to ask for user input through the current user interface(s).")
}

private[sbt] trait SbtPrivateInteractionService extends InteractionService
