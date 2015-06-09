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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AuthToLocalBuilder helps to create auth_to_local rules for use in configuration files like
 * core-site.xml.  No duplicate rules will be generated.
 * <p/>
 * Allows previously existing rules to be added verbatim.  Also allows new rules to be generated
 * based on a principal and local username.  For each principal added to the builder, generate
 * a rule conforming to one of the following formats:
 * <p/>
 * Qualified Principal (the principal contains a user and host):
 * RULE:[2:$1@$0](PRIMARY@REALM)s/.*\/LOCAL_USERNAME/
 * <p/>
 * Unqualified Principal (only user is specified):
 * RULE:[1:$1@$0](PRIMARY@REALM)s/.*\/LOCAL_USERNAME/
 * <p>
 * Additionally, for each realm included in the rule set, generate a default realm rule
 * in the format: RULE:[1:$1@$0](.*@REALM)s/@.{@literal *}//
 * <p>
 * Ordering guarantees for the generated rule string are as follows:
 * <ul>
 *   <li>Rules with the same expected component count are ordered according to match component count</li>
 *   <li>Rules with different expected component count are ordered according to the default string ordering</li>
 *   <li>Rules in the form of .*@REALM are ordered after all other rules with the same expected component count</li>
 * </ul>
 *
 */
public class AuthToLocalBuilder {
  public static final ConcatenationType DEFAULT_CONCATENATION_TYPE = ConcatenationType.NEW_LINES;

  /**
   * Ordered set of rules which have been added to the builder.
   */
  private Set<Rule> setRules = new TreeSet<Rule>();


  /**
   * A flag indicating whether case insensitive support to the local username has been requested. This will append an //L switch to the generic realm rule
   */
  private boolean caseInsensitiveUser;

  /**
   * Default constructor. Case insensitive support false by default
   */
  public AuthToLocalBuilder() {
    this.caseInsensitiveUser = false;
  }

  public AuthToLocalBuilder(boolean caseInsensitiveUserSupport) {
    this.caseInsensitiveUser = caseInsensitiveUserSupport;
  }

  /**
   * Add existing rules from the given authToLocal configuration property.
   * The rules are added verbatim.
   *
   * @param authToLocalRules config property value containing the existing rules
   */
  public void addRules(String authToLocalRules) {
    if (authToLocalRules != null && ! authToLocalRules.isEmpty()) {
      String[] rules = authToLocalRules.split("RULE:|DEFAULT");
      for (String r : rules) {
        r = r.trim();
        if (! r.isEmpty()) {
          Rule rule = createRule(r);
          setRules.add(rule);
          // ensure that a default rule is added for each realm
          addDefaultRealmRule(rule.getPrincipal());
        }
      }
    }
  }


  /**
   * Adds a rule for the given principal and local user.
   * The principal must contain a realm component.
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
   * @param principal     a string containing the full principal
   * @param localUsername a string declaring that local username to map the principal to
   * @throws IllegalArgumentException if the provided principal doesn't contain a realm element
   */
  public void addRule(String principal, String localUsername) {
    if ((principal != null) && (localUsername != null) &&
        !principal.isEmpty() && !localUsername.isEmpty()) {

      Principal p = new Principal(principal);
      if (p.getRealm() == null) {
        throw new IllegalArgumentException(
            "Attempted to add a rule for a principal with no realm: " + principal);
      }

      Rule rule = createHostAgnosticRule(p, localUsername);
      setRules.add(rule);
      addDefaultRealmRule(rule.getPrincipal());
    }
  }

  /**
   * Generates the auth_to_local rules used by configuration settings such as core-site/auth_to_local.
   * <p/>
   * Each rule is concatenated using the default ConcatenationType, like calling
   * {@link #generate(String, ConcatenationType)} with {@link #DEFAULT_CONCATENATION_TYPE}
   *
   * @param realm a string declaring the realm to use in rule set
   * @return a string containing the generated auth-to-local rule set
   */
  public String generate(String realm) {
    return generate(realm, null);
  }

