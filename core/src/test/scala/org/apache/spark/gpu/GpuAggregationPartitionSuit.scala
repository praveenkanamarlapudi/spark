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

package org.apache.spark.gpu

import org.apache.spark.SharedSparkContext
import org.apache.spark.rdd.{ AggregationOperation, GpuAggregationPartition }
import org.apache.spark.scheduler.OpenCLContext
import org.scalatest.FunSuite
import scala.language.existentials
import scala.reflect.ClassTag
import org.apache.spark.rdd.GpuPartition
import java.io.ObjectInputStream

/**
 *
 */
class GpuAggregationPartitionSuit extends FunSuite with SharedSparkContext {

  val DEFAULT_CAPACITY = (1 << 10)
  val openCLContext = new OpenCLContext

  override def beforeAll() {
    super.beforeAll()
    //    setLogLevel(LogLevel.LOG_TRACE)
    openCLContext.initOpenCL("/org/apache/spark/gpu/kernel.cl")
  }

  test("Aggregation simple(Int, Int) size test") {
    val testData: IndexedSeq[(Int, Int)] = Array((11, 3), (11, 1), (11, 4), (12, 6), (12, 5))

    val partition = new GpuPartition[(Int, Int)](openCLContext, DEFAULT_CAPACITY)

    partition.fill(testData.toIterator)

    val aggregationPartition = new GpuAggregationPartition[(Int, Int)](openCLContext, partition, Array(0), Array(AggregationOperation.max), DEFAULT_CAPACITY)

    aggregationPartition.fill(testData.toIterator)
    val expectedData = Array((11, 2), (12, 6))

    assert(aggregationPartition.size === expectedData.length)
  }

  ignore("Aggregation simple(Int, Int)  2 test") {
    val testData: IndexedSeq[(Int, Int)] = Array((111, 3), (111, 1), (111, 4), (112, 6), (112, 5))

    val partition = new GpuPartition[(Int, Int)](openCLContext, DEFAULT_CAPACITY)

    partition.fill(testData.toIterator)

    val aggregationPartition = new GpuAggregationPartition[(Int, Int)](openCLContext, partition, Array(0), Array(AggregationOperation.max), DEFAULT_CAPACITY)

    aggregationPartition.fill(testData.toIterator)
    val expectedData = Array((11, 2), (12, 6))

    assert(aggregationPartition.size === expectedData.length)

    expectedData.zipWithIndex.foreach {
      case ((gbVal, aggValue), index) =>
        assert(aggregationPartition.intData(0).get(index) === gbVal, "values do not match")
        assert(aggregationPartition.intData(1).get(index) === aggValue, "values do not match")
      case _ => fail("We should not be here")
    }
  }

  test("Aggregation simple(Int, Long)  size test") {
    val testData: IndexedSeq[(Int, Long)] = Array((11, 3L), (11, 1L), (11, 4L), (12, 6L), (12, 5L))

    val partition = new GpuPartition[(Int, Long)](openCLContext, DEFAULT_CAPACITY)

    partition.fill(testData.toIterator)

    val aggregationPartition = new GpuAggregationPartition[(Int, Long)](openCLContext, partition, Array(0), Array(AggregationOperation.max), DEFAULT_CAPACITY)

    aggregationPartition.fill(testData.toIterator)
    val expectedData = Array((11, 4L), (12, 6L))

    assert(aggregationPartition.size === expectedData.length)
  }

  ignore("Aggregation simple(Int, Long)  match test") {
    val testData: IndexedSeq[(Int, Long)] = Array((11, 3L), (11, 1L), (11, 4L), (12, 6L), (12, 5L))

    val partition = new GpuPartition[(Int, Long)](openCLContext, DEFAULT_CAPACITY)

    partition.fill(testData.toIterator)

    val aggregationPartition = new GpuAggregationPartition[(Int, Long)](openCLContext, partition, Array(0), Array(AggregationOperation.max), DEFAULT_CAPACITY)

    aggregationPartition.fill(testData.toIterator)
    val expectedData = Array((11, 4L), (12, 6L))

    assert(aggregationPartition.size === expectedData.length)

    expectedData.zipWithIndex.foreach {
      case ((gbVal, aggValue), index) =>
        assert(aggregationPartition.intData(0).get(index) === gbVal, "values do not match")
        assert(aggregationPartition.longData(0).get(index) === aggValue, "values do not match")
      case _ => fail("We should not be here")
    }
  }

}