/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.orm.helpers;

import java.util.Collection;

import com.google.common.collect.Iterables;

public class SQLOperations {
  public interface BatchOperation<T> {
    /**
     * @param chunk        portion of total batch
     * @param currentBatch current batch number
     * @param totalBatches total batches
     * @param totalSize    total size of the batch
     * @return number of processed items
     */
    int chunk(Collection<T> chunk, int currentBatch, int totalBatches, int totalSize);
  }

  /**
   * Split the collection to the batches
   *
   * @param collection collection with values to be splitted
   * @param batchSize  size of the one split
   * @param callback   logic to process one split
   * @return Total number of processed items in collection
   */
  public static <T> int batch(Collection<T> collection, int batchSize, BatchOperation<T> callback) {
    if (collection == null || collection.isEmpty() || batchSize == 0) {
      return 0;
    }
    int totalSize = collection.size();
    int totalChunks = (int) Math.ceil((float) totalSize / batchSize);
    int currentChunk = 0;
    int resultSum = 0;

    for (Collection<T> chunk : Iterables.partition(collection, batchSize)) {
      currentChunk += 1;
      resultSum += callback.chunk(chunk, currentChunk, totalChunks, totalSize);
    }
    return resultSum;
  }
}