  /**
   * Generates the auth_to_local rules used by configuration settings such as core-site/auth_to_local.
   * <p/>
   * Each rule is concatenated using the specified
   * {@link org.apache.ambari.server.controller.AuthToLocalBuilder.ConcatenationType}.
   * If the concatenation type is <code>null</code>, the default concatenation type is assumed -
   * see {@link #DEFAULT_CONCATENATION_TYPE}.
   *
   * @param realm             a string declaring the realm to use in rule set
   * @param concatenationType the concatenation type to use to generate the rule set string
   * @return a string containing the generated auth-to-local rule set
   */
  public String generate(String realm, ConcatenationType concatenationType) {
    StringBuilder builder = new StringBuilder();
    // ensure that a default rule is added for this realm
    setRules.add(createDefaultRealmRule(realm));

    if (concatenationType == null) {
      concatenationType = DEFAULT_CONCATENATION_TYPE;
    }

    for (Rule rule : setRules) {
      appendRule(builder, rule.toString(), concatenationType);
    }

    appendRule(builder, "DEFAULT", concatenationType);
    return builder.toString();
  }

  /**
   * Append a rule to the given string builder.
   *
   * @param stringBuilder     string builder to which rule is added
   * @param rule              rule to add
   * @param concatenationType the concatenation type to use to generate the rule set string
   */
  private void appendRule(StringBuilder stringBuilder, String rule, ConcatenationType concatenationType) {
    if (stringBuilder.length() > 0) {
      switch (concatenationType) {
        case NEW_LINES:
          stringBuilder.append('\n');
          break;
        case NEW_LINES_ESCAPED:
          stringBuilder.append("\\\n");
          break;
        case SPACES:
          stringBuilder.append(" ");
          break;
        default:
          throw new UnsupportedOperationException(String.format("The auth-to-local rule concatenation type is not supported: %s",
              concatenationType.name()));
      }
    }

    stringBuilder.append(rule);
  }

  /**
   * Add a default realm rule for the realm associated with a principal.
   * If the realm is null or is a wildcard ".*" then no rule id added.
   *
   * @param principal  principal which contains the realm
   */
  private void addDefaultRealmRule(Principal principal) {
    String realm = principal.getRealm();
    if (realm != null && ! realm.equals(".*")) {
      setRules.add(createDefaultRealmRule(realm));
    }
  }

  /**
   * Create a rule that expects 2 components in the principal and ignores hostname in the comparison.
   *
   * @param principal  principal
   * @param localUser  local user
   *
   * @return a new rule that ignores hostname in the comparison
   */
  private Rule createHostAgnosticRule(Principal principal, String localUser) {
    List<String> principalComponents = principal.getComponents();
    int componentCount = principalComponents.size();

    return new Rule(principal, componentCount, 1, String.format(
        "RULE:[%d:$1@$0](%s@%s)s/.*/%s/", componentCount,
        principal.getComponent(1), principal.getRealm(), localUser));
  }

  /**
   * Create a default rule for a realm which matches all principals with 1 component and the same realm.
   *
   * @param realm  realm that the rule is being created for
   *
   * @return  a new default realm rule
   */
  private Rule createDefaultRealmRule(String realm) {
    String caseSensitivityRule = caseInsensitiveUser ? "/L" : "";

    return new Rule(new Principal(String.format(".*@%s", realm)),
      1, 1, String.format("RULE:[1:$1@$0](.*@%s)s/@.*//" + caseSensitivityRule, realm));
  }

  /**
   * Create a rule from an existing string representation.
   * @param rule  string representation of a rule
   *
   * @return  a new rule which matches the provided string representation
   */
  private Rule createRule(String rule) {
    return new Rule(rule.startsWith("RULE:") ? rule : String.format("RULE:%s", rule));
  }

  /**
   * Creates and returns a deep copy of this AuthToLocalBuilder.
   *
   * @return a deep copy of this AuthToLocalBuilder
   */
  public AuthToLocalBuilder copy() {
    AuthToLocalBuilder copy = new AuthToLocalBuilder();

    // TODO: This needs to be done in a loop rather than use Set.addAll because there may be an issue
    // TODO: with the Rule.compareTo method?
    for(Rule rule:setRules) {
      copy.setRules.add(rule);
    }
    copy.caseInsensitiveUser = this.caseInsensitiveUser;

    return copy;
  }


