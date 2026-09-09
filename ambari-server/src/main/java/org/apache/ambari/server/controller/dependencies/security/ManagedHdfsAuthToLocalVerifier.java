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
package org.apache.ambari.server.controller.dependencies.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.ambari.server.controller.dependencies.ManagedDependencyIntegrationException;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Inspects a bounded Ambari-generated Hadoop auth-to-local policy without touching
 * Hadoop's process-global KerberosName state.
 */
public final class ManagedHdfsAuthToLocalVerifier {
  public static final int SCHEMA_VERSION = 1;
  public static final int MAX_RULE_BYTES = 64 * 1024;
  public static final int MAX_RULE_COUNT = 512;
  public static final int MAX_TEMPLATE_BYTES = 128 * 1024;

  private static final String HADOOP_MECHANISM = "hadoop";
  private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
  private static final Pattern SIMPLE_IDENTIFIER =
      Pattern.compile("[A-Za-z_][A-Za-z0-9._-]{0,127}");
  private static final Pattern REALM =
      Pattern.compile("[A-Za-z0-9](?:[A-Za-z0-9.-]{0,251}[A-Za-z0-9])?");
  private static final Pattern RULE = Pattern.compile(
      "^RULE:\\[([12]):\\$1@\\$0\\]\\(([^()]*)\\)s/(\\.\\*|@\\.\\*)/([^/]*)/(?:/L)?$");
  private static final Pattern DEFAULT_REALM_TEMPLATE =
      Pattern.compile("^\\s*default_realm\\s*=\\s*\\{\\{realm\\}\\}\\s*$");

  public Policy inspectPolicy(PolicySource source) {
    Objects.requireNonNull(source, "source");
    if (!source.ambariManagesRules()) {
      throw invalid("DEPENDENCY_HDFS_AUTH_TO_LOCAL_UNMANAGED",
          "HDFS auth-to-local rules must be managed by Ambari.");
    }
    if (!source.ambariManagesKrb5Conf()) {
      throw invalid("DEPENDENCY_KRB5_DEFAULT_REALM_UNPROVEN",
          "Ambari must manage the provider's standard krb5.conf file.");
    }
    if (!"/etc".equals(normalize(source.krb5ConfDirectory()))) {
      throw invalid("DEPENDENCY_KRB5_DEFAULT_REALM_UNPROVEN",
          "The supported provider krb5.conf directory is /etc.");
    }

    String mechanism = normalize(source.configuredMechanism()).toLowerCase(Locale.ROOT);
    if (mechanism.isEmpty()) {
      mechanism = HADOOP_MECHANISM;
    }
    if (!HADOOP_MECHANISM.equals(mechanism)) {
      throw invalid("DEPENDENCY_HDFS_AUTH_TO_LOCAL_UNSUPPORTED",
          "The provider auth-to-local mechanism is unsupported.");
    }

    String kerberosRealm = requireRealm(source.kerberosEnvRealm(),
        "kerberos-env/realm");
    String effectiveRealm = source.krb5ConfRealm() == null
        ? kerberosRealm : requireRealm(source.krb5ConfRealm(), "krb5-conf/realm");
    if (!kerberosRealm.equals(effectiveRealm)) {
      throw invalid("DEPENDENCY_KRB5_DEFAULT_REALM_UNPROVEN",
          "The managed krb5.conf realm does not match the provider identity realm.");
    }

    String expectedTemplateFingerprint = requireHash(
        source.expectedStockKrb5ConfTemplateFingerprint(),
        "expectedStockKrb5ConfTemplateFingerprint");
    String template = requireBoundedText(source.krb5ConfTemplate(), MAX_TEMPLATE_BYTES,
        "DEPENDENCY_KRB5_DEFAULT_REALM_UNPROVEN",
        "The managed krb5.conf template is unavailable or unsupported.");
    validateStockRealmTemplate(template);
    String templateFingerprint = fingerprintTemplate(template);
    if (!expectedTemplateFingerprint.equals(templateFingerprint)) {
      throw invalid("DEPENDENCY_KRB5_DEFAULT_REALM_UNPROVEN",
          "The managed krb5.conf template differs from the active stack template.");
    }

    String rules = requireBoundedText(source.effectiveRules(), MAX_RULE_BYTES,
        "DEPENDENCY_HDFS_AUTH_TO_LOCAL_UNSUPPORTED",
        "The provider auth-to-local policy is unavailable or exceeds the supported size.");
    List<ParsedRule> parsedRules = parseRules(rules);
    String rulesFingerprint = hashBytes("rules", rules.getBytes(StandardCharsets.UTF_8));
    String realmSourceFingerprint = hash("realm-source",
        Boolean.toString(source.ambariManagesKrb5Conf()), "/etc", templateFingerprint,
        effectiveRealm);
    String policyFingerprint = hash("policy", Integer.toString(SCHEMA_VERSION), mechanism,
        effectiveRealm, Boolean.toString(source.ambariManagesRules()),
        Boolean.toString(source.ambariManagesKrb5Conf()), rulesFingerprint,
        realmSourceFingerprint, Integer.toString(parsedRules.size()));
    return new Policy(SCHEMA_VERSION, mechanism, effectiveRealm,
        source.ambariManagesRules(), source.ambariManagesKrb5Conf(), rulesFingerprint,
        realmSourceFingerprint, policyFingerprint);
  }

