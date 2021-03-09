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

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

public class SQLOperationsTest {
  @Before
  public void setup() throws Exception {

  }

  @Test
  public void testBatchOperation() {
    final List<Integer> testCollection = new ArrayList<>();
    final int collectionSize = 150;
    final int batchSize = 10;
    final int totalExpectedBatches = (int) Math.ceil((float) collectionSize / batchSize);

    for (int i = 0; i < collectionSize; i++) {
      testCollection.add(i);
    }

    int processedItems = SQLOperations.batch(testCollection, batchSize, (chunk, currentBatch, totalBatches, totalSize) -> {
      Assert.assertTrue(chunk.size() <= batchSize);
      Assert.assertEquals(totalExpectedBatches, totalBatches);
      return chunk.size();
    });
    Assert.assertEquals(collectionSize, processedItems);
  }
}