  /**
   * Rule implementation.
   */
  private static class Rule implements Comparable<Rule> {
    /**
     * pattern used to parse existing rules
     */
    private static final Pattern PATTERN_RULE_PARSE =
        Pattern.compile("RULE:\\s*\\[\\s*(\\d)\\s*:\\s*(.+?)(?:@(.+?))??\\s*\\]\\s*\\((.+?)\\)\\s*(.*)");

    /**
     * associated principal
     */
    private Principal principal;

    /**
     * string representation of the rule
     */
    private String rule;

    /**
     * expected component count
     */
    private int expectedComponentCount;

    /**
     * number of components being matched in the rule
     */
    private int matchComponentCount;

    /**
     * Constructor.
     *
     * @param principal               principal
     * @param expectedComponentCount  number of components needed by a principal to match
     * @param matchComponentCount     number of components which are included in the rule evaluation
     * @param rule                    string representation of the rule
     */
    public Rule(Principal principal, int expectedComponentCount, int matchComponentCount, String rule) {
      this.principal = principal;
      this.expectedComponentCount = expectedComponentCount;
      this.matchComponentCount = matchComponentCount;
      this.rule = rule;
    }

    /**
     * Constructor.
     *
     * @param rule  string representation of the rule
     */
    public Rule(String rule) {
      //this.rule = rule;
      Matcher m = PATTERN_RULE_PARSE.matcher(rule);
      if (! m.matches()) {
        throw new IllegalArgumentException("Invalid rule: " + rule);
      }
      expectedComponentCount = Integer.valueOf(m.group(1));

      String matchPattern = m.group(2);
      matchComponentCount = (matchPattern.startsWith("$") ?
          matchPattern.substring(1) :
          matchPattern).
            split("\\$").length;
      String patternRealm = m.group(3);
      principal = new Principal(m.group(4));
      String replacementRule = m.group(5);
      if (patternRealm != null) {
        this.rule = String.format("RULE:[%d:%s@%s](%s)%s",
            expectedComponentCount, matchPattern, patternRealm,
            principal.toString(), replacementRule);
      } else {
        this.rule = String.format("RULE:[%d:%s](%s)%s",
            expectedComponentCount, matchPattern,
            principal.toString(), replacementRule);
      }
    }

    /**
     * Get the associated principal.
     *
     * @return associated principal
     */
    public Principal getPrincipal() {
      return principal;
    }

    /**
     * Get the expected component count.  This specified the number of components
     * that a principal must contain to match this rule.
     *
     * @return the expected component count
     */
    public int getExpectedComponentCount() {
      return expectedComponentCount;
    }

    /**
     * Get the match component count.  This is the number of components that are evaluated
     * when attempting to match a principal to the rule.
     *
     * @return the match component count
     */
    public int getMatchComponentCount() {
      return matchComponentCount;
    }

    /**
     * String representation of the rule in the form
     * RULE:[componentCount:matchString](me@foo.com)s/pattern/localUser/
     *
     * @return string representation of the rule
     */
    @Override
    public String toString() {
      return rule;
    }

    /**
     * Compares rules.
     * <p>
     * For rules with different expected component counts, the default string comparison is used.
     * For rules with the same expected component count rules are ordered so that rules with a higher
     * match component count occur first.
     * <p>
     * For rules with the same expected component count, default realm rules in the form of
     * .*@myRealm.com are ordered last.
     *
     * @param other  the other rule to compare
     *
     * @return a negative integer, zero, or a positive integer as this object is less than,
     *         equal to, or greater than the specified object
     */
    @Override
    public int compareTo(Rule other) {
      Principal thatPrincipal = other.getPrincipal();
      //todo: better implementation that recursively evaluates realm and all components
      if (expectedComponentCount != other.getExpectedComponentCount()) {
        return rule.compareTo(other.rule);
      } else {
        if (matchComponentCount != other.getMatchComponentCount()) {
          return other.getMatchComponentCount() - matchComponentCount;
        } else {
          if (principal.equals(thatPrincipal)) {
            return rule.compareTo(other.rule);
          } else {
            // check for wildcard realms '.*'
            String realm = principal.getRealm();
            String thatRealm = thatPrincipal.getRealm();
            if (realm == null ? thatRealm != null : ! realm.equals(thatRealm)) {
              if (realm != null && realm.equals(".*")) {
                return 1;
              } else if (thatRealm != null && thatRealm.equals(".*")) {
                return -1;
              }
            }
            // check for wildcard component 1
            String component1 = principal.getComponent(1);
            String thatComponent1 = thatPrincipal.getComponent(1);
            if (component1 != null && component1.equals(".*")) {
              return 1;
            } else if(thatComponent1 != null && thatComponent1.equals(".*")) {
              return -1;
            } else {
              return rule.compareTo(other.rule);
            }
          }
        }
      }
    }