  public ConsumerPatternProof provePattern(PolicySource source, Policy expectedPolicy,
      ConsumerPatternInput consumer) {
    Objects.requireNonNull(expectedPolicy, "expectedPolicy");
    Objects.requireNonNull(consumer, "consumer");
    Policy currentPolicy = inspectPolicy(source);
    if (!currentPolicy.equals(expectedPolicy)) {
      throw invalid("DEPENDENCY_HDFS_AUTH_TO_LOCAL_STALE",
          "The provider auth-to-local policy changed after it was inspected.");
    }

    String consumerRealm = requireRealm(consumer.consumerRealm(), "consumerRealm");
    String expectedUser = requireIdentifier(consumer.expectedShortUser(),
        "expectedShortUser");
    if (!currentPolicy.effectiveDefaultRealm().equals(consumerRealm)) {
      throw invalid("CROSS_REALM_NOT_SUPPORTED",
          "The consumer and HDFS provider must use the same Kerberos realm.");
    }
    String expectedPattern = expectedUser + "/_HOST@" + consumerRealm;
    if (!expectedPattern.equals(consumer.principalPattern())) {
      throw invalid("DEPENDENCY_HDFS_AUTH_TO_LOCAL_MISMATCH",
          "The consumer principal pattern does not match its approved unique identity.");
    }

    List<ParsedRule> rules = parseRules(source.effectiveRules());
    String mappedUser = evaluate(rules, 2, expectedUser, consumerRealm,
        currentPolicy.effectiveDefaultRealm(), true);
    if (!expectedUser.equals(mappedUser)) {
      throw invalid("DEPENDENCY_HDFS_AUTH_TO_LOCAL_MISMATCH",
          "The consumer principal pattern does not map to its approved unique identity.");
    }

    String patternFingerprint = fingerprintConsumerPattern(consumer);
    String proofFingerprint = hash("pattern-proof", Integer.toString(SCHEMA_VERSION),
        currentPolicy.policyFingerprint(), patternFingerprint, mappedUser);
    return new ConsumerPatternProof(SCHEMA_VERSION, currentPolicy.policyFingerprint(),
        patternFingerprint, mappedUser, proofFingerprint);
  }

  /** Returns the canonical hash for one exact, symbolic consumer role principal. */
  public String fingerprintConsumerPattern(ConsumerPatternInput consumer) {
    Objects.requireNonNull(consumer, "consumer");
    String realm = requireRealm(consumer.consumerRealm(), "consumerRealm");
    String user = requireIdentifier(consumer.expectedShortUser(), "expectedShortUser");
    String expectedPattern = user + "/_HOST@" + realm;
    if (!expectedPattern.equals(consumer.principalPattern())) {
      throw invalid("DEPENDENCY_HDFS_AUTH_TO_LOCAL_MISMATCH",
          "The consumer principal pattern does not match its approved unique identity.");
    }
    return hash("consumer-pattern", Integer.toString(SCHEMA_VERSION),
        realm, user, expectedPattern);
  }

