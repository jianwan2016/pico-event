package org.pico.event

import java.io.Closeable
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import org.pico.atomic.syntax.std.atomicReference._
import org.pico.disposal.{OnClose, SimpleDisposer}

/** A simple SinkSource which implements subscriber tracking.
  *
  * This implementation does not release references have a well-defined closed state.
  * The SimpleSinkSource wrapper type will properly define the closed state.
  *
  * @tparam A The sink event type
  * @tparam B The source event type
  */
trait Subscribers[-A, +B] extends SimpleDisposer {
  /** Add the subscriber.  Events continue to be published to the subscriber until
    * it is garbage collected.
    */
  def subscribe(subscriber: B => Unit): Closeable

  /** Publish the event to subscribers.
    */
  def publish(event: A): Unit

  /** Clean up any weak references as necessary.
    */
  def houseKeep(): Unit
}

object Subscribers {
  def apply[A, B](f: A => B): Subscribers[A, B] = {
    new Subscribers[A, B] {
      val subscribers = new AtomicReference(List.empty[WeakReference[Wrapper[B => Unit]]])
      val garbage = new AtomicInteger(0)

      def transform: A => B = f

      override def subscribe(subscriber: B => Unit): Closeable = {
        val wrapper = Wrapper(subscriber)
        val subscriberRef = new WeakReference(wrapper)

        subscribers.update(subscriberRef :: _)

        houseKeep()

        OnClose {
          identity(wrapper)
          subscriberRef.clear()
          houseKeep()
        }
      }

      override def publish(event: A): Unit = {
        val v = transform(event)

        subscribers.get().foreach { subscriberRef =>
          // The use of the wrapper variable facilitates garbage collection.
          //
          // Weak references are reset to null by the garbage collector when it discovers the
          // reference is not reachable and GCs invocations are non-deterministic.
          //
          // The act of calling the subscriber keeps the subscriber reference reachable during the
          // call.  Should a GC coincide with the call to the subscriber, the GC would determine
          // that the subscriber is reachable and not set the corresponding weak reference to null
          // even if this subscriber reference is the only subscriber reference.
          //
          // The extra level of indirection introduced by the wrapper variable coupled with the
          // strategy of setting it to null as early as possible minimises the probability of
          // keeping the weak reference alive unnecessarily.
          var wrapper = subscriberRef.get()

          if (wrapper != null) {
            val subscriber = wrapper.target
            // Drop reference to wrapper as early as possible, as per above explanation, so that the
            // garbage collector can collect it if there are no other references to it.
            wrapper = null
            subscriber(v)
          } else {
            garbage.incrementAndGet()
          }
        }

        houseKeep()
      }

      override def houseKeep(): Unit = {
        if (garbage.get() > subscribers.get().size) {
          garbage.set(0)
          subscribers.update { subscriptions =>
            subscriptions.filter { subscription =>
              subscription.get() != null
            }
          }
        }
      }
    }
  }
}
