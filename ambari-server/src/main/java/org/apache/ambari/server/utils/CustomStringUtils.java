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

import java.util.List;
import java.util.ListIterator;

public class CustomStringUtils {
  /**
   * <code>CustomStringUtils</code> instances should NOT be constructed in
   * standard programming. Instead, the class should be used as
   * <code>CustomStringUtils.containsCaseInsensitive("foo", arrayList)</code>
   */
  public CustomStringUtils(){
    super();
  }

  /**
   * Returns <tt>true</tt> if this list contains the specified string element, ignoring case considerations.
   * @param s element whose presence in this list is to be tested
   * @param l list of strings, where presence of string element would be checked
   * @return <tt>true</tt> if this list contains the specified element
   */
  public static boolean containsCaseInsensitive(String s, List<String> l){
    for (String listItem : l){
      if (listItem.equalsIgnoreCase(s)){
        return true;
      }
    }
    return false;
  }

  /**
   * Make list of string lowercase
   * @param l list of strings, which need to be in lowercase
   */
  public static void toLowerCase(List<String> l) {
    ListIterator<String> iterator = l.listIterator();
    while (iterator.hasNext()) {
      iterator.set(iterator.next().toLowerCase());
    }
  }

  /**
   * Make list of string lowercase
   * @param l list of strings, which need to be in lowercase
   */
  public static void toUpperCase(List<String> l) {
    ListIterator<String> iterator = l.listIterator();
    while (iterator.hasNext()) {
      iterator.set(iterator.next().toUpperCase());
    }
  }

  /**
   * Insert a string after a substring
   * @param toInsertInto the base string to be changed
   * @param addAfter insert a string after this if found in <code>toInsertInto</code>
   * @param toInsert insert this string
   * @return if the <code>addAfter</code> argument occurs as a substring within this <code>toInsertInto</code>,
   * then the index of the first character of the first such substring is returned; if it does not occur as a substring, -1 is returned.
   */
  public static int insertAfter(StringBuilder toInsertInto, String addAfter, String toInsert) {
    int index = toInsertInto.indexOf(addAfter);
    if (index > -1) {
      toInsertInto.insert(index + addAfter.length(), toInsert);
    }
    return index;
  }

  /**
   * Insert a string after a substring if a specified string is not present already.
   * @param toInsertInto the base string to be changed
   * @param addAfter insert a string after this if found in <code>toInsertInto</code>
   * @param toInsert insert this string
   * @param ifNotThere only do the insert if this string is not found
   * @return if the <code>addAfter</code> argument occurs as a substring within this <code>toInsertInto</code>,
   * then the index of the first character of the first such substring is returned; if it does not occur as a substring, -1 is returned.
   * If the <code>ifNotThere</code> exists already in <code>toInsertInto</code>, -2 is returned.
   */
  public static int insertAfterIfNotThere(StringBuilder toInsertInto, String addAfter, String toInsert, String ifNotThere) {
    if (toInsertInto.indexOf(ifNotThere) > -1) return -2;
    return insertAfter(toInsertInto, addAfter, toInsert);
  }

  /**
   * Insert a string after a substring if the string is not present already.
   * @param toInsertInto the base string to be changed
   * @param addAfter insert a string after this if found in <code>toInsertInto</code>
   * @param toInsert insert this string
   * @return if the <code>addAfter</code> argument occurs as a substring within this <code>toInsertInto</code>,
   * then the index of the first character of the first such substring is returned; if it does not occur as a substring, -1 is returned.
   * If the <code>toInsert</code> exists already in <code>toInsertInto</code>, -2 is returned.
   */
  public static int insertAfterIfNotThere(StringBuilder toInsertInto, String addAfter, String toInsert) {
    return insertAfterIfNotThere(toInsertInto, addAfter, toInsert, toInsert);
  }

  /**
   * Delete a substring
   * @param toDeleteFrom the base string to be changed
   * @param toDelete delete this string from <code>toDeleteFrom</code> if found
   * @return if the <code>toDelete</code> argument occurs as a substring within this <code>toDeleteFrom</code>,
   * then the index of the first character of the first such substring is returned; if it does not occur as a substring, -1 is returned.
   */
  public static  int deleteSubstring(StringBuilder toDeleteFrom, String toDelete) {
    int index = toDeleteFrom.indexOf(toDelete);
    if (index > -1) {
      toDeleteFrom.delete(index, index + toDelete.length());
    }
    return index;
  }

  /**
   * Replace a substring
   * @param replaceIn the base string to be changed
   * @param toFind replace this string with <code>toReplace</code> if found
   * @param toReplace replace <code>toFind</code> string with this
   * @return if the <code>toFind</code> argument occurs as a substring within this <code>replaceIn</code>,
   * then the index of the first character of the first such substring is returned; if it does not occur as a substring, -1 is returned.
   */
  public static int replace(StringBuilder replaceIn, String toFind, String toReplace) {
    int index = replaceIn.indexOf(toFind);
    if (index > -1) {
      replaceIn.replace(index, index + toFind.length(), toReplace);
    }
    return index;
  }

  /**
   * Replace a substring if a specified string is not present already.
   * @param replaceIn the base string to be changed
   * @param toFind replace this string with <code>toReplace</code> if found
   * @param toReplace replace <code>toFind</code> string with this
   * @param ifNotThere only do the replace if this string is not found
   * @return if the <code>toFind</code> argument occurs as a substring within this <code>replaceIn</code>,
   * then the index of the first character of the first such substring is returned; if it does not occur as a substring, -1 is returned.
   * If the <code>ifNotThere</code> exists already in <code>replaceIn</code>, -2 is returned.
   */
  public static int replaceIfNotThere(StringBuilder replaceIn, String toFind, String toReplace, String ifNotThere) {
    if (replaceIn.indexOf(ifNotThere) > -1) return -2;
    return replace(replaceIn, toFind, toReplace);
  }

  /**
   * Replace a substring if a the string is not present already.
   * @param replaceIn the base string to be changed
   * @param toFind replace this string with <code>toReplace</code> if found
   * @param toReplace replace <code>toFind</code> string with this
   * @return if the <code>toFind</code> argument occurs as a substring within this <code>replaceIn</code>,
   * then the index of the first character of the first such substring is returned; if it does not occur as a substring, -1 is returned.
   * If the <code>toReplace</code> exists already in <code>replaceIn</code>, -2 is returned.
   */
  public static int replaceIfNotThere(StringBuilder replaceIn, String toFind, String toReplace) {
    return replaceIfNotThere(replaceIn, toFind, toReplace, toReplace);
  }
}