  public ConsumerLocalMappingProof proveConsumerLocalMappings(String effectiveRules,
      ConsumerLocalMappingInput consumer) {
    Objects.requireNonNull(consumer, "consumer");
    String realm = requireRealm(consumer.realm(), "consumerRealm");
    String effectiveUser = requireIdentifier(consumer.effectiveShortUser(),
        "effectiveShortUser");
    String smokeUser = requireIdentifier(consumer.smokeShortUser(), "smokeShortUser");
    String rolePattern = effectiveUser + "/_HOST@" + realm;
    String headlessPrincipal = effectiveUser + "@" + realm;
    if (!rolePattern.equals(consumer.rolePrincipalPattern())
        || !headlessPrincipal.equals(consumer.headlessPrincipal())) {
      throw invalid("DEPENDENCY_HDFS_AUTH_TO_LOCAL_MISMATCH",
          "The consumer HBase principals do not match its approved unique identity.");
    }
    Principal smokePrincipal = parseOneComponentPrincipal(consumer.smokePrincipal(), realm,
        "smokePrincipal");
    if (effectiveUser.equals(smokeUser)) {
      throw invalid("DEPENDENCY_HDFS_AUTH_TO_LOCAL_MISMATCH",
          "The HBase service and smoke identities must remain distinct.");
    }

    List<ParsedRule> rules = parseRules(effectiveRules);
    requireMappedUser(rules, 2, effectiveUser, realm, realm, effectiveUser,
        "the HBase daemon principal");
    requireMappedUser(rules, 1, effectiveUser, realm, realm, effectiveUser,
        "the HBase headless principal");
    requireMappedUser(rules, 1, smokePrincipal.primary(), realm, realm, smokeUser,
        "the HBase smoke principal");

    String rulesFingerprint = hashBytes("consumer-local-rules",
        effectiveRules.getBytes(StandardCharsets.UTF_8));
    String identityFingerprint = hash("consumer-local-identities",
        Integer.toString(SCHEMA_VERSION), realm, effectiveUser, rolePattern,
        headlessPrincipal, smokePrincipal.primary(), smokeUser);
    String proofFingerprint = hash("consumer-local-proof", Integer.toString(SCHEMA_VERSION),
        rulesFingerprint, identityFingerprint);
    return new ConsumerLocalMappingProof(SCHEMA_VERSION, rulesFingerprint,
        identityFingerprint, effectiveUser, smokeUser, proofFingerprint);
  }

  public String fingerprintTemplate(String template) {
    String bounded = requireBoundedText(template, MAX_TEMPLATE_BYTES,
        "DEPENDENCY_KRB5_DEFAULT_REALM_UNPROVEN",
        "The managed krb5.conf template is unavailable or unsupported.");
    return hashBytes("krb5-template", normalizeLineEndings(bounded)
        .getBytes(StandardCharsets.UTF_8));
  }

  /** Returns the exact canonical fingerprint used by a consumer-local mapping proof. */
  public String fingerprintConsumerLocalRules(String effectiveRules) {
    String bounded = requireBoundedText(effectiveRules, MAX_RULE_BYTES,
        "DEPENDENCY_HDFS_AUTH_TO_LOCAL_UNSUPPORTED",
        "The consumer auth-to-local policy is unavailable or exceeds the supported size.");
    return hashBytes("consumer-local-rules", bounded.getBytes(StandardCharsets.UTF_8));
  }

  private List<ParsedRule> parseRules(String value) {
    String rules = requireBoundedText(value, MAX_RULE_BYTES,
        "DEPENDENCY_HDFS_AUTH_TO_LOCAL_UNSUPPORTED",
        "The provider auth-to-local policy is unavailable or exceeds the supported size.");
    String normalized = normalizeLineEndings(rules);
    if (containsUnsupportedControl(normalized)) {
      throw unsupportedRule(-1, "control characters");
    }
    String[] lines = normalized.split("\\n", -1);
    if (lines.length == 0 || lines.length > MAX_RULE_COUNT) {
      throw unsupportedRule(-1, "rule count");
    }

    List<ParsedRule> parsed = new ArrayList<>(lines.length);
    boolean foundDefault = false;
    for (int index = 0; index < lines.length; index++) {
      String line = lines[index];
      if (line.isEmpty()) {
        throw unsupportedRule(index, "blank rule");
      }
      if ("DEFAULT".equals(line)) {
        if (foundDefault || index != lines.length - 1) {
          throw unsupportedRule(index, "DEFAULT placement");
        }
        foundDefault = true;
        parsed.add(ParsedRule.terminalDefault());
        continue;
      }
      if (foundDefault) {
        throw unsupportedRule(index, "rule after DEFAULT");
      }
      parsed.add(parseRule(line, index));
    }
    if (!foundDefault) {
      throw unsupportedRule(-1, "missing DEFAULT");
    }
    return List.copyOf(parsed);
  }

