package org.apache.spark.hbase

import org.apache.spark.hbase.keyspace.KeySpaceRegistry.KSREG
import org.apache.spark.hbase.keyspace._
import org.scalatest._

class KeyTest extends FlatSpec with Matchers {

  implicit val TestKeySpaceReg: KSREG = Map(
    new KeySpaceString("d").keyValue,
    new KeySpaceLong("r").keyValue,
    new KeySpaceUUID("x").keyValue,
    new KeySpaceUUID("v").keyValue
  )

  val d = Key("d", "2")
  val r = Key("r", "1")
  Seq(d, r).sortWith(Key.comparator) should be(Seq(r, d))

  val d0 = Key("d", "CESE1111")
  d0.bytes.mkString(",") should be("3,-126,76,116,0,100,67,69,83,69,49,49,49,49")
  d0.toString should be("CESE1111:d")
  d0.asString should be("CESE1111")
  val d1 = Key("d", "CESE9999")
  d1.bytes.mkString(",") should be("3,-122,14,116,0,100,67,69,83,69,57,57,57,57")
  d1.toString should be("CESE9999:d")
  d1.asString should be("CESE9999")

  val v0 = Key("v", "f81d4fae-7dec-11d0-a765-00a0c91e6bf6")
  v0.bytes.mkString(",") should be("-8,29,79,-82,0,118,-8,29,79,-82,125,-20,17,-48,-89,101,0,-96,-55,30,107,-10")
  v0.toString should be("f81d4fae-7dec-11d0-a765-00a0c91e6bf6:v")
  v0.asString should be("f81d4fae-7dec-11d0-a765-00a0c91e6bf6")

  val v1 = Key("v", "f81d4fae-7dec-11d0-a765-00a0c91e6bf6")
  val v2 = Key("v", "f81d4fae-7dec-11d0-a765-00a0c91e6bf7")
  val x3 = Key("x", "f81d4fae-7dec-11d0-a765-00a0c91e6bf8")
  x3.toString should be("f81d4fae-7dec-11d0-a765-00a0c91e6bf8:x")
  x3.asString should be("f81d4fae-7dec-11d0-a765-00a0c91e6bf8")

  v0.compareTo(v1) should be(0)
  v0.compareTo(v2) should be < 0
  x3.compareTo(v0) should be > 0
  x3.compareTo(v2) should be > 0

  v0.equals(v1) should be(true)
  v0.hashCode should equal(v1.hashCode)
  v1.compareTo(v2) should be < 0
  v2.compareTo(v1) should be > 0
  x3.compareTo(v1) should be > 0
  v1.equals(v2) should be(false)
  v2.equals(v2) should be(true)
  v2.keySpace should be(KeySpace("v"))
  x3.keySpace should not be (KeySpace("v"))

  val seq = Seq(v0, v1, v2, x3)
  val customComparatorSortedSeq = seq.sortWith(Key.comparator)
  val sortedSeq = seq.sorted

  customComparatorSortedSeq should be(sortedSeq)
}