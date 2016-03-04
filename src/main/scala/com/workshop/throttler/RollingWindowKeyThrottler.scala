package com.workshop.throttler

import java.util.concurrent.atomic.AtomicInteger
import com.workshop.framework.Clock
import scala.concurrent.duration.FiniteDuration


class RollingWindowKeyThrottler(durationWindow: FiniteDuration, max: Int, clock: Clock) {

  val map = collection.mutable.HashMap.empty[String, InvocationInfo]

  final def tryAcquire(key: String): Boolean = {
    val item = map.getOrElseUpdate(key, defaultInvocationInfo)
    _tryAcquire((key, item))
  }

  private def _tryAcquire = slidingWindowEnded orElse incrementAndGet

  private def slidingWindowEnded: PartialFunction[(String, InvocationInfo), Boolean] = {
    case item if (clock.currentMillis - item._2.timestamp) > durationWindow.toMillis =>
      map -= item._1
      tryAcquire(item._1)
  }


  private def incrementAndGet: PartialFunction[(String, InvocationInfo), Boolean] = {
    case item => item._2.count.incrementAndGet() <= max
  }

  private def defaultInvocationInfo = {
    InvocationInfo(clock.currentMillis, new AtomicInteger(0))
  }
}

case class InvocationInfo(timestamp: Long, count: AtomicInteger)
