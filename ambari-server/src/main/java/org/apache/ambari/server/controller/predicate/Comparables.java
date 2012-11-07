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

package org.apache.ambari.server.controller.predicate;

/**
 *
 */
public class Comparables {

  public static Comparable<String> forInteger(final Integer value) {

    return new Comparable<String>() {
      @Override
      public int compareTo(String s) {
        return value.compareTo(Integer.valueOf(s));
      }

      @Override
      public boolean equals(Object obj) {
        if (obj == null) {
          return false;
        }
        if (obj == value) {
          return true;
        }
        if (obj instanceof String) {
          try {
            return value.equals(Integer.valueOf((String)obj));
          } catch (NumberFormatException nfe) {
            return false;
          }
        }
        if (obj instanceof  Integer) {
          return value.equals((Integer)obj);
        }
        return  false;
      }

      @Override
      public int hashCode() {
        return value.toString().hashCode();
      }
    };
  }

  public static Comparable<String> forFloat(final Float value) {

    return new Comparable<String>() {
      @Override
      public int compareTo(String s) {
        return value.compareTo(Float.valueOf(s));
      }

      @Override
      public boolean equals(Object obj) {
        if (obj == null) {
          return false;
        }
        if (obj == value) {
          return true;
        }
        if (obj instanceof String) {
          try {
            return value.equals(Float.valueOf((String)obj));
          } catch (NumberFormatException nfe) {
            return false;
          }
        }
        if (obj instanceof  Float) {
          return value.equals((Float)obj);
        }
        return  false;
      }

      @Override
      public int hashCode() {
        return value.toString().hashCode();
      }
    };
  }

  public static Comparable<String> forDouble(final Double value) {

    return new Comparable<String>() {
      @Override
      public int compareTo(String s) {
        return value.compareTo(Double.valueOf(s));
      }

      @Override
      public boolean equals(Object obj) {
        if (obj == null) {
          return false;
        }
        if (obj == value) {
          return true;
        }
        if (obj instanceof String) {
          try {
            return value.equals(Double.valueOf((String)obj));
          } catch (NumberFormatException nfe) {
            return false;
          }
        }
        if (obj instanceof  Double) {
          return value.equals((Double)obj);
        }
        return  false;
      }

      @Override
      public int hashCode() {
        return value.toString().hashCode();
      }

    };
  }

  public static Comparable<String> forLong(final Long value) {

    return new Comparable<String>() {
      @Override
      public int compareTo(String s) {
        return value.compareTo(Long.valueOf(s));
      }

      @Override
      public boolean equals(Object obj) {
        if (obj == null) {
          return false;
        }
        if (obj == value) {
          return true;
        }
        if (obj instanceof String) {
          try {
            return value.equals(Long.valueOf((String)obj));
          } catch (NumberFormatException nfe) {
            return false;
          }
        }
        if (obj instanceof  Long) {
          return value.equals((Long)obj);
        }
        return  false;
      }

      @Override
      public int hashCode() {
        return value.toString().hashCode();
      }
    };
  }

}
