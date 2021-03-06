package scala.scalanative
package linker

import java.io.{File, PrintWriter}
import scala.collection.mutable
import nir._
import nir.serialization._
import nir.Shows._
import util.sh

sealed trait Linker {

  /** Link the whole world under closed world assumption. */
  def link(entries: Seq[Global]): (Seq[Global], Seq[Attr.Link], Seq[Defn])
}

object Linker {

  /** Create a new linker given tools configuration. */
  def apply(config: tools.Config): Linker = new Impl(config)

  private final class Impl(config: tools.Config) extends Linker {
    private def load(
        global: Global): Option[(Seq[Dep], Seq[Attr.Link], Defn)] =
      config.paths.collectFirst {
        case path if path.contains(global) =>
          path.load(global)
      }.flatten

    def link(entries: Seq[Global]): (Seq[Global], Seq[Attr.Link], Seq[Defn]) = {
      val resolved    = mutable.Set.empty[Global]
      val unresolved  = mutable.Set.empty[Global]
      val links       = mutable.Set.empty[Attr.Link]
      val defns       = mutable.UnrolledBuffer.empty[Defn]
      val direct      = mutable.Stack.empty[Global]
      var conditional = mutable.UnrolledBuffer.empty[Dep.Conditional]

      def processDirect =
        while (direct.nonEmpty) {
          val workitem = direct.pop()

          if (!workitem.isIntrinsic && !resolved.contains(workitem) &&
              !unresolved.contains(workitem)) {

            load(workitem).fold[Unit] {
              unresolved += workitem
            } {
              case (deps, newlinks, defn) =>
                resolved += workitem
                defns += defn
                links ++= newlinks

                deps.foreach {
                  case Dep.Direct(dep) =>
                    direct.push(dep)

                  case cond: Dep.Conditional =>
                    conditional += cond
                }
            }
          }
        }

      def processConditional = {
        val rest = mutable.UnrolledBuffer.empty[Dep.Conditional]

        conditional.foreach {
          case Dep.Conditional(dep, cond)
              if resolved.contains(dep) || unresolved.contains(dep) =>
            ()

          case Dep.Conditional(dep, cond) if resolved.contains(cond) =>
            direct.push(dep)

          case dep =>
            rest += dep
        }

        conditional = rest
      }

      direct.pushAll(entries)
      while (direct.nonEmpty) {
        processDirect
        processConditional
      }

      config.paths.foreach(_.close)

      (unresolved.toSeq, links.toSeq, defns.sortBy(_.name.toString).toSeq)
    }
  }
}
