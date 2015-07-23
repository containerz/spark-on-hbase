# spark-on-hbase
* Spark optimised for scanning, joining and updating HBaseRdd
* The master setup is for HBase API 1.1.0.1, Scala 2.10 and Spark 1.4.1 but it is possible to create branches for older APIs

# features overview

* __HKeySpace__ and __HKey__ - abstract classes for defining different types of key, a small but integral piece of code. All the keys, whether HBase row key or Pair RDD keys are represented as HKey and each HKey is defined by HKeySpace type and byte array value. This ensures that each key stores it's salt and meta-data about the type in the first 6 bytes when serialised into a byte array.
* __RegionPartitioner__ - extension of spark Partitioner that uses HBase internals to create key ranges for given number of regions - it emulates precisely the behaviour of HBase partitioner if HBase table splits are managed manually or works as a good approximation to HBase partitioner if automatic region splitting used. Note that approximation still works good because each spark task talks only to one region if there's more partitions that regions and only to a few regions if there is more regions than spark partitions.
* __HBaseRdd__ - extension of spark RDD that is capable of single-stage join using with pluggable implementations, provided are multiget lookup and filter scan joins
* __HBaseTable__ - class for executing mutations of HBase from an RDD input - supports bulk operations as well as standard HBase Client operations
* __HBaseJoin__ - using HBase ordered properties and multiget functionality, this abstract function is provided with several variants for optimised joins



# quick start on YARN

    - cp default-spark-env.template default-spark-env ...modify env.variable to match your environment
    - ./scripts/assemble.spark - this will clone spark into /usr/lib/spark, build assembly jar and put it into <hdfs path>
    - ./scripts/build.demo - this will build a spark-on-hbase-demo.jar which can be run as a shell or a job - see below



# configuration and scripts

...


