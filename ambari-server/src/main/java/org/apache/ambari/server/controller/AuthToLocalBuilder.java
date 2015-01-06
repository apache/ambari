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

package org.apache.ambari.server.controller;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AuthToLocalBuilder helps to create auth_to_local rules for use in configuration files like
 * core-site.xml.
 * <p/>
 * For each principal appended to the rule set, parse out the primary value and match it to a local
 * username.  Then when done appending all principals, generate the rules where each entry yields
 * one of the following rule:
 * <p/>
 * Qualified Principal: RULE:[2:$1@$0](PRIMARY@REALM)s/.*\/LOCAL_USERNAME/
 * <p/>
 * Unqualified Principal: RULE:[1:$1@$0](PRIMARY@REALM)s/.*\/LOCAL_USERNAME/
 */
public class AuthToLocalBuilder {

  /**
   * A Regular expression declaring a qualified principal such that the principal is in the following format:
   * primary/instance@REALM
   */
  private static final Pattern PATTERN_QUALIFIED_PRINCIPAL = Pattern.compile("(\\w+)/.*@.*");

  /**
   * A Regular expression declaring an un qualified principal such that the principal is in the following format:
   * primary@REALM
   */
  private static final Pattern PATTERN_UNQUALIFIED_PRINCIPAL = Pattern.compile("(\\w+)@.*");

  /**
   * A map of qualified principal names (primary/instance@REALM, with instance and @REALM removed).
   * <p/>
   * A TreeMap is used to help generate deterministic ordering of rules for testing.
   */
  private Map<String, String> qualifiedAuthToLocalMap = new TreeMap<String, String>();

  /**
   * A map of unqualified principal names (primary@REALM, with @REALM removed).
   * <p/>
   * A TreeMap is used to help generate deterministic ordering of rules for testing.
   */
  private Map<String, String> unqualifiedAuthToLocalMap = new TreeMap<String, String>();


  /**
   * Appends a principal and local username mapping to the builder.
   * <p/>
   * The supplied principal is parsed to determine if it is qualified or unqualified and stored
   * accordingly so that when the mapping rules are generated the appropriate rule is generated.
   * <p/>
   * If a principal is added that yields a duplicate primary principal value (relative to the set of
   * qualified or unqualified rules), that later entry will overwrite the older entry, allowing for
   * only one mapping rule.
   * <p/>
   * If the principal does not match one of the two expected patterns, it will be ignored.
   *
   * @param principal     a String containing the full principal to append
   * @param localUsername a String declaring that local username to map the principal to
   */
  public void append(String principal, String localUsername) {
    if ((principal != null) && (localUsername != null) && !principal.isEmpty() && !localUsername.isEmpty()) {
      // Determine if the principal is contains an instance declaration
      Matcher matcher;

      matcher = PATTERN_QUALIFIED_PRINCIPAL.matcher(principal);
      if (matcher.matches()) {
        qualifiedAuthToLocalMap.put(matcher.group(1), localUsername);
      } else {
        matcher = PATTERN_UNQUALIFIED_PRINCIPAL.matcher(principal);
        if (matcher.matches()) {
          unqualifiedAuthToLocalMap.put(matcher.group(1), localUsername);
        }
      }
    }
  }

  /**
   * Generates the auth_to_local rules used by configuration settings such as core-site/auth_to_local.
   *
   * @param realm a String declaring the realm to use in rule set
   *
   */
  public String generate(String realm) {

    StringBuilder builder = new StringBuilder();

    for (Map.Entry<String, String> entry : qualifiedAuthToLocalMap.entrySet()) {
      // RULE:[2:$1@$0](PRIMARY@REALM)s/.*/LOCAL_USERNAME/
      appendRule(builder, String.format("RULE:[2:$1@$0](%s@%s)s/.*/%s/", entry.getKey(), realm, entry.getValue()));
    }

    for (Map.Entry<String, String> entry : unqualifiedAuthToLocalMap.entrySet()) {
      // RULE:[1:$1@$0](PRIMARY@REALM)s/.*/LOCAL_USERNAME/
      appendRule(builder, String.format("RULE:[1:$1@$0](%s@%s)s/.*/%s/", entry.getKey(), realm, entry.getValue()));
    }

    // RULE:[1:$1@$0](.*@YOUR.REALM)s/@.*//
    appendRule(builder, String.format("RULE:[1:$1@$0](.*@%s)s/@.*//", realm));

    appendRule(builder, "DEFAULT");

    return builder.toString();
  }

  private void appendRule(StringBuilder stringBuilder, String rule) {
    if (stringBuilder.length() > 0) {
      stringBuilder.append('\n');
    }
    stringBuilder.append(rule);
  }
}
