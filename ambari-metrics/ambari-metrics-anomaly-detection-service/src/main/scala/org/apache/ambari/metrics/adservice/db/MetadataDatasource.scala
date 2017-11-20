/**
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package org.apache.ambari.metrics.adservice.db

trait MetadataDatasource {

  type Key = Array[Byte]
  type Value = Array[Byte]

  /**
    *  Idempotent call at the start of the application to initialize db
    */
  def initialize(): Unit

  /**
    * This function obtains the associated value to a key. It requires the (key-value) pair to be in the DataSource
    *
    * @param key
    * @return the value associated with the passed key.
    */
  def apply(key: Key): Value = get(key).get

  /**
    * This function obtains the associated value to a key, if there exists one.
    *
    * @param key
    * @return the value associated with the passed key.
    */
  def get(key: Key): Option[Value]

  /**
    * This function obtains all the values
    *
    * @return the list of values
    */
  def getAll: List[Value]

  /**
    * This function associates a key to a value, overwriting if necessary
    */
  def put(key: Key, value: Value): Unit

  /**
    * Delete key from the db
    */
  def delete(key: Key): Unit

  /**
    * This function updates the DataSource by deleting, updating and inserting new (key-value) pairs.
    *
    * @param toRemove which includes all the keys to be removed from the DataSource.
    * @param toUpsert which includes all the (key-value) pairs to be inserted into the DataSource.
    *                 If a key is already in the DataSource its value will be updated.
    * @return the new DataSource after the removals and insertions were done.
    */
  def update(toRemove: Seq[Key], toUpsert: Seq[(Key, Value)]): Unit

  /**
    * This function closes the DataSource, without deleting the files used by it.
    */
  def close(): Unit

}
