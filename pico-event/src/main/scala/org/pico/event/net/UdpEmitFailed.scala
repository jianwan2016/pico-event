package org.pico.event.net

import java.net.InetSocketAddress
import java.nio.ByteBuffer

/** An error message indicating failure to emit a UDP packet exactly as requested
  *
  * @param address the target address the emit attempt was made to
  * @param buffer the data that was to be emitted
  * @param sentBytes the number of bytes actually emitted
  */
case class UdpEmitFailed(
    address: InetSocketAddress,
    buffer: ByteBuffer,
    sentBytes: Int)
