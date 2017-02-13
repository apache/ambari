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
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import org.apache.ambari.view.hive20.internal.dto.ColumnInfo;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QueryGenerationUtils {

  public static final String ADDED = "ADDED";
  public static final String DELETED = "DELETED";
  public static final String MODIFIED = "MODIFIED";

  public static boolean isNullOrEmpty(Map map) {
    return null != map && !map.isEmpty();
  }

  public static boolean isNullOrEmpty(Collection collection) {
    return null == collection || collection.isEmpty();
  }

  public static boolean isEqual(Map oldProps, Map newProps) {
    if(oldProps == null && newProps == null) return true;

    if(oldProps != null && newProps != null){
      if(oldProps.size() != newProps.size()) return false;

      Set<Map.Entry> entrySet = oldProps.entrySet();
      for(Map.Entry e : entrySet){
        Object key = e.getKey();
        if(oldProps.get(key) == null){
          if(newProps.get(key) != null) return false;
        }else {
          if (newProps.get(key) == null || !newProps.get(key).equals(oldProps.get(key))) {
            return false;
          }
        }
      }
    }

    return true;
  }

  /**
   * return a map with 3 keys "DELETED" and "ADDED" and "MODIFIED" to show the different between oldProps and newProps
   * for "ADDED" and "MODIFIED" the values in map are of newProps
   * @param oldProps
   * @param newProps
   * @return
   */
  public static Optional<Map<String, Map<Object,Object>>> findDiff(Map oldProps, Map newProps) {
    Map<String, Map<Object, Object>> ret = new HashMap<>();
    Map<Object, Object> added = new HashMap<>();
    Map<Object, Object> modified = new HashMap<>();
    Map<Object, Object> deleted = new HashMap<>();

    if(oldProps == null && newProps == null) return Optional.absent();

    if(oldProps == null && newProps != null){
      oldProps = new HashMap();
    }
    if(oldProps != null && newProps != null){
      Set<Map.Entry> entrySet = oldProps.entrySet();
      for(Map.Entry e : entrySet){
        Object key = e.getKey();
        Object newValue = newProps.get(key);
        if(e.getValue() == null){
          if( newValue != null){
            added.put(key, newValue);
          }
        }else {
          if (newValue == null) {
            deleted.put(key, newValue);
          }else if (!e.getValue().equals(newValue)){
            modified.put(key, newValue);
          }
        }
      }

      Set<Map.Entry> newEntrySet = newProps.entrySet();
      for(Map.Entry e : newEntrySet){
        if(e.getValue() != null && oldProps.get(e.getKey()) == null){
          added.put(e.getKey(), e.getValue());
        }
      }
    }
    ret.put(ADDED, added);
    ret.put(DELETED, deleted);
    ret.put(MODIFIED, modified);

    return Optional.of(ret);
  }

  public static String getPropertiesAsKeyValues(Map<String, String> parameters) {
    List<String> props = (List<String>) FluentIterable.from(parameters.entrySet())
            .transform(new Function<Map.Entry<String, String>, String>() {
              @Nullable
              @Override
              public String apply(@Nullable Map.Entry<String, String> entry) {
                return "'" + entry.getKey() + "'='" + entry.getValue() + "'";
              }
            }).toList();

    return Joiner.on(",").join(props);
  }

  public static String getColumnRepresentation(ColumnInfo column) {
    StringBuilder colQuery = new StringBuilder().append("`").append(column.getName()).append("`");
    colQuery.append(" ").append(column.getType());
    if(!QueryGenerationUtils.isNullOrZero(column.getPrecision())){
      if(!QueryGenerationUtils.isNullOrZero(column.getScale())){
        colQuery.append("(").append(column.getPrecision()).append(",").append(column.getScale()).append(")");
      }else{
        colQuery.append("(").append(column.getPrecision()).append(")");
      }
    }
    if(!Strings.isNullOrEmpty(column.getComment())) {
      colQuery.append(" COMMENT '").append(column.getComment()).append("'");
    }

    return colQuery.toString();
  }

  public static boolean isNullOrZero(Integer integer) {
    return null == integer || 0 == integer;
  }
}
