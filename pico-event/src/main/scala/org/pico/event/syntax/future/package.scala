package org.pico.event.syntax

import org.pico.event.Sink

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

package object future {
  implicit class FutureOps_2Qos8tq[A](val self: Future[A]) extends AnyVal {
    /** Publish the result of the future into the sink when it completes successfully
      */
    def successInto(sink: Sink[A]): Future[A] = {
      self.foreach { value =>
        sink.publish(value)
      }

      self
    }

    /** Publish the error of the future into the sink when it fails
      */
    def failureInto(sink: Sink[Throwable]): Future[A] = {
      self.onFailure { case value =>
        sink.publish(value)
      }

      self
    }

    /** Publish the result of the future into the success sink when it completes successfully or
      * the error of the future into the sink when it fails.
      */
    def completeInto(successSink: Sink[A], failureSink: Sink[Throwable]): Future[A] = {
      self.successInto(successSink).failureInto(failureSink)
    }

    /** Publish the result or error of the future into the sink when it completes.
      */
    def completeInto(sink: Sink[Either[Throwable, A]]): Future[A] = {
      self.successInto(sink.comap(Right(_))).failureInto(sink.comap(Left(_)))
    }
  }
}
