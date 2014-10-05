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
import org.apache.spark.deploy.worker.WorkerArguments
import org.apache.spark.rdd.{ChunkIterator, RDDChunk}
import org.scalatest.FunSuite

import scala.collection.immutable.IndexedSeq
import scala.language.existentials
import scala.reflect.ClassTag

/**
 *
 */
class GpuLayout extends FunSuite with SharedSparkContext {

  class ParametricClass[T: ClassTag](var element: Array[T]) {

    def typeInfo(): Class[_] = {
      val tag: ClassTag[T] = reflect.classTag[T]
      tag.runtimeClass
    }

  }

  test("org.apache.spark.rdd.RDDChunk.initArray test") {
    val x = new RDDChunk[(Int, String, Float, Double, String)](Array("INT", "STRING", "FLOAT", "DOUBLE", "STRING"))
    assert(x.intData.length === 1)
    assert(x.longData.length === 0)
    assert(x.floatData.length === 1)
    assert(x.doubleData.length === 1)
    assert(x.stringData.length === 2)
  }

  test("org.apache.spark.rdd.RDDChunk.fill test") {
    val testData = (0 to 10).reverse.zipWithIndex.toIterator

    val chunk = new RDDChunk[(Int, Int)](Array("INT", "INT"))
    chunk.fill(testData)
    (0 until chunk.MAX_SIZE).foreach(i =>
      if (i <= 10) {
        assert(chunk.intData(0)(i) === (10 - i), "values do not match")
        assert(chunk.intData(1)(i) === i, "indexes  do not match")
      } else {
        assert(chunk.intData(0)(i) === 0, "values do not match")
        assert(chunk.intData(1)(i) === 0, "values do not match")
      }
    )
  }

  test("org.apache.spark.rdd.ChunkIterator test") {
    val testData = (0 to 10).reverse.zipWithIndex.toIterator

    val colTypes = Array("INT", "INT")
    val chunkItr = new ChunkIterator(testData, colTypes)

    val chunk = chunkItr.next
    (0 until chunk.MAX_SIZE).foreach(i =>
      if (i <= 10) {
        assert(chunk.intData(0)(i) === (10 - i), "values do not match")
        assert(chunk.intData(1)(i) === i, "indexes  do not match")
      } else {
        assert(chunk.intData(0)(i) === 0, "values do not match")
        assert(chunk.intData(1)(i) === 0, "values do not match")
      }
    )
  }

  test("GpuRDD test") {
    val testData: IndexedSeq[(Int, Int)] = (0 to 10).reverse.zipWithIndex

    val rdd = sc.parallelize(testData)
    val gpuRdd = rdd.toGpuRDD(Array("INT", "INT"))
    val collectedChunks: Array[RDDChunk[Product]] = gpuRdd.collect()
    assert(collectedChunks.length === 1)
    val chunk = collectedChunks(0)
    (0 until chunk.MAX_SIZE).foreach(i =>
      if (i <= 10) {
        assert(chunk.intData(0)(i) === (10 - i), "values do not match")
        assert(chunk.intData(1)(i) === i, "indexes  do not match")
      } else {
        assert(chunk.intData(0)(i) === 0, "values do not match")
        assert(chunk.intData(1)(i) === 0, "values do not match")
      }
    )
  }

  test("org.apache.spark.deploy.worker.WorkerArguments.inferDefaultGpu test") {
    val arguments = new WorkerArguments(Array("spark://localhost:7077"))
    assert( arguments.inferDefaultGpu() === 1, "There is one GPU on this device")
  }

  test("org.apache.spark.rdd.RDDChunk.toTypedColumnIndex test") {
    val testData: IndexedSeq[(Int, Int)] = (0 to 10).reverse.zipWithIndex

    val rdd = sc.parallelize(testData)
    val gpuRdd = rdd.toGpuRDD(Array("INT", "STRING", "FLOAT", "DOUBLE", "STRING", "INT", "STRING"))
    val collectedChunks: Array[RDDChunk[Product]] = gpuRdd.collect()
    assert(collectedChunks.length === 1)
    val chunk = collectedChunks(0)
    assert(chunk.toTypedColumnIndex(0) === 0)
    assert(chunk.toTypedColumnIndex(1) === 0)
    assert(chunk.toTypedColumnIndex(2) === 0)
    assert(chunk.toTypedColumnIndex(3) === 0)
    assert(chunk.toTypedColumnIndex(4) === 1)
    assert(chunk.toTypedColumnIndex(5) === 1)
    assert(chunk.toTypedColumnIndex(6) === 2)
  }
}
