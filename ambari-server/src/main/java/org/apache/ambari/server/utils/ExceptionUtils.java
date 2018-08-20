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

public class ExceptionUtils {

  public interface ThrowingLambda<T extends Exception, R> {
    R doIt() throws T;
  }

  public static <R> R unchecked(ThrowingLambda<? extends Exception, R> throwingLambda) {
    try {
      return throwingLambda.doIt();
    }
    catch (Exception ex) {
      throw ex instanceof RuntimeException ? (RuntimeException)ex :
        ex instanceof IOException ? new UncheckedIOException((IOException)ex) :
          new RuntimeException(ex);
    }
  }

}
