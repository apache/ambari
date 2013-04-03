package org.apache.ambari.server.api.predicate.operators;

import org.apache.ambari.server.api.predicate.InvalidQueryException;
import org.apache.ambari.server.controller.predicate.BasePredicate;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.predicate.OrPredicate;
import org.apache.ambari.server.controller.spi.Predicate;

import java.util.ArrayList;
import java.util.List;

/**
 * IN relational operator.
 * This is a binary operator which takes a comma delimited right operand and
 * creates equals predicates with the left operand and each right operand token.
 * The equals predicates are combined with an OR predicate.
 *
 */
public class InOperator extends AbstractOperator implements RelationalOperator {

  public InOperator() {
    super(0);
  }

  @Override
  public String getName() {
    return "InOperator";
  }

  @Override
  public Predicate toPredicate(String prop, String val) throws InvalidQueryException {

    if (val == null) {
      throw new InvalidQueryException("IN operator is missing a required right operand.");
    }

    String[] tokens = val.split(",");
    List<EqualsPredicate> listPredicates = new ArrayList<EqualsPredicate>();
    for (String token : tokens) {
      listPredicates.add(new EqualsPredicate(prop, token.trim()));
    }
    return listPredicates.size() == 1 ? listPredicates.get(0) :
        buildOrPredicate(listPredicates);
  }

  private OrPredicate buildOrPredicate(List<EqualsPredicate> listPredicates) {
    return new OrPredicate(listPredicates.toArray(new BasePredicate[listPredicates.size()]));
  }

  @Override
  public TYPE getType() {
    return TYPE.IN;
  }
}
