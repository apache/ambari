/*
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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.infra.job.archive;

public class LocalItemWriterListener implements ItemWriterListener {
  private final FileAction fileAction;
  private final DocumentWiper documentWiper;

  public LocalItemWriterListener(FileAction fileAction, DocumentWiper documentWiper) {
    this.fileAction = fileAction;
    this.documentWiper = documentWiper;
  }


  @Override
  public void onCompleted(WriteCompletedEvent event) {
    fileAction.perform(event.getOutFile());
    documentWiper.delete(event.getFirstDocument(), event.getLastDocument());
  }
}