  private ParsedRule parseRule(String line, int index) {
    Matcher matcher = RULE.matcher(line);
    if (!matcher.matches()) {
      throw unsupportedRule(index, "syntax");
    }
    int componentCount = Integer.parseInt(matcher.group(1));
    String matchExpression = matcher.group(2);
    String substitutionExpression = matcher.group(3);
    String replacement = matcher.group(4);
    boolean lowerCase = line.endsWith("/L");

    int realmSeparator = matchExpression.indexOf('@');
    if (realmSeparator <= 0 || realmSeparator != matchExpression.lastIndexOf('@')) {
      throw unsupportedRule(index, "match expression");
    }
    String primaryExpression = matchExpression.substring(0, realmSeparator);
    String realmExpression = matchExpression.substring(realmSeparator + 1);
    requireRuleRealm(realmExpression, index);

    boolean defaultRealmRule = ".*".equals(primaryExpression);
    if (defaultRealmRule) {
      if (componentCount != 1 || !"@.*".equals(substitutionExpression)
          || !replacement.isEmpty()) {
        throw unsupportedRule(index, "generated default-realm shape");
      }
    } else {
      if (!SIMPLE_IDENTIFIER.matcher(primaryExpression).matches()
          || !".*".equals(substitutionExpression)
          || !SIMPLE_IDENTIFIER.matcher(replacement).matches()) {
        throw unsupportedRule(index, "generated identity shape");
      }
    }

    try {
      Pattern match = Pattern.compile(matchExpression);
      Pattern substitution = Pattern.compile(substitutionExpression);
      return new ParsedRule(false, componentCount, match, substitution, replacement,
          lowerCase);
    } catch (PatternSyntaxException e) {
      throw unsupportedRule(index, "regular expression");
    }
  }

  private String evaluate(List<ParsedRule> rules, int componentCount, String primary,
      String realm, String effectiveDefaultRealm, boolean allowTerminalDefault) {
    String base = primary + "@" + realm;
    for (ParsedRule rule : rules) {
      if (rule.defaultRule()) {
        if (allowTerminalDefault && effectiveDefaultRealm.equals(realm)) {
          return primary;
        }
        continue;
      }
      if (rule.componentCount() != componentCount
          || !rule.match().matcher(base).matches()) {
        continue;
      }
      String result = rule.substitution().matcher(base)
          .replaceFirst(rule.replacement());
      if (rule.lowerCase()) {
        result = result.toLowerCase(Locale.ENGLISH);
      }
      if (!SIMPLE_IDENTIFIER.matcher(result).matches()) {
        throw invalid("DEPENDENCY_HDFS_AUTH_TO_LOCAL_MISMATCH",
            "The provider auth-to-local policy produced a non-simple identity.");
      }
      return result;
    }
    throw invalid("DEPENDENCY_HDFS_AUTH_TO_LOCAL_MISMATCH",
        "No provider auth-to-local rule maps the consumer principal pattern.");
  }

  private void requireMappedUser(List<ParsedRule> rules, int componentCount, String primary,
      String realm, String effectiveDefaultRealm, String expectedUser, String description) {
    String mappedUser = evaluate(rules, componentCount, primary, realm,
        effectiveDefaultRealm, false);
    if (!expectedUser.equals(mappedUser)) {
      throw invalid("DEPENDENCY_HDFS_AUTH_TO_LOCAL_MISMATCH",
          "The consumer auth-to-local policy does not map " + description
              + " to its approved local identity.");
    }
  }

  private Principal parseOneComponentPrincipal(String value, String expectedRealm,
      String field) {
    String normalized = normalize(value);
    int separator = normalized.indexOf('@');
    if (separator <= 0 || separator != normalized.lastIndexOf('@')
        || normalized.substring(0, separator).contains("/")
        || !SIMPLE_IDENTIFIER.matcher(normalized.substring(0, separator)).matches()
        || !expectedRealm.equals(normalized.substring(separator + 1))) {
      throw invalid("DEPENDENCY_HDFS_AUTH_TO_LOCAL_MISMATCH",
          field + " is not a supported same-realm one-component principal.");
    }
    return new Principal(normalized.substring(0, separator), expectedRealm);
  }

