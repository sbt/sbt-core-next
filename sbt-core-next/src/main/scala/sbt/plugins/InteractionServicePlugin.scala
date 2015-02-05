package sbt
package plugins

import InteractionServiceKeys._

/**
 * This plugin provides the basic settings used by plugins that want to be able to communicate with a UI.
 *
 * Basically, we just stub out the setting you can use to look up the current UI context.
 */
object InteractionServicePlugin extends AutoPlugin {
  override def trigger = AllRequirements
  override def requires = plugins.CorePlugin

  override val globalSettings: Seq[Setting[_]] = Seq(
    interactionService in Global <<= (interactionService in Global) ?? CommandLineUIServices)
}
