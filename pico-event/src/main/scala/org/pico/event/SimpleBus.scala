package org.pico.event

/** A simple [[SinkSource]] where the sink message type and source message type are the same.
  */
trait SimpleBus[A] extends SimpleSinkSource[A, A] {
  override def transform: A => A = identity
}
