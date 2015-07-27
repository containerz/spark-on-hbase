package org.apache.spark.hbase.keyspace

import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.filter._
import org.apache.hadoop.hbase.util.Pair
import org.apache.hadoop.hbase.{HConstants, TableName}
import org.apache.spark.SparkContext
import org.apache.spark.hbase.HBaseRDD
import org.apache.spark.hbase.keyspace.HKeySpaceRegistry.HKSREG

import scala.collection.JavaConverters._

/**
 * Created by mharis on 10/07/15.
 *
 * HBaseRdd is an RDD[(HKey, hbase.client.Result)] - HKey is defined in this implementation elsewhere because there
 * is a lot of that needs to be taken care of for a hbase row key to scale.
 *
 * HKey and HBaseRdd are interdependent in that, the first 4 bytes are salt for the key generated by the HKeySpace to
 * which the HKey belongs and the following 2 bytes are the signature of the HKeySpace. With this representation it
 * is possible to ensure that:
 * 1) any type of key can be "made" to be distributed evenly
 * 2) different key types can be mixed in a single hbase table (but don't have to be - depends on application)
 * 3) fuzzy row filter can be applied on the 2-byte key space signature to fast forward on hbase server-side
 *
 * columns is a sequence of string identifiers which can either reference a column family, e.g. 'N' or a specific
 * column, e.g. 'F:propensity'
 */
class HBaseRddHKey(sc: SparkContext
               , @transient tableName: TableName
               , idSpace: Short
               , minStamp: Long
               , maxStamp: Long
               , columns: String*
                )(implicit reg: HKSREG) extends HBaseRDD[HKey, Result](sc, tableName, minStamp, maxStamp) {

  def this(sc: SparkContext, tableName: TableName, columns: String*)(implicit reg: HKSREG)
  = this(sc, tableName, -1.toShort, HConstants.OLDEST_TIMESTAMP, HConstants.LATEST_TIMESTAMP, columns: _*)

  def this(sc: SparkContext, tableName: TableName, minStamp: Long, maxStamp: Long, columns: String*)(implicit reg: HKSREG)
  = this(sc, tableName, -1.toShort, minStamp, maxStamp, columns: _*)

  def this(sc: SparkContext, tableName: TableName, idSpace: Short, columns: String*)(implicit reg: HKSREG)
  = this(sc, tableName, idSpace, HConstants.OLDEST_TIMESTAMP, HConstants.LATEST_TIMESTAMP, columns: _*)

  override def bytesToKey = (rowKey: Array[Byte]) => HKey(rowKey)

  override def keyToBytes = (key: HKey) => key.bytes

  override def resultToValue = (row: Result) => row

  override protected def getRegionScan(region: Int): Scan = {
    val scan = super.getRegionScan(region)
    if (idSpace != -1) {
      scan.setFilter(new FuzzyRowFilter(
        List(new Pair(HKeySpace(idSpace).allocate(0), Array[Byte](1, 1, 1, 1, 0, 0))).asJava
      ))
    }
    scan
  }

}
