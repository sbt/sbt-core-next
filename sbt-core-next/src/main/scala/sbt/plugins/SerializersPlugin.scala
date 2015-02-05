package sbt
package plugins

import sbt.serialization._
import SerializersKeys._

object SerializersPlugin extends AutoPlugin {
  override def trigger = AllRequirements
  override def requires = plugins.CorePlugin

  override val globalSettings: Seq[Setting[_]] = Seq(
    registeredProtocolConversions in Global <<= (registeredProtocolConversions in Global) ?? Nil,
    registeredSerializers in Global <<= (registeredSerializers in Global) ?? Nil)

  def registerTaskSerialization[T](key: TaskKey[T])(implicit pickler: Pickler[T], unpickler: Unpickler[T], mf: Manifest[T]): Setting[_] =
    registeredSerializers in Global += RegisteredSerializer(pickler, unpickler, mf)
  def registerSettingSerialization[T](key: SettingKey[T])(implicit pickler: Pickler[T], unpickler: Unpickler[T]): Setting[_] =
    registeredSerializers in Global += RegisteredSerializer(pickler, unpickler, key.key.manifest)
}
