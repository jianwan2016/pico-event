package org.pico.event

/** This class introduces an indirection to aid the garbage collector in discovering unreachable
  * targets.  WARNING: It must not be an AnyVal.
  */
case class Wrapper[A](target: A)
