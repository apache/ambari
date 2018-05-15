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

import com.google.common.collect.Lists;

import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class HiveResult implements Iterator<HiveResult.Row> {

    public static final String NULL = "NULL";
    private static final int DEFAULT_BATCH_SIZE = 50;
    private static ResultSetMetaData metaData;
    private Row colNames;
    private NumberFormat nf = new DecimalFormat();
    private List<Row> rows = Lists.newArrayList();

    public HiveResult(ResultSet rs) throws SQLException {
        nf.setRoundingMode(RoundingMode.FLOOR);
        nf.setMinimumFractionDigits(0);
        nf.setMaximumFractionDigits(2);
        metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        colNames = new Row(columnCount);
        int index = 0;
        while (rs.next() && index <DEFAULT_BATCH_SIZE){
            index ++;
            rows.add(new Row(columnCount,rs));
        }


    }

    public List<Row> getRows(){
        return rows;
    }

    public List<Row> getData() {
        return rows;
    }

    /**
     * use the lists iterator
     *
     * @return {@code true} if the iteration has more elements
     */
    @Override
    public boolean hasNext() {
        return rows.iterator().hasNext();
    }

    /**
     * Returns the next row in the iteration.
     *
     * @return the next element in the iteration
     */
    @Override
    public Row next() {
        return rows.iterator().next();
    }

    /**
     * Removes from the underlying collection the last element returned
     * by this iterator (optional operation).  This method can be called
     * only once per call to {@link #next}.  The behavior of an iterator
     * is unspecified if the underlying collection is modified while the
     * iteration is in progress in any way other than by calling this
     * method.
     *
     * @throws UnsupportedOperationException if the {@code remove}
     *                                       operation is not supported by this iterator
     * @throws IllegalStateException         if the {@code next} method has not
     *                                       yet been called, or the {@code remove} method has already
     *                                       been called after the last call to the {@code next}
     *                                       method
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    public Row getColNames() {
        return colNames;
    }


    @Override
    public String toString() {
        return "HiveResult{" +
                "colNames=" + colNames +
                ", rows=" + rows +
                '}';
    }

    public class Row {
        String[] values;

        public Row(int size) throws SQLException {
            values = new String[size];
            for (int i = 0; i < size; i++) {
                values[i] = metaData.getColumnLabel(i + 1);
            }
        }


        public Row(int size, ResultSet rs) throws SQLException {
            values = new String[size];
            for (int i = 0; i < size; i++) {
                if (nf != null) {
                    Object object = rs.getObject(i + 1);
                    if (object == null) {
                        values[i] = null;
                    } else if (object instanceof Number) {
                        values[i] = nf.format(object);
                    } else {
                        values[i] = object.toString();
                    }
                } else {
                    values[i] = rs.getString(i + 1);
                }
                values[i] = values[i] == null ? NULL : values[i];

            }

        }

        @Override
        public String toString() {
            return "Row{" +
                    "values=" + Arrays.toString(values) +
                    '}';
        }
    }


}
