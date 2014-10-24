/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.rdd

import java.io._
import java.lang.reflect.{Array => JArray}

import org.apache.spark.scheduler.OpenCLContext
import org.apache.spark.{Logging, Partition, TaskContext}
import org.jocl.CL._
import org.jocl._

import scala.collection.immutable.IndexedSeq
import scala.reflect.ClassTag

/**
 *
 */
class GpuRDD[T <: Product : ClassTag](prev: RDD[T], val columnTypes: Array[String],
                                      chunkCapacity: Int)
  extends RDD[T](prev) {
  /**
   * :: DeveloperApi ::
   * Implemented by subclasses to compute a given partition.
   */
  override def compute(split: Partition, context: TaskContext): Iterator[T] = {
    new GpuPartitionIterator(firstParent[T].iterator(split, context), columnTypes, chunkCapacity)
  }

  /**
   * Implemented by subclasses to return the set of partitions in this RDD. This method will only
   * be called once, so it is safe to implement a time-consuming computation in it.
   */
  override def getPartitions: Array[Partition] = firstParent[T].partitions
}