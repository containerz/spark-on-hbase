/**
 * this script is enetered with sc already defined
 */
import scala.math._
import org.apache.spark.hbase.demo.Demo
import org.apache.spark.hbase._
import org.apache.hadoop.hbase.util.Bytes
import org.apache.spark.storage.StorageLevel._


/**
 * Initialise DEMO within the current spark shell context and import all public members into the shell's global scope
 */
val DEMO = new Demo(sc)

import DEMO._

help