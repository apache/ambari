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

package org.apache.ambari.server.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.Callable;

/**
 * Utilities for more convenient exception handling
 */
public class ExceptionUtils {

  /**
   * Utility to simplify the try-catch-rethrow-RuntimeExeption pattern commonly found in the code.
   * @param throwingLambda A lambda expression that can throw an (ususally checked) exception
   * @param <R> The return type of the expression
   * @return The return value of the lamba expression. In case an {@link Exception} is thrown during lambda invocation the
   *         exception is converted into a {@link RuntimeException} if needed. See {@link #convertToRuntime}
   *         for conversion logic.
   */
  public static <R> R unchecked(Callable<R> throwingLambda) {
    try {
      return throwingLambda.call();
    }
    catch (Exception ex) {
      throw convertToRuntime(ex);
    }
  }

  /**
   * Same as {@link #unchecked(Callable)} but for void methods.
   * @param throwingLambda A void lambda expression that can throw an (ususally checked) exception
   */
  public static void uncheckedVoid(ThrowingRunnable throwingLambda) {
    try {
      throwingLambda.run();
    }
    catch (Exception ex) {
      throw convertToRuntime(ex);
    }
  }


  /**
   * Utility to convert checked exceptions to runtime exceptions.
   * <ul>
   *   <li>If the input exception is already of type {@link RuntimeException} the input exception is returned</li>
   *   <li>If the input exception is of type {@link IOException} the execption will be wrapped into an
   *   {@link UncheckedIOException}</li>
   *   <li>Other checked exeptions will be wrapped into a {@link RuntimeException}</li>
   *   <li>Conversion logic could be improved later</li>
   * </ul>
   * @param ex The input exception can be a checked as well as a runtime exception
   * @return a runtime exception which equals to {@code ex} if ex is a runtime exception or {@code ex} wrapped in a
   * {@link RuntimeException}.
   */
  public static RuntimeException convertToRuntime(Exception ex) {
    return ex instanceof RuntimeException ? (RuntimeException)ex :
      ex instanceof IOException ? new UncheckedIOException((IOException)ex) :
        new RuntimeException(ex);
  }

  public interface ThrowingRunnable {
    void run() throws Exception;
  }

}
