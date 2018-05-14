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

package org.apache.ambari.view.hive20.internal.query.generators;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import org.apache.ambari.view.hive20.internal.dto.ColumnInfo;
import org.apache.ambari.view.hive20.internal.dto.ColumnOrder;
import org.apache.ambari.view.hive20.internal.dto.TableMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;

import static org.apache.ambari.view.hive20.internal.query.generators.QueryGenerationUtils.isNullOrEmpty;

public class AlterTableQueryGenerator implements QueryGenerator {
  private static final Logger LOG = LoggerFactory.getLogger(AlterTableQueryGenerator.class);

  public static List<String> SYSTEM_PROPERTY_LIST = Arrays.asList("last_modified_time", "transient_lastDdlTime", "last_modified_by", "numRows", "numFiles", "rawDataSize", "totalSize", "COLUMN_STATS_ACCURATE");

  private final TableMeta oldMeta;
  private final TableMeta newMeta;

  public AlterTableQueryGenerator(TableMeta oldMeta, TableMeta newMeta) {
    this.oldMeta = oldMeta;
    this.newMeta = newMeta;
  }

  public TableMeta getOldMeta() {
    return oldMeta;
  }

  public TableMeta getNewMeta() {
    return newMeta;
  }

  public String getQueryPerfix() {
    return new StringBuffer(" ALTER TABLE ")
      .append("`").append(this.getOldMeta().getDatabase()).append("`.`").append(this.getOldMeta().getTable().trim()).append("` ").toString();
  }

  public Optional<String> getQuery() {
    List<Optional<String>> queries = new LinkedList<>();

    Optional<List<Optional<String>>> columnQuery = this.generateColumnQuery();
    if (columnQuery.isPresent()) {
      queries.addAll(columnQuery.get());
    }

    if (null != this.getNewMeta().getDetailedInfo() && null != this.getNewMeta().getDetailedInfo()) {
      Optional<String> tablePropertiesQuery = this.generateTablePropertiesQuery(this.getOldMeta().getDetailedInfo().getParameters(),
        this.getNewMeta().getDetailedInfo().getParameters());
      queries.add(tablePropertiesQuery);
    }

    // storage change is not required to be handled.
//    if (null != this.getOldMeta().getStorageInfo() && null != this.getNewMeta().getStorageInfo()) {
//      String oldSerde = this.getOldMeta().getStorageInfo().getSerdeLibrary();
//      String newSerde = this.getNewMeta().getStorageInfo().getSerdeLibrary();
//      Map<String, String> oldParameters = this.getOldMeta().getStorageInfo().getParameters();
//      Map<String, String> newParameters = this.getNewMeta().getStorageInfo().getParameters();
//
//      Optional<String> serdeProperties = this.generateSerdeQuery(oldSerde, oldParameters, newSerde, newParameters);
//      queries.add(serdeProperties);
//    }

    // change of bucketed columns is not required right now
//    if (null != this.getOldMeta().getStorageInfo() && null != this.getNewMeta().getStorageInfo()) {
//      List<String> oldBucketCols = this.getOldMeta().getStorageInfo().getBucketCols();
//      List<ColumnOrder> oldSortCols = this.getOldMeta().getStorageInfo().getSortCols();
//      String oldNumBuckets = this.getOldMeta().getStorageInfo().getNumBuckets();
//
//      List<String> newBucketCols = this.getNewMeta().getStorageInfo().getBucketCols();
//      List<ColumnOrder> newSortCols = this.getNewMeta().getStorageInfo().getSortCols();
//      String newNumBuckets = this.getNewMeta().getStorageInfo().getNumBuckets();
//
//      Optional<String> storagePropertyQuery = this.generateStoragePropertyQuery(oldBucketCols, oldSortCols, oldNumBuckets, newBucketCols, newSortCols, newNumBuckets);
//      queries.add(storagePropertyQuery);
//    }


    List<String> queryList = FluentIterable.from(queries).filter(new Predicate<Optional<String>>() {
      @Override
      public boolean apply(Optional<String> input) {
        return input.isPresent();
      }
    }).transform(new Function<Optional<String>, String>() {
      @Override
      public String apply(Optional<String> input) {
          return input.get();
      }
    }).toList();

    if (!queryList.isEmpty()) {
      return Optional.of(Joiner.on(";\n").join(queryList));
    } else {
      return Optional.absent();
    }

  }

  Optional<List<Optional<String>>> generateColumnQuery() {
    List<ColumnInfo> oldColumns = this.getOldMeta().getColumns();
    List<ColumnInfo> newColumns = this.getNewMeta().getColumns();
    boolean cascade = null != this.getNewMeta().getPartitionInfo() && !isNullOrEmpty(this.getNewMeta().getPartitionInfo().getColumns());
    Optional<List<String>> queries = createColumnQueries(oldColumns, newColumns, cascade);
    if (queries.isPresent()) {
      List<Optional<String>> queryList = FluentIterable.from(queries.get()).transform(new Function<String, Optional<String>>() {
        @Override
        public Optional<String> apply(String input) {
          return Optional.of(getQueryPerfix() + input);
        }
      }).toList();
      return Optional.of(queryList);
    } else {
      return Optional.absent();
    }
  }

