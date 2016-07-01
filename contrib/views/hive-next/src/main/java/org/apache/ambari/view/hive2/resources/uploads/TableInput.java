/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive2.resources.uploads;

import org.apache.ambari.view.hive2.resources.uploads.query.TableInfo;

/**
 * used as input in REST call
 */
class TableInput extends TableInfo {
  public Boolean isFirstRowHeader = Boolean.FALSE;

  public TableInput() {
  }

  public Boolean getIsFirstRowHeader() {
    return isFirstRowHeader;
  }

  public void setIsFirstRowHeader(Boolean isFirstRowHeader) {
    this.isFirstRowHeader = isFirstRowHeader;
  }

  public void validate(){
    if( null == this.getHiveFileType()){
      throw new IllegalArgumentException("fileType parameter cannot be null.");
    }
    if( null == this.getTableName()){
      throw new IllegalArgumentException("tableName parameter cannot be null.");
    }
    if( null == this.getDatabaseName()){
      throw new IllegalArgumentException("databaseName parameter cannot be null.");
    }
  }
}
