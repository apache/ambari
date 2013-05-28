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
package org.apache.ambari.server.controller.utilities;

import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.predicate.GreaterEqualsPredicate;
import org.apache.ambari.server.controller.predicate.GreaterPredicate;
import org.apache.ambari.server.controller.predicate.LessEqualsPredicate;
import org.apache.ambari.server.controller.predicate.LessPredicate;
import org.apache.ambari.server.controller.predicate.NotPredicate;
import org.apache.ambari.server.controller.predicate.OrPredicate;
import org.apache.ambari.server.controller.spi.Predicate;

import java.util.LinkedList;
import java.util.List;

/**
 * Builder for predicates.
 * <p/>
 * The builder enforces a domain specific language according to the following
 * grammar :
 * <p/>
 * <predicate> ::= <property_name> <relational_operator> <value>
 * <predicate> ::= NOT <predicate>
 * <predicate> ::= ( <predicate> )
 * <predicate> ::= <predicate> AND <predicate>
 * <predicate> ::= <predicate> OR <predicate>
 *
 * <relational_operator> ::= =|>|<|>=|<=
 * <p/>
 * The predicate builder uses the normal method chaining of the builder pattern
 * along with intermediate objects.  The use of intermediate objects allows
 * for compiler checked constraints.
 * <p/>
 * For example, the predicate builder can be used to build a predicate where
 * property1=="foo" && property2=="bar".
 *
 * <pre>
 * {@code
 * PredicateBuilder builder = new PredicateBuilder();
 *
 * Predicate predicate = builder.property(property1).equals("foo").
 *     and().property(property2).equals("bar").toPredicate();
 * }
 * </pre>
 *
 * In this example, we are starting with an instance of {@link PredicateBuilder}.
 * Calling the method {@link PredicateBuilder#property(String)} returns an
 * instance of {@link PredicateBuilderProperty} which exposes methods for attaching
 * a relational operator to the property to form a simple predicate.
 * <p/>
 * Notice that the method {@link PredicateBuilderProperty#equals(Comparable)}
 * returns an instance of {@link PredicateBuilderPredicate} which exposes methods
 * for using predicates with logical operators to create complex predicates.
 * <p/>
 * Calling the method {@link PredicateBuilderPredicate#and()} returns an instance
 * of {@link PredicateBuilder} which allows us to start over building the predicate
 * for property2.
 * <p/>
 * The reason for having these intermediate return objects is that they only
 * expose the methods that make sense for that point in the building process.
 * In other words, we can use the compiler to check the syntax of our DSL
 * grammar at compile time rather than having a single builder class with a
 * bunch of runtime checks.
 * <p/>
 * For example, if the user tries to make an inappropriate call to the and()
 * method ...
 *
 * <pre>
 * {@code
 *
 * Predicate predicate = builder.property(property1).and().
 *     property(property2).equals("bar").toPredicate();
 * }
 * </pre>
 *
 * ... the compiler will flag it as an error and the code will simply not compile.
 */
public class PredicateBuilder {

  private String propertyId;
  private List<Predicate> predicates = new LinkedList<Predicate>();
  private Operator operator = null;
  private final PredicateBuilder outer;
  private boolean done = false;
  private boolean not = false;


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a predicate builder.
   */
  public PredicateBuilder() {
    this.outer = null;
  }

  /**
   * Construct a predicate builder within another predicate builder.
   *
   * @param outer  the outer predicate builder
   */
  private PredicateBuilder(PredicateBuilder outer) {
    this.outer = outer;
  }


  // ----- enums ------------------------------------------------------

  /**
   * Logical operators
   */
  private enum Operator {
    And,
    Or
  }


  // ----- PredicateBuilder --------------------------------------------------

  /**
   * Create a property from the given property id.  This supports method
   * chaining by returning an instance of {@link PredicateBuilderProperty}
   * which is an intermediate object that represents the property in the DSL.
   *
   * @param id  the property id
   *
   * @return a property that can be used in the building of the predicate.
   *
   * @throws IllegalStateException if an attempt is made to reuse a predicate builder
   */
  public PredicateBuilderProperty property(String id) {
    checkDone();
    propertyId = id;
    return new PredicateBuilderProperty();
  }

