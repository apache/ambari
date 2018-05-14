/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive20.internal;

import com.google.common.base.Optional;

/**
 * Simple implementation of a container class which can
 * hold one of two values
 * <p>
 * Callers should check if the value if left or right before
 * trying to get the value
 *
 * @param <L> Left Value
 * @param <R> Right value
 */
public class Either<L, R> {

  private final Optional<L> left;
  private final Optional<R> right;


  public boolean isLeft() {
    return left.isPresent() && !right.isPresent();
  }

  public boolean isRight() {
    return !left.isPresent() && right.isPresent();
  }

  public boolean isNone() { return  !(left.isPresent() || right.isPresent()); }

  public L getLeft() {
    return left.orNull();
  }

  public R getRight() {
    return right.orNull();
  }


  private Either(Optional<L> left, Optional<R> right) {
    this.left = left;
    this.right = right;
  }


  public static <L, R> Either<L, R> left(L value) {
    return new Either<>(Optional.of(value), Optional.<R>absent());
  }

  public static <L, R> Either<L, R> right(R value) {
    return new Either<>(Optional.<L>absent(), Optional.of(value));
  }

  public static <L, R> Either<L, R> none() {
    return new Either<>(Optional.<L>absent(), Optional.<R>absent());
  }

}



