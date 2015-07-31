package org.apache.spark.hbase

import org.scalatest.{FlatSpec, Matchers}

/**
 * Created by mharis on 31/07/15.
 */
class ByteUtilsTest extends FlatSpec with Matchers {

  behavior of "Hexadecimal conversions"
  it should "be correctly reverse bytes to the original hexadecimal" in {
    val input = "----020ac416f90d91cffc09b56a9e7aea0420e0cf59----"
    val b = ByteUtils.parseRadix16(input.getBytes, 4, 40)
    ByteUtils.toRadix16(b, 0, 40) should be("020ac416f90d91cffc09b56a9e7aea0420e0cf59")
  }
}