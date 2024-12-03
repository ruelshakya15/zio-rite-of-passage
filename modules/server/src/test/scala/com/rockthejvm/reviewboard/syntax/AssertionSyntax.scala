package com.rockthejvm.reviewboard.syntax

import zio.*
import zio.test.*

extension [R, E, A](zio: ZIO[R, E, A])
  def assert(assertion: Assertion[A]): ZIO[R, E, TestResult] =
    assertZIO(zio)(assertion)

  def assert(
      predicate: (=> A) => Boolean
  ): ZIO[R, E, TestResult] = // passed by name because Assertion.assertion take by name parameter
    assert(Assertion.assertion("test assertion")(predicate))
