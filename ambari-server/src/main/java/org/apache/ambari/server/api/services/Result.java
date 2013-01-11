/**
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

package org.apache.ambari.server.api.services;


import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.api.util.TreeNode;

/**
 * Represents a result from a request handler invocation.
 */
public interface Result {

  public static enum STATUS { OK(200, "OK", false), CREATED(201, "Created", false), ACCEPTED(202, "Accepted", false),
    CONFLICT(409, "Resource Conflict", true), NOT_FOUND(404, "Not Found", true), BAD_REQUEST(400, "Bad Request", true),
    UNAUTHORIZED(401, "Unauthorized", true), FORBIDDEN(403, "Forbidden", true),
    SERVER_ERROR(500, "Internal Server Error", true);

    private int    m_code;
    private String m_desc;
    private boolean m_isErrorState;

    private STATUS(int code, String description, boolean isErrorState) {
      m_code = code;
      m_desc = description;
      m_isErrorState = isErrorState;
    }

    public int getStatus() {
      return m_code;
    }

    public String getDescription() {
      return m_desc;
    }

    public boolean isErrorState() {
      return m_isErrorState;
    }

    @Override
    public String toString() {
      return getDescription();
    }
  };

  /**
   * Obtain the results of the request invocation as a Tree structure.
   *
   * @return the results of the request a a Tree structure
   */
  public TreeNode<Resource> getResultTree();

  /**
   * Determine whether the request was handled synchronously.
   * If the request is synchronous, all work was completed prior to returning.
   *
   * @return true if the request was synchronous, false if it was asynchronous
   */
  public boolean isSynchronous();

  public ResultStatus getStatus();

  public void setResultStatus(ResultStatus status);
}
