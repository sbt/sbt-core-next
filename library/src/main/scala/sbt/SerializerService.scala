package sbt

import sbt.serialization._

/**
 * Represents a Manifest/Serializer pair we can use
 *  to serialize task values + events later.
 */
sealed trait RegisteredSerializer {
  type T
  def manifest: Manifest[T]
  def serializer: Pickler[T] with Unpickler[T]
}
object RegisteredSerializer {
  def apply[U](implicit pickler: Pickler[U], unpickler: Unpickler[U], mf: Manifest[U]): RegisteredSerializer =
    new RegisteredSerializer {
      type T = U
      override val serializer = PicklerUnpickler[U](pickler, unpickler)
      override val manifest = mf
    }
}
/**
 * Represents a dynamic type conversion to be applied
 * prior to selecting a RegisteredSerializer when sending over
 * the wire protocol.
 */
sealed trait RegisteredProtocolConversion {
  type From
  type To
  def fromManifest: Manifest[From]
  def toManifest: Manifest[To]
  def convert(from: From): To
}
object RegisteredProtocolConversion {
  def apply[F, T](convert: F => T)(implicit fromMf: Manifest[F], toMf: Manifest[T]): RegisteredProtocolConversion =
    new RegisteredProtocolConversion {
      override type From = F
      override type To = T
      override def fromManifest = fromMf
      override def toManifest = toMf
      override def convert(from: F): T = convert(from)
    }
}

object SerializerServiceKeys {
  val registeredProtocolConversions = settingKey[Seq[RegisteredProtocolConversion]]("Conversions to apply before serializing task results.")
  val registeredSerializers = settingKey[Seq[RegisteredSerializer]]("All the serializers needed for task result values.")
}
