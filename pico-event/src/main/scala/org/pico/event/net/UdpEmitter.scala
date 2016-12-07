package org.pico.event.net

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

import org.pico.disposal.std.autoCloseable._
import org.pico.event.{Bus, Sink, SinkSource}

object UdpEmitter {
  /** Create a [[SinkSource]] that emits UDP packets when publishing to its sink and reports
    * failures from its source.
    *
    * @param addressLookup A function that returns the address to which the UDP packet should be emitted
    */
  def apply(addressLookup: () => InetSocketAddress): SinkSource[ByteBuffer, UdpEmitFailed] = {
    val clientChannel = DatagramChannel.open

    val errors = Bus[UdpEmitFailed]

    val sink = Sink[ByteBuffer] { buffer =>
      val address = addressLookup()
      val sentBytes = clientChannel.send(buffer, address)

      if (buffer.limit() != sentBytes) {
        errors.publish(UdpEmitFailed(address, buffer, sentBytes))
      }
    }

    sink.disposes(clientChannel)

    SinkSource.from[ByteBuffer, UdpEmitFailed](sink, errors)
  }
}
