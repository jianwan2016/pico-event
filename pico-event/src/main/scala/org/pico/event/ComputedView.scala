package org.pico.event

import java.util.concurrent.atomic.AtomicReference

@specialized(Boolean, Long, Double)
trait ComputedView[A] extends View[A] {
  private val ref = new AtomicReference[A](compute())

  /** Compute a value for the view.
    */
  def compute(): A

  /** Invalidate the view, which forces recomputation the next time value is called.
    */
  final def invalidate(): Unit = invalidations.invalidate()

  final override def value: A = {
    if (invalidations.valid) {
      ref.get()
    } else {
      invalidations.validate()
      val newValue = compute()
      ref.set(newValue)
      newValue
    }
  }

  /** A source that emits invalidation events.
    */
  final override val invalidations: Invalidations = Invalidations()
}