  /**
   * TODO : this uses CASCADE. confirm that it is expected.
   * ALTER TABLE table_name [PARTITION partition_spec] CHANGE [COLUMN] col_old_name col_new_name column_type
   * [COMMENT col_comment] [FIRST|AFTER column_name] [CASCADE|RESTRICT];
   * <p>
   * ALTER TABLE table_name
   * [PARTITION partition_spec]                 -- (Note: Hive 0.14.0 and later)
   * ADD|REPLACE COLUMNS (col_name data_type [COMMENT col_comment], ...)
   * [CASCADE|RESTRICT]                         -- (Note: Hive 0.15.0 and later)
   *
   * @param oldColumns
   * @param newColumns
   * @return
   */
  static Optional<List<String>> createColumnQueries(List<ColumnInfo> oldColumns, List<ColumnInfo> newColumns, boolean cascade) {
    if (isNullOrEmpty(oldColumns) || isNullOrEmpty(newColumns)) {
      LOG.error("oldColumns = {} or newColumns = {} was null.", oldColumns, newColumns);
      throw new IllegalArgumentException("Old or new columns cannot be empty.");
    }

    //TODO : removing columns not allowed right now. handle this later using REPLACE for native serde or error.
    if (oldColumns.size() > newColumns.size()) {
      LOG.error("removing columns from hive table is not supported yet.");
      throw new IllegalArgumentException("removing columns is not allowed.");
    }

    List<String> queries = new LinkedList<>();
    int i = 0;
    boolean foundChange = false;
    for (; i < oldColumns.size(); i++) {
      ColumnInfo oldColumn = oldColumns.get(i);
      ColumnInfo newColumn = newColumns.get(i);

      if (!oldColumn.equals(newColumn)) {
        foundChange = true;
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" CHANGE COLUMN `").append(oldColumn.getName()).append("` ")
          .append(QueryGenerationUtils.getColumnRepresentation(newColumn));

          if(cascade){
            queryBuilder.append(" CASCADE");
          }

        queries.add(queryBuilder.toString());
      }
    }

    if (i < newColumns.size()) {
      StringBuilder queryBuilder = new StringBuilder();
      queryBuilder.append(" ADD COLUMNS ( ");
      boolean first = true;
      for (; i < newColumns.size(); i++) {
        foundChange = true;
        ColumnInfo columnInfo = newColumns.get(i);
        if (!first) {
          queryBuilder.append(", ");
        } else {
          first = false;
        }

        queryBuilder.append(QueryGenerationUtils.getColumnRepresentation(columnInfo));
      }
      queryBuilder.append(" )");

      if(cascade){
        queryBuilder.append(" CASCADE");
      }

      queries.add(queryBuilder.toString());
    }

