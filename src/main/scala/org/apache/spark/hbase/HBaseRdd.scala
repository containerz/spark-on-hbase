package org.apache.spark.hbase

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.filter._
import org.apache.hadoop.hbase.util.Pair
import org.apache.hadoop.hbase.{HConstants, TableName}
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.rdd.RDD
import org.apache.spark.{Partition, SerializableWritable, SparkContext, TaskContext}
import org.apache.hadoop.hbase.util.Bytes

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
class HBaseRdd(sc: SparkContext
               , @transient val hbaseConf: Configuration
               , @transient val tableName: TableName
               , val idSpace: Short
               , val minStamp: Long
               , val maxStamp: Long
               , val columns: String*
                ) extends RDD[(HKey, Result)](sc, Nil) with HBaseUtils {

  private val tableNameAsString = tableName.toString
  private val configuration = new SerializableWritable(hbaseConf)

  def this(sc: SparkContext, hbaConf: Configuration, tableName: TableName, columns: String*)
  = this(sc, hbaConf, tableName, -1.toShort, HConstants.OLDEST_TIMESTAMP, HConstants.LATEST_TIMESTAMP, columns: _*)

  def this(sc: SparkContext, hbaConf: Configuration, tableName: TableName, minStamp: Long, maxStamp: Long, columns: String*)
  = this(sc, hbaConf, tableName, -1.toShort, minStamp, maxStamp, columns: _*)

  def this(sc: SparkContext, hbaConf: Configuration, tableName: TableName, idSpace: Short, columns: String*)
  = this(sc, hbaConf, tableName, idSpace, HConstants.OLDEST_TIMESTAMP, HConstants.LATEST_TIMESTAMP, columns: _*)

  val regionSplits: Array[(Array[Byte], Array[Byte])] = getRegionSplits(hbaseConf, tableName)

  override protected def getPartitions: Array[Partition] = {
    (for (i <- 0 to regionSplits.size - 1) yield {
      new Partition {
        override val index: Int = i
      }
    }).toArray
  }

  @DeveloperApi
  override def compute(split: Partition, context: TaskContext): Iterator[(HKey, Result)] = {

    val connection = ConnectionFactory.createConnection(configuration.value)
    val table = connection.getTable(TableName.valueOf(tableNameAsString))
    val scan = new Scan()
    scan.setMaxVersions(1)
    scan.setConsistency(Consistency.STRONG)
    if (columns.size > 0) {
      columns.foreach(_ match {
        case cf: String if (!cf.contains(':')) => scan.addFamily(Bytes.toBytes(cf))
        case column: String => column.split(":") match {
          case Array(cf, qualifier) => scan.addColumn(Bytes.toBytes(cf), Bytes.toBytes(qualifier))
        }
      })
    }
    val (startKey, stopKey) = regionSplits(split.index)
    if (startKey.size > 0) scan.setStartRow(startKey)
    if (stopKey.size > 0) scan.setStopRow(stopKey)
    if (minStamp != HConstants.OLDEST_TIMESTAMP || maxStamp != HConstants.LATEST_TIMESTAMP) {
      scan.setTimeRange(minStamp, maxStamp)
    }
    if (idSpace != -1) {
      scan.setFilter(new FuzzyRowFilter(
        List(new Pair(HKeySpace(idSpace).allocate(0), Array[Byte](1, 1, 1, 1, 0, 0))).asJava
      ))
    }
    val scanner: ResultScanner = table.getScanner(scan)
    var current: Option[(HKey, Result)] = None

    new Iterator[(HKey, Result)] {
      override def hasNext: Boolean = current match {
        case None => forward
        case _ => true
      }

      override def next(): (HKey, Result) = {
        if (!current.isDefined && !forward) {
          throw new NoSuchElementException
        }
        val n = current.get
        current = None
        n
      }

      private def forward: Boolean = {
        if (current.isEmpty) {
          val result = scanner.next
          if (result == null || result.isEmpty) {
            table.close
            connection.close
            false
          } else {
            current = Some((HKey(result.getRow), result))
            true
          }
        } else {
          throw new IllegalStateException
        }
      }
    }
  }

}