  /**
   * Set the builder to negate the predicate being built.  This supports method
   * chaining by returning an instance of {@link PredicateBuilder} which can be
   * used to continue building the predicate.
   *
   * For example, the following shows a usage of the not() method to
   * produce a predicate where property "p1" does not equal "foo".
   *
   * <pre>
   * {@code
   * Predicate predicate = builder.not().property("p1").equals("foo").toPredicate();
   * }
   * </pre>
   *
   * @return a builder to be used to continue building the predicate
   */
  public PredicateBuilder not() {
    not = true;
    return this;
  }

  /**
   * Set the builder to begin a block around the predicate being built.  Calling this
   * method is the equivalent of using a left parenthesis.  This supports method
   * chaining by returning an instance of {@link PredicateBuilder} which can be
   * used to continue building the predicate.
   *
   * For example, the following shows a usage of the begin() method to
   * produce a predicate where p1==foo && (p2==bar || p3 == cat).
   *
   * <pre>
   * {@code
   * Predicate predicate = builder.property("p1").equals("foo").and().
   *     begin().property("p2").equals("bar").or().property("p3").equals("cat").end().
   *     toPredicate();
   * }
   * </pre>
   *
   * @return a builder to be used to continue building the predicate
   *
   * @throws IllegalStateException if an attempt is made to reuse a predicate builder
   */
  public PredicateBuilder begin() {
    checkDone();
    return new PredicateBuilder(this);
  }

  /**
   * Produce a {@link Predicate} object from the builder.
   *
   * @return the predicate object
   */
  public Predicate toPredicate() {
    return getPredicate();
  }

  // ----- helper methods ----------------------------------------------------

  private void checkDone() {
    if (done) {
      throw new IllegalStateException("Can't reuse a predicate builder.");
    }
  }

  private PredicateBuilderPredicate getPredicateBuilderWithPredicate() {
    return new PredicateBuilderPredicate();
  }

  private void addPredicate(Predicate predicate) {
    predicates.add(predicate);
  }

  private void handleComparator() {
    if (operator == null) {
      return;
    }

    if (predicates.size() == 0) {
      throw new IllegalStateException("No left operand.");
    }
    Predicate predicate;

    switch (operator) {
      case And:
        predicate = new AndPredicate(predicates.toArray(new Predicate[predicates.size()]));
        break;
      case Or:
        predicate = new OrPredicate(predicates.toArray(new Predicate[predicates.size()]));
        break;
      default:
        throw new IllegalStateException("Unknown operator " + this.operator);
    }
    predicates.clear();
    addPredicate(predicate);
  }

  private Predicate getPredicate() {
    handleComparator();

    if (predicates.size() == 1) {
      Predicate predicate = predicates.get(0);
      if (not) {
        predicate = new NotPredicate(predicate);
        not = false;
      }
      return predicate;
    }
    throw new IllegalStateException("Can't return a predicate.");
  }

  // ----- inner classes -----------------------------------------------------

  // ----- PredicateBuilderProperty ------------------------------------------

  /**
   * A builder object that represents the property portion of the predicate being built.
   * The PredicateBuilderProperty is itself a builder object that may be returned for
   * method chaining of the predicate builder methods.
   */
  public class PredicateBuilderProperty {

    /**
     * Create a {@link PredicateBuilderPredicate} representing an equals 
     * predicate for the property represented by this builder for the given
     * value.  This supports method chaining by returning an instance of 
     * {@link PredicateBuilderPredicate} which can be used to continue building 
     * the predicate.
     *
     * For example, the following shows a usage of the equals() method to
     * produce a predicate where p1==foo.
     *
     * <pre>
     * {@code
     * Predicate predicate = builder.property("p1").equals("foo").
     *     toPredicate();
     * }
     * </pre>
     * 
     * @param value  the right operand (value) of the = operator
     * @param <T>    the type of the property
     *           
     * @return a new builder representing an equals predicate
     *
     * @throws IllegalStateException if no property name was specified on this builder
     */
    public <T>PredicateBuilderPredicate equals(Comparable<T> value) {
      if (propertyId == null) {
        throw new IllegalStateException("No property.");
      }
      addPredicate(new EqualsPredicate<T>(propertyId, value));

      return new PredicateBuilderPredicate();
    }