    if (foundChange) {
      return Optional.of(queries);
    } else {
      return Optional.absent();
    }
  }

  Optional<String> generateStoragePropertyQuery(List<String> oldBucketCols, List<ColumnOrder> oldSortCols, String oldNumBuckets, List<String> newBucketCols, List<ColumnOrder> newSortCols, String newNumBuckets) {
    Optional<String> query = createStoragePropertyQuery(oldBucketCols, oldSortCols, oldNumBuckets, newBucketCols, newSortCols, newNumBuckets);
    if (query.isPresent()) return Optional.of(getQueryPerfix() + query.get());
    else return Optional.absent();
  }

  /**
   * ALTER TABLE table_name CLUSTERED BY (col_name, col_name, ...) [SORTED BY (col_name, ...)]
   * INTO num_buckets BUCKETS;
   *
   * @param oldBucketCols
   * @param oldSortCols
   * @param oldNumBuckets
   * @param newBucketCols
   * @param newSortCols
   * @param newNumBuckets
   * @return
   */
  static Optional<String> createStoragePropertyQuery(List<String> oldBucketCols, List<ColumnOrder> oldSortCols, String oldNumBuckets, List<String> newBucketCols, List<ColumnOrder> newSortCols, String newNumBuckets) {
    StringBuilder queryBuilder = new StringBuilder();
    boolean foundDiff = false;

    if (isNullOrEmpty(newBucketCols)) {
      if (!isNullOrEmpty(oldBucketCols)) {
        // TODO : all cols removed. how to handle this. Ignoring
        LOG.error("cannot handle removal of all the columns from buckets.");
        throw new IllegalArgumentException("removing all columns from CLUSTERED BY not allowed.");
      } else {
        // NOTHING ADDED to CLUSTERED BY.
        return Optional.absent();
      }
    } else {
      queryBuilder.append(" CLUSTERED BY ( ").append(Joiner.on(",").join(newBucketCols)).append(" ) ");
    }

    if (!isNullOrEmpty(newSortCols)) {
      queryBuilder.append(" SORTED BY ( ")
        .append(Joiner.on(",").join(FluentIterable.from(newSortCols).transform(new Function<ColumnOrder, String>() {
          @Nullable
          @Override
          public String apply(@Nullable ColumnOrder input) {
            return input.getColumnName() + " " + input.getOrder().name();
          }
        })))
        .append(" ) ");
    }

    if (Strings.isNullOrEmpty(newNumBuckets)) {
      LOG.error("Number of buckets cannot be empty if CLUSTERED BY is mentioned.");
      throw new IllegalArgumentException("Number of buckets cannot be empty.");
    } else {
      queryBuilder.append(" INTO ").append(newNumBuckets).append(" BUCKETS ");
    }

    return Optional.of(queryBuilder.toString());
  }

  Optional<String> generateSerdeQuery(String oldSerde, Map<String, String> oldParameters, String newSerde, Map<String, String> newParameters) {
    Optional<String> query = createSerdeQuery(oldSerde, oldParameters, newSerde, newParameters);
    if (query.isPresent()) return Optional.of(getQueryPerfix() + query.get());
    else return Optional.absent();
  }

  /**
   * assuming that getStorageInfo().getParameters() gives only serde properties
   *
   * @return
   */
  static Optional<String> createSerdeQuery(String oldSerde, Map<String, String> oldParameters, String newSerde, Map<String, String> newParameters) {
    String query = "";
    boolean serdeChanged = false;
    if (null != newSerde) {
      serdeChanged = !newSerde.equals(oldSerde);
      query += " SET SERDE " + newSerde + " ";
    }
    Optional<Map<String, Map<Object, Object>>> diff = QueryGenerationUtils.findDiff(oldParameters, newParameters);
    if (diff.isPresent()) {
      Map<String, Map<Object, Object>> diffMap = diff.get();
      Map<Object, Object> added = diffMap.get(QueryGenerationUtils.ADDED);
      Map<Object, Object> modified = diffMap.get(QueryGenerationUtils.MODIFIED);
      Map<Object, Object> deleted = diffMap.get(QueryGenerationUtils.DELETED);

      // TODO : how to handle deleted? actually I cannot find anything in hive alter table that will remove existing property
      Map addedOrModified = new HashMap<>(added);
      addedOrModified.putAll(modified);

      if (serdeChanged) {
        query += " WITH SERDEPROPERTIES ";
      } else {
        query += " SET SERDEPROPERTIES ";
      }
      query += " ( " + QueryGenerationUtils.getPropertiesAsKeyValues(addedOrModified) + " ) ";
    }

    if (!query.trim().isEmpty()) {
      return Optional.of(query);
    }

    return Optional.absent();
  }

  Optional<String> generateTablePropertiesQuery(Map<String, String> oldProps, Map<String, String> newProps) {
    Optional<String> query = createTablePropertiesQuery(oldProps, newProps);
    if (query.isPresent()) return Optional.of(getQueryPerfix() + query.get());
    else return Optional.absent();
  }


  static Optional<String> createTablePropertiesQuery(Map<String, String> oldProps, Map<String, String> newProps) {
    if( null == newProps && null == oldProps){
      return Optional.absent();
    }

    if (null == newProps) {
      newProps = new HashMap<>();
    }

    if(null == oldProps){
      oldProps = new HashMap<>();
    }
    // ignore system generated table properties during comparison

   for(String prop : SYSTEM_PROPERTY_LIST){
      newProps.remove(prop);
      oldProps.remove(prop);
   }

    if (!QueryGenerationUtils.isEqual(oldProps, newProps) && !newProps.isEmpty()) {
      return Optional.of(" SET TBLPROPERTIES (" + QueryGenerationUtils.getPropertiesAsKeyValues(newProps) + ")");
    }

    return Optional.absent();
  }

  Optional<String> generateTableRenameQuery(String oldDatabaseName, String oldTableName, String newDatabaseName, String newTableName) {
    Optional<String> query = createTableRenameQuery(oldDatabaseName, oldTableName, newDatabaseName, newTableName);
    if (query.isPresent()) return Optional.of(getQueryPerfix() + query.get());
    else return Optional.absent();
  }

  static Optional<String> createTableRenameQuery(String oldDatabaseName, String oldTableName, String newDatabaseName, String newTableName) {
    if (Strings.isNullOrEmpty(oldTableName) || Strings.isNullOrEmpty(newTableName)) {
      LOG.error("oldTableName or newTableName is empty : {}, {} ", oldTableName, newTableName);
      throw new IllegalArgumentException("oldTableName and newTableName both should be non empty.");
    }

    String oldName = (null != oldDatabaseName ? oldDatabaseName.trim() + "." : "") + oldTableName.trim();
    String newName = (null != newDatabaseName ? newDatabaseName.trim() + "." : "") + newTableName.trim();

    if (!oldName.equals(newName)) {
      return Optional.of(" RENAME TO " + newName);
    }

    return Optional.absent();
  }
}
