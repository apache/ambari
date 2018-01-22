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


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class MpackVersion implements Comparable<MpackVersion> {

  private final static String VERSION_WITH_HOTFIX_AND_BUILD_PATTERN = "^([0-9]+).([0-9]+).([0-9]+)-h([0-9]+)-b([0-9]+)";
  private final static String VERSION_WITH_BUILD_PATTERN = "^([0-9]+).([0-9]+).([0-9]+)-b([0-9]+)";
  private final static String LEGACY_STACK_VERSION_PATTERN = "^([0-9]+).([0-9]+).([0-9]+).([0-9]+)-([0-9]+)";

  private final static Pattern PATTERN_WITH_HOTFIX = Pattern.compile(VERSION_WITH_HOTFIX_AND_BUILD_PATTERN);
  private final static Pattern PATTERN_LEGACY_STACK_VERSION = Pattern.compile(LEGACY_STACK_VERSION_PATTERN);
  private final static Pattern PATTERN_WITHOUT_HOTFIX = Pattern.compile(VERSION_WITH_BUILD_PATTERN);

  private int major;
  private int minor;
  private int maint;
  private int hotfix;
  private int build;


  public MpackVersion(int major, int minor, int maint, int hotfix, int build) {
    this.major = major;
    this.minor = minor;
    this.maint = maint;
    this.hotfix = hotfix;
    this.build = build;
  }

  public static MpackVersion parse(String mpackVersion) {
    Matcher versionMatcher = validateMpackVersion(mpackVersion);
    MpackVersion result = null;

    if (versionMatcher.pattern().pattern().equals(VERSION_WITH_BUILD_PATTERN)) {
      result = new MpackVersion(Integer.parseInt(versionMatcher.group(1)), Integer.parseInt(versionMatcher.group(2)),
              Integer.parseInt(versionMatcher.group(3)), 0, Integer.parseInt(versionMatcher.group(4)));

    } else {
      result = new MpackVersion(Integer.parseInt(versionMatcher.group(1)), Integer.parseInt(versionMatcher.group(2)),
              Integer.parseInt(versionMatcher.group(3)), Integer.parseInt(versionMatcher.group(4)), Integer.parseInt(versionMatcher.group(5)));

    }

    return result;
  }

  public static MpackVersion parseStackVersion(String stackVersion) {
    Matcher versionMatcher = validateStackVersion(stackVersion);
    MpackVersion result = new MpackVersion(Integer.parseInt(versionMatcher.group(1)), Integer.parseInt(versionMatcher.group(2)),
          Integer.parseInt(versionMatcher.group(3)), Integer.parseInt(versionMatcher.group(4)), Integer.parseInt(versionMatcher.group(5)));

    return result;
  }

  private static Matcher validateStackVersion(String version) {
    if (StringUtils.isEmpty(version)) {
      throw new IllegalArgumentException("Stack version can't be empty or null");
    }

    String stackVersion = StringUtils.trim(version);

    Matcher versionMatcher = PATTERN_WITH_HOTFIX.matcher(stackVersion);
    if (!versionMatcher.find()) {
      versionMatcher = PATTERN_LEGACY_STACK_VERSION.matcher(stackVersion);
      if (!versionMatcher.find()) {
        throw new IllegalArgumentException("Wrong format for stack version, should be N.N.N.N-N or N.N.N-hN-bN");
      }
    }

    return versionMatcher;
  }

  private static Matcher validateMpackVersion(String version) {
    if (StringUtils.isEmpty(version)) {
      throw new IllegalArgumentException("Mpack version can't be empty or null");
    }

    String mpackVersion = StringUtils.trim(version);

    Matcher versionMatcher = PATTERN_WITH_HOTFIX.matcher(mpackVersion);
    if (!versionMatcher.find()) {
      versionMatcher = PATTERN_WITHOUT_HOTFIX.matcher(mpackVersion);
      if (!versionMatcher.find()) {
        throw new IllegalArgumentException("Wrong format for mpack version, should be N.N.N-bN or N.N.N-hN-bN");
      }
    }

    return versionMatcher;
  }

  @Override
  public int compareTo(MpackVersion other) {
    int result = this.major - other.major;
    if(result == 0) {
      result = this.minor - other.minor;
      if(result == 0) {
        result = this.maint - other.maint;
        if(result == 0) {
          result = this.hotfix - other.hotfix;
          if(result == 0) {
            result = this.build - other.build;
          }
        }
      }
    }
    return result > 0 ? 1 : result < 0 ? -1 : 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MpackVersion that = (MpackVersion) o;

    if (build != that.build) return false;
    if (hotfix != that.hotfix) return false;
    if (maint != that.maint) return false;
    if (major != that.major) return false;
    if (minor != that.minor) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = major;
    result = 31 * result + minor;
    result = 31 * result + maint;
    result = 31 * result + hotfix;
    result = 31 * result + build;
    return result;
  }
}
