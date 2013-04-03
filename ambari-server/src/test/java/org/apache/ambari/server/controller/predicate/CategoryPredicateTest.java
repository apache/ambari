package org.apache.ambari.server.controller.predicate;

import junit.framework.Assert;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

/**
 * Tests for bas category predicate.
 */
public class CategoryPredicateTest {
  @Test
  public void testAccept() {
    String propertyId = PropertyHelper.getPropertyId("category1", "foo");
    TestCategoryPredicate predicate = new TestCategoryPredicate(propertyId);

    TestPredicateVisitor visitor = new TestPredicateVisitor();
    predicate.accept(visitor);

    Assert.assertSame(predicate, visitor.visitedCategoryPredicate);
  }

  public static class TestCategoryPredicate extends CategoryPredicate {

    public TestCategoryPredicate(String propertyId) {
      super(propertyId);
    }

    @Override
    public boolean evaluate(Resource resource) {
      return false;
    }
  }

  public static class TestPredicateVisitor implements PredicateVisitor {

    CategoryPredicate visitedCategoryPredicate = null;

    @Override
    public void acceptComparisonPredicate(ComparisonPredicate predicate) {
    }

    @Override
    public void acceptArrayPredicate(ArrayPredicate predicate) {
    }

    @Override
    public void acceptUnaryPredicate(UnaryPredicate predicate) {
    }

    @Override
    public void acceptAlwaysPredicate(AlwaysPredicate predicate) {
    }

    @Override
    public void acceptCategoryPredicate(CategoryPredicate predicate) {
      visitedCategoryPredicate = predicate;
    }
  }
}
