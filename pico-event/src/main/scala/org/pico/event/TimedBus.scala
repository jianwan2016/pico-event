package org.pico.event

import java.io.Closeable
import java.util.concurrent.atomic.AtomicReference

import org.pico.disposal.SimpleDisposer
import org.pico.disposal.std.autoCloseable._

import scala.concurrent.duration.{Deadline, Duration}

/** A bus which measures the duration of every publish call.
  */
trait TimedBus[A] extends Bus[A] with SimpleDisposer {
  val impl = this.swapDisposes(ClosedSubscribers, new AtomicReference(Subscribers[A, A](identity)))

  impl.get().disposes(this)

  override def publish(event: A): Unit = impl.get().publish(event)

  override def subscribe(subscriber: A => Unit): Closeable = impl.get().subscribe(subscriber)
}

object TimedBus {
  /** Create a bus which measures the duration of every publish call and publishes those durations
    * to the provided sink.
    */
  def apply[A](times: Sink[Duration]): Bus[A] = {
    new TimedBus[A] {
      override def publish(event: A): Unit = {
        val start = Deadline.now

        try {
          super.publish(event)
        } finally {
          times.publish(Deadline.now - start)
        }
      }
    }
  }
}
