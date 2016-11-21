package java.lang

import scala.scalanative.native.Ptr
import scala.scalanative.runtime.{Info, InfoOps}

final class _Class[A](val info: Ptr[Info]) {
  def getName(): String = info.name

  override def hashCode: Int = info.cast[Long].##

  override def equals(other: Any): scala.Boolean = other match {
    case other: _Class[_] =>
      info == other.info
    case _ =>
      false
  }

  // TODO:
  def getInterfaces(): Array[_Class[_]] = ???
  def getSuperclass(): _Class[_]        = ???
  def getComponentType(): _Class[_]     = ???
  def isArray(): scala.Boolean          = ???
}

object _Class {
  private[java] implicit def _class2class[A](cls: _Class[A]): Class[A] =
    cls.asInstanceOf[Class[A]]
  private[java] implicit def class2_class[A](cls: Class[A]): _Class[A] =
    cls.asInstanceOf[_Class[A]]
}