    @Override
    public boolean equals(Object o) {
      return this == o || o instanceof Rule && rule.equals(((Rule) o).rule);
    }

    @Override
    public int hashCode() {
      return rule.hashCode();
    }
  }

  /**
   * Principal implementation.
   */
  private static class Principal {

    /**
     * principal pattern which allows for null realm
     */
    private static final Pattern p = Pattern.compile("([^@]+)(?:@(.*))?");

    /**
     * string representation
     */
    private String principal;

    /**
     * associated realm
     */
    private String realm;

    /**
     * list of components in the principal not including the realm
     */
    private List<String> components;

    /**
     * Constructor.
     *
     * @param principal  string representation of the principal
     */
    public Principal(String principal) {
      this.principal = principal;

      Matcher m = p.matcher(principal);

      if (m.matches()) {
        String allComponents = m.group(1);
        if (allComponents == null) {
          components = Collections.emptyList();
        } else {
          allComponents = allComponents.startsWith("/") ? allComponents.substring(1) : allComponents;
          components = Arrays.asList(allComponents.split("/"));
        }
        realm = m.group(2);
      } else {
        throw new IllegalArgumentException("Invalid Principal: " + principal);
      }
    }

    /**
     * Get all of the components which make up the principal.
     *
     * @return list of principal components
     */
    public List<String> getComponents() {
      return components;
    }

    /**
     * Get the component at the specified location.
     * Uses the range 1-n to match the notation used in the rule.
     *
     * @param position position of the component in the range 1-n
     *
     * @return the component at the specified location or null
     */
    public String getComponent(int position) {
      if (position > components.size()) {
        return null;
      } else {
        return components.get(position - 1);
      }
    }

    /**
     * Get the associated realm.
     *
     * @return the associated realm
     */
    public String getRealm() {
      return realm;
    }

    @Override
    public String toString() {
      return principal;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Principal principal1 = (Principal) o;

      return components.equals(principal1.components) &&
             principal.equals(principal1.principal) &&
             !(realm != null ?
                 !realm.equals(principal1.realm) :
                 principal1.realm != null);

    }

    @Override
    public int hashCode() {
      int result = principal.hashCode();
      result = 31 * result + (realm != null ? realm.hashCode() : 0);
      result = 31 * result + components.hashCode();
      return result;
    }
  }

  /**
   * ConcatenationType is an enumeration of auth-to-local rule concatenation types.
   */
  public enum ConcatenationType {
    /**
     * Each rule is appended to the set of rules on a new line (<code>\n</code>)
     */
    NEW_LINES,
    /**
     * Each rule is appended to the set of rules on a new line, escaped using a \ (<code>\\n</code>)
     */
    NEW_LINES_ESCAPED,
    /**
     * Each rule is appended to the set of rules using a space - the ruleset exists on a single line
     */
    SPACES;

    /**
     * Translate a string declaring a concatenation type to the enumerated value.
     * <p/>
     * If the string value is <code>null</code> or empty, return the default type - {@link #NEW_LINES}.
     *
     * @param value a value to translate
     * @return a ConcatenationType
     */
    public static ConcatenationType translate(String value) {
      if(value != null) {
        value = value.trim();

        if(!value.isEmpty()) {
          return valueOf(value.toUpperCase());
        }
      }

      return DEFAULT_CONCATENATION_TYPE;
    }
  }
}
