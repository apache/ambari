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

package org.apache.ambari.view.hive2.internal;

import com.google.common.base.Optional;
import org.apache.hive.jdbc.HiveConnection;

/**
 * Life cycle management for java.sql.Connection
 */
public interface Connectable  {

    /**
     * Get the underlying connection
     * @return an optional wrapping the connection
     */
    Optional<HiveConnection> getConnection();

    /**
     * Check if the connection is open
     * @return
     */
    boolean isOpen();

    /**
     * Open a connection
     * @throws ConnectionException
     */
    void connect() throws ConnectionException;

    /**
     * Reconnect if closed
     * @throws ConnectionException
     */
    void reconnect() throws ConnectionException;

    /**
     * Close the connection
     * @throws ConnectionException
     */
    void disconnect() throws ConnectionException;

    /**
     * True when the connection is unauthorized
     * @return
     */
    boolean isUnauthorized();

}
