package org.pico.event.syntax

import org.pico.disposal.std.autoCloseable._
import org.pico.event.{Live, SimpleBus, Sink, Source}

package object source {
  implicit class SourceOps_hJob2ex[A](val self: Source[A]) extends AnyVal {
    /** Create a live value with an initial value, which will have the latest value that was
      * emitted by the event source.
      *
      * @param initial The initial value
      * @return The live value that will change to contain the latest value emitted by the source
      */
    def latest(initial: A): Live[A] = self.foldRight(initial)((v, _) => v)

    /** Create a live value that counts the number of events that have been emitted.
      *
      * @return The live value that will change to contain the latest value emitted by the source
      */
    def eventCount: Live[Long] = self.foldRight(0L)((_, v) => v + 1)
  }

  implicit class SourceOps_KhVNHpu[A, B](val self: Source[Either[A, B]]) extends AnyVal {
    /** Divert values on the left of emitted events by the source into the provided sink.
      *
      * Values on the right of the event will be emitted by the returned source.
      *
      * @param sink The sink to which left side of emitted events will be published
      * @return The source that emits the right side of emitted events
      */
    def divertLeft(sink: Sink[A]): Source[B] = {
      new SimpleBus[B] { temp =>
        temp += self.subscribe {
          case Right(rt) => temp.publish(rt)
          case Left(lt) => sink.publish(lt)
        }
      }
    }

    /** Divert values on the right of emitted events by the source into the provided sink.
      *
      * Values on the left of the event will be emitted by the returned source.
      *
      * @param sink The sink to which right side of emitted events will be published
      * @return The source that emits the left side of emitted events
      */
    def divertRight(sink: Sink[B]): Source[A] = {
      new SimpleBus[A] { temp =>
        temp += self.subscribe {
          case Right(rt) => sink.publish(rt)
          case Left(lt) => temp.publish(lt)
        }
      }
    }
  }
}