    /**
     * Create a {@link PredicateBuilderPredicate} representing an greater than 
     * predicate for the property represented by this builder for the given
     * value.  This supports method chaining by returning an instance of 
     * {@link PredicateBuilderPredicate} which can be used to continue building 
     * the predicate.
     *
     * For example, the following shows a usage of the greaterThan() method to
     * produce a predicate where p1 > 5.
     *
     * <pre>
     * {@code
     * Predicate predicate = builder.property("p1").greaterThan(5).
     *     toPredicate();
     * }
     * </pre>
     *
     * @param value  the right operand (value) of the > operator
     * @param <T>    the type of the property
     *
     * @return a new builder representing a greater than predicate
     *
     * @throws IllegalStateException if no property name was specified on this builder
     */
    public <T>PredicateBuilderPredicate greaterThan(Comparable<T> value) {
      if (propertyId == null) {
        throw new IllegalStateException("No property.");
      }
      addPredicate(new GreaterPredicate<T>(propertyId, value));

      return new PredicateBuilderPredicate();
    }

    /**
     * Create a {@link PredicateBuilderPredicate} representing a 
     * greater than or equals predicate for the property represented by this 
     * builder for the given value.  This supports method chaining by returning 
     * an instance of {@link PredicateBuilderPredicate} which can be used to 
     * continue building the predicate.
     *
     * For example, the following shows a usage of the greaterThanEqualTo()
     * method to produce a predicate where p1 >= 5.
     *
     * <pre>
     * {@code
     * Predicate predicate = builder.property("p1").greaterThanEqualTo(5).
     *     toPredicate();
     * }
     * </pre>
     *
     * @param value  the right operand (value) of the >= operator
     * @param <T>    the type of the property
     *
     * @return a new builder representing a greater than or equals predicate
     *
     * @throws IllegalStateException if no property name was specified on this builder
     */
    public <T>PredicateBuilderPredicate greaterThanEqualTo(Comparable<T> value) {
      if (propertyId == null) {
        throw new IllegalStateException("No property.");
      }
      addPredicate(new GreaterEqualsPredicate<T>(propertyId, value));

      return new PredicateBuilderPredicate();
    }

    /**
     * Create a {@link PredicateBuilderPredicate} representing a 
     * less than predicate for the property represented by this builder 
     * for the given value.  This supports method chaining by returning 
     * an instance of {@link PredicateBuilderPredicate} which can be used to 
     * continue building the predicate.
     *
     * For example, the following shows a usage of the lessThan()
     * method to produce a predicate where p1 < 5.
     *
     * <pre>
     * {@code
     * Predicate predicate = builder.property("p1").lessThan(5).
     *     toPredicate();
     * }
     * </pre>
     *
     * @param value  the right operand (value) of the < operator
     * @param <T>    the type of the property
     *
     * @return a new builder representing a less than predicate
     *
     * @throws IllegalStateException if no property name was specified on this builder
     */
    public <T>PredicateBuilderPredicate lessThan(Comparable<T> value) {
      if (propertyId == null) {
        throw new IllegalStateException("No property.");
      }
      addPredicate(new LessPredicate<T>(propertyId, value));

      return new PredicateBuilderPredicate();
    }

    /**
     * Create a {@link PredicateBuilderPredicate} representing a 
     * less than or equals predicate for the property represented by this 
     * builder for the given value.  This supports method chaining by returning 
     * an instance of {@link PredicateBuilderPredicate} which can be used to 
     * continue building the predicate.
     *
     * For example, the following shows a usage of the lessThanEqualTo()
     * method to produce a predicate where p1 <= 5.
     *
     * <pre>
     * {@code
     * Predicate predicate = builder.property("p1").lessThanEqualTo(5).
     *     toPredicate();
     * }
     * </pre>
     *
     * @param value  the right operand (value) of the <= operator
     * @param <T>    the type of the property
     *
     * @return a new builder representing a less than or equals predicate
     *
     * @throws IllegalStateException if no property name was specified on this builder
     */
    public <T>PredicateBuilderPredicate lessThanEqualTo(Comparable<T> value) {
      if (propertyId == null) {
        throw new IllegalStateException("No property.");
      }
      addPredicate(new LessEqualsPredicate<T>(propertyId, value));

      return new PredicateBuilderPredicate();
    }
  }

  // ----- PredicateBuilderPredicate -----------------------------------------
  
  /**
   * A builder object that represents an inner predicate portion of the predicate being built.
   * Note that the predicate represented by an instance of PredicateBuilderPredicate may be
   * part of a larger complex predicate being built by the predicate builder.  The
   * PredicateBuilderPredicate is itself a builder object that may be returned for method
   * chaining of the predicate builder methods.
   */
  public class PredicateBuilderPredicate {

    /**
     * Get a {@link PredicateBuilder} object that can be used to build a 
     * predicate that will be ANDed with the predicate represented by this 
     * PredicateBuilderPredicate.
     *
     * For example, the following shows a usage of the and() method to
     * produce a predicate where p1==foo && p2==bar.
     *
     * <pre>
     * {@code
     * Predicate predicate = builder.property(p1).equals("foo").
     *     and().property(p2).equals("bar").toPredicate();
     * }
     * </pre>
     * 
     * @return a new predicate builder that should be used to build the predicate
     *         being ANDed with the predicate from this builder
     */
    public PredicateBuilder and() {

      if (operator != Operator.And) {
        handleComparator();
        operator = Operator.And;
      }
      return PredicateBuilder.this;
    }

    /**
     * Get a {@link PredicateBuilder} object that can be used to build a 
     * predicate that will be ORed with the predicate represented by this 
     * PredicateBuilderPredicate.
     *
     * For example, the following shows a usage of the and() method to
     * produce a predicate where p1==foo || p2==bar.
     *
     * <pre>
     * {@code
     * Predicate predicate = builder.property(p1).equals("foo").
     *     or().property(p2).equals("bar").toPredicate();
     * }
     * </pre>
     *
     * @return a new predicate builder that should be used to build the predicate
     *         being ORed with the predicate from this builder
     */
    public PredicateBuilder or() {

      if (operator != Operator.Or) {
        handleComparator();
        operator = Operator.Or;
      }
      return PredicateBuilder.this;
    }

    /**
     * Produce a {@link Predicate} object from the builder.
     *
     * @return the predicate object
     *
     * @throws IllegalStateException if the block is unbalanced (missing end call)
     */
    public Predicate toPredicate() {
      if (outer != null) {
        throw new IllegalStateException("Unbalanced block - missing end.");
      }
      done = true;
      return getPredicate();
    }

    /**
     * Set the builder to end a block around the predicate being built.  Calling this
     * method is the equivalent of using a right parenthesis.  This supports method
     * chaining by returning an instance of {@link PredicateBuilderPredicate} which can 
     * be used to continue building the predicate.
     *
     * For example, the following shows a usage of the end() method to
     * produce a predicate where p1==foo && (p2==bar || p3 == cat).
     *
     * <pre>
     * {@code
     * Predicate predicate = builder.property("p1").equals("foo").and().
     *     begin().property("p2").equals("bar").or().property("p3").equals("cat").end().
     *     toPredicate();
     * }
     * </pre>
     *
     * @return a builder to be used to continue building the predicate
     *
     * @throws IllegalStateException if the block is unbalanced (missing end call)
     */
    public PredicateBuilderPredicate end() {
      if (outer == null) {
        throw new IllegalStateException("Unbalanced block - missing begin.");
      }
      outer.addPredicate(getPredicate());
      return outer.getPredicateBuilderWithPredicate();
    }
  }
}