  private void validateStockRealmTemplate(String template) {
    String[] lines = normalizeLineEndings(template).split("\\n", -1);
    boolean inLibDefaults = false;
    int supportedAssignments = 0;
    for (String line : lines) {
      String trimmed = line.trim();
      if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
        inLibDefaults = "[libdefaults]".equals(trimmed);
      }
      if (trimmed.startsWith("#") || !trimmed.contains("default_realm")) {
        continue;
      }
      if (!inLibDefaults || !DEFAULT_REALM_TEMPLATE.matcher(line).matches()) {
        throw invalid("DEPENDENCY_KRB5_DEFAULT_REALM_UNPROVEN",
            "The managed krb5.conf template has an unsupported default realm expression.");
      }
      supportedAssignments++;
    }
    if (supportedAssignments != 1) {
      throw invalid("DEPENDENCY_KRB5_DEFAULT_REALM_UNPROVEN",
          "The managed krb5.conf template must have one supported default realm expression.");
    }
  }

  private void requireRuleRealm(String value, int index) {
    if (!REALM.matcher(value).matches()) {
      throw unsupportedRule(index, "realm expression");
    }
  }

  private static String requireRealm(String value, String field) {
    String normalized = normalize(value);
    if (!REALM.matcher(normalized).matches()) {
      throw invalid("DEPENDENCY_KRB5_DEFAULT_REALM_UNPROVEN",
          field + " is missing or unsupported.");
    }
    return normalized;
  }

  private static String requireIdentifier(String value, String field) {
    String normalized = normalize(value);
    if (!SIMPLE_IDENTIFIER.matcher(normalized).matches()) {
      throw invalid("DEPENDENCY_HDFS_AUTH_TO_LOCAL_MISMATCH",
          field + " is missing or unsupported.");
    }
    return normalized;
  }

  private static String requireBoundedText(String value, int maximumBytes, String code,
      String message) {
    if (value == null || value.isEmpty()
        || value.getBytes(StandardCharsets.UTF_8).length > maximumBytes) {
      throw invalid(code, message);
    }
    return value;
  }

  private static boolean containsUnsupportedControl(String value) {
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      if (character < 0x20 && character != '\n') {
        return true;
      }
      if (character == 0x7f) {
        return true;
      }
    }
    return false;
  }

  private static String normalizeLineEndings(String value) {
    return value.replace("\r\n", "\n").replace('\r', '\n');
  }

  private static String normalize(String value) {
    return value == null ? "" : value.trim();
  }

  private static String requireHash(String value, String field) {
    if (value == null || !HASH.matcher(value).matches()) {
      throw invalid("DEPENDENCY_KRB5_DEFAULT_REALM_UNPROVEN",
          field + " must be a SHA-256 fingerprint.");
    }
    return value;
  }

  private static String hashBytes(String purpose, byte[] value) {
    return hash(purpose, HexFormat.of().formatHex(value));
  }

  private static String hash(String... values) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      StringBuilder canonical = new StringBuilder();
      append(canonical, "managed-hdfs-auth-to-local", Integer.toString(SCHEMA_VERSION));
      append(canonical, values);
      return "sha256:" + HexFormat.of().formatHex(
          digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is required by the Java runtime", e);
    }
  }

  private static void append(StringBuilder target, String... values) {
    for (String value : values) {
      String actual = value == null ? "" : value;
      target.append(actual.length()).append(':').append(actual);
    }
  }

  private static ManagedDependencyIntegrationException unsupportedRule(int index,
      String category) {
    String location = index < 0 ? "" : " at index " + index;
    return invalid("DEPENDENCY_HDFS_AUTH_TO_LOCAL_UNSUPPORTED",
        "The provider auth-to-local policy has unsupported " + category + location + ".");
  }

  private static ManagedDependencyIntegrationException invalid(String code, String message) {
    return new ManagedDependencyIntegrationException(422, code, message);
  }

  private record ParsedRule(boolean defaultRule, int componentCount, Pattern match,
      Pattern substitution, String replacement, boolean lowerCase) {
    private static ParsedRule terminalDefault() {
      return new ParsedRule(true, 0, null, null, "", false);
    }
  }

  public record PolicySource(
      @JsonIgnore String effectiveRules,
      String configuredMechanism,
      boolean ambariManagesRules,
      String kerberosEnvRealm,
      boolean ambariManagesKrb5Conf,
      String krb5ConfRealm,
      String krb5ConfDirectory,
      @JsonIgnore String krb5ConfTemplate,
      String expectedStockKrb5ConfTemplateFingerprint) {

    @Override
    public String toString() {
      return "PolicySource[rawPolicy=redacted, ambariManagesRules=" + ambariManagesRules
          + ", ambariManagesKrb5Conf=" + ambariManagesKrb5Conf + "]";
    }
  }

  public record Policy(
      int schemaVersion,
      String mechanism,
      String effectiveDefaultRealm,
      boolean ambariManagesRules,
      boolean ambariManagesKrb5Conf,
      String rulesFingerprint,
      String realmSourceFingerprint,
      String policyFingerprint) {
    public Policy {
      if (schemaVersion != SCHEMA_VERSION || !HADOOP_MECHANISM.equals(mechanism)) {
        throw new IllegalArgumentException("unsupported auth-to-local policy descriptor");
      }
      if (!ambariManagesRules || !ambariManagesKrb5Conf) {
        throw new IllegalArgumentException("auth-to-local policy must be Ambari-managed");
      }
      effectiveDefaultRealm = requireRecordRealm(effectiveDefaultRealm);
      rulesFingerprint = requireRecordHash(rulesFingerprint, "rulesFingerprint");
      realmSourceFingerprint = requireRecordHash(realmSourceFingerprint,
          "realmSourceFingerprint");
      policyFingerprint = requireRecordHash(policyFingerprint, "policyFingerprint");
    }
  }

  public record ConsumerPatternInput(
      String consumerRealm,
      String expectedShortUser,
      String principalPattern) {
  }

  public record ConsumerPatternProof(
      int schemaVersion,
      String providerPolicyFingerprint,
      String principalPatternFingerprint,
      String mappedShortUser,
      String proofFingerprint) {
    public ConsumerPatternProof {
      if (schemaVersion != SCHEMA_VERSION) {
        throw new IllegalArgumentException("unsupported auth-to-local proof descriptor");
      }
      providerPolicyFingerprint = requireRecordHash(providerPolicyFingerprint,
          "providerPolicyFingerprint");
      principalPatternFingerprint = requireRecordHash(principalPatternFingerprint,
          "principalPatternFingerprint");
      mappedShortUser = requireRecordIdentifier(mappedShortUser, "mappedShortUser");
      proofFingerprint = requireRecordHash(proofFingerprint, "proofFingerprint");
    }
  }

  public record ConsumerLocalMappingInput(
      String realm,
      String effectiveShortUser,
      String rolePrincipalPattern,
      String headlessPrincipal,
      String smokePrincipal,
      String smokeShortUser) {
  }

  public record ConsumerLocalMappingProof(
      int schemaVersion,
      String rulesFingerprint,
      String identityFingerprint,
      String effectiveShortUser,
      String smokeShortUser,
      String proofFingerprint) {
    public ConsumerLocalMappingProof {
      if (schemaVersion != SCHEMA_VERSION) {
        throw new IllegalArgumentException("unsupported consumer-local mapping proof");
      }
      rulesFingerprint = requireRecordHash(rulesFingerprint, "rulesFingerprint");
      identityFingerprint = requireRecordHash(identityFingerprint, "identityFingerprint");
      effectiveShortUser = requireRecordIdentifier(effectiveShortUser,
          "effectiveShortUser");
      smokeShortUser = requireRecordIdentifier(smokeShortUser, "smokeShortUser");
      proofFingerprint = requireRecordHash(proofFingerprint, "proofFingerprint");
    }
  }

  private record Principal(String primary, String realm) {
  }

  private static String requireRecordHash(String value, String field) {
    if (value == null || !HASH.matcher(value).matches()) {
      throw new IllegalArgumentException(field + " must be a SHA-256 fingerprint");
    }
    return value;
  }

  private static String requireRecordRealm(String value) {
    if (value == null || !REALM.matcher(value).matches()) {
      throw new IllegalArgumentException("effectiveDefaultRealm is unsupported");
    }
    return value;
  }

  private static String requireRecordIdentifier(String value, String field) {
    if (value == null || !SIMPLE_IDENTIFIER.matcher(value).matches()) {
      throw new IllegalArgumentException(field + " is unsupported");
    }
    return value;
  }
}
