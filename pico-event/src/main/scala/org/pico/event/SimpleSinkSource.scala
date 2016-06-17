package org.pico.event

import java.io.Closeable
import java.util.concurrent.atomic.AtomicReference

import org.pico.disposal.std.autoCloseable._

trait SimpleSinkSource[A, B] extends SinkSource[A, B] {
  val impl = this.swapDisposes(ClosedSinkSource, new AtomicReference(BareSinkSource[A, B](transform)))

  impl.get().disposes(this)

  def transform: A => B

  override def publish(event: A): Unit = impl.get().publish(event)

  override def subscribe(subscriber: B => Unit): Closeable = impl.get().subscribe(subscriber)
}

object SimpleSinkSource {
  def apply[A, B](f: A => B): SinkSource[A, B] = {
    new SimpleSinkSource[A, B] {
      override def transform: A => B = f
    }
  }
}