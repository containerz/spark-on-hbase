This is a generic extension of spark for efficient scanning, joining and updating HBase tables from a spark environment. The master setup is for HBase API 1.1.0.1, Scala 2.10 and Spark 1.4.1 but it is possible to create branches for older APIs simply by changing the versions properties in the pom.xml (dataframes api is not necessary for the basic use case so practically any spark version > 0.92 should work but for HBase old API some refactoring will be required)

# important concepts and classes

* __HKeySpace__ and __HKey__ - abstract classes for defining different types of key, a small but integral piece of code. All the keys, whether HBase row key or Pair RDD keys are represented as HKey and each HKey is defined by HKeySpace type and byte array value. This ensures that each key stores it's salt and meta-data about the type in the first 6 bytes when serialised into a byte array.
* __RegionPartitioner__ - extension of spark Partitioner that uses HBase internals to create key ranges for given number of regions - it emulates precisely the behaviour of HBase partitioner if HBase table splits are managed manually or works as a good approximation to HBase partitioner if automatic region splitting used. Note that approximation still works good because each spark task talks only to one region if there's more partitions that regions and only to a few regions if there is more regions than spark partitions.
* __HBaseRdd__ - extension of spark RDD that is capable of single-stage join using with pluggable implementations, provided are multiget lookup and filter scan joins
* __HBaseTable__ - class for mapping hbase table to an rdd and for executing mutations of HBase from an RDD input - supports bulk operations as well as standard HBase Client operations
* __HBaseJoin__ - using HBase ordered properties and multiget functionality, this abstract function is provided with several variants for optimised joins


# quick start - running DEMO on YARN cluster

First thing you'll need is a deafult-spark-env, there's a template you can copy and then modify to match your environment.

```cp scripts/default-spark-env.template scripts/default-spark-env```

On the yarn nodes as well as driver, the following files should be distributed:
```/usr/lib/hbase/lib``` needs to contain all hbase java libraries required by the hbase client
```/usr/lib/hbase/lib/native``` needs to contain all required native libraries for compression algorithms etc.

Further, on the driver you'll need the distributions of spark and hadoop as defined in the pom.xml and on the path defined by `$SPARK_HOME/` and `$HADOOP_HOME` in the spark-default-env respectively 
NOTE: that the scripts predefined here for spark-shell and spark-submit define the spark master as yarn-client so the driver is the computer from which you are building the demo app.
    
If you don't have your spark assembly jar ready on the driver or available in hdfs for executors, you'll first need to build it and put it on the driver and into hdfs:

```./scripts/build spark```
    
You can then use build script to build a demo application, which will package and prepare start shell and submit scripts:

```./scripts/build demo```

You can then run the demo appliation as a shell:

`./scripts/demo-spark-shell`

# ...

# TODOs

- Currently HBaseRdd is RDD[(HKey, Result)] but most of the functionality can be described for RDD[(Array[Byte], Result)] - implementing all of the rich functionality at the lower level will enable usage without HKey and HKeySpace
- refactor for enabling forks and make the graph demo an example of a fork
