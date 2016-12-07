package org.pico.event

import java.util.concurrent.atomic.AtomicReference

import org.pico.atomic.syntax.std.atomicReference._
import org.pico.disposal.std.autoCloseable._

/** A view that allows writes with compare-and-set semantics.  All successful writes will invalidate
  * the view.
  */
@specialized(Boolean, Long, Double)
trait Cell[A] extends View[A] {
  /** Write a value into the cell.
    */
  def value_=(that: A): Unit

  /** Atomically update a value with the provided transformation function, retrying as necessary.
    * The transformation function mustn't contain any side-effects and ought to be computationally
    * inexpensive to avoid starvation.
    */
  def update(f: A => A): (A, A)

  /** Atomically write a value into the cell and return the value it replaced.
    */
  def getAndSet(a: A): A

  /** Atomically write a value into the cell only if its current value is the expected value,
    * returning true if the write was successful.
    */
  def compareAndSet(expect: A, update: A): Boolean
}

object Cell {
  /** Create a cell with an initial value.
    */
  def apply[A](initial: A): Cell[A] = {
    new Cell[A] {
      val valueRef = new AtomicReference[A](initial)

      override def value: A = {
        invalidations.validate()
        valueRef.get()
      }

      override def value_=(that: A): Unit = {
        valueRef.set(that)
        invalidations.invalidate()
      }

      override def update(f: A => A): (A, A) = {
        val result = valueRef.update(f)
        invalidations.invalidate()
        result
      }

      override def getAndSet(a: A): A = {
        val result = valueRef.getAndSet(a)
        invalidations.invalidate()
        result
      }

      override def compareAndSet(expect: A, update: A): Boolean = {
        val result = valueRef.compareAndSet(expect, update)
        if (result) {
          invalidations.invalidate()
        }
        result
      }

      override lazy val invalidations: Invalidations = this.disposes(Invalidations())
    }
  }
}
