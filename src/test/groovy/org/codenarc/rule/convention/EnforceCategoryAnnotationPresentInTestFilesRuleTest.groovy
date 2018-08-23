/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codenarc.rule.convention

import org.codenarc.rule.AbstractRuleTestCase
import org.junit.Test

/**
 * Tests for EnforceCategoryAnnotationPresentInTestFilesRule
 *
 * @author Mustafa Munjid
 */
class EnforceCategoryAnnotationPresentInTestFilesRuleTest extends AbstractRuleTestCase<EnforceCategoryAnnotationPresentInTestFilesRule> {

  @Test
  void testRuleProperties() {
    assert rule.priority == 2
    assert rule.name == 'EnforceCategoryAnnotationPresentInTestFiles'
  }

  @Test
  void testNoViolations() {
    final SOURCE = '''
            import org.junit.experimental.categories.Category

    @Category(UnitTest)
    class MyClassTest {
    }

    class MyClass {
    }
    '''
    assertNoViolations(SOURCE)
  }

  @Test
  void testSingleViolation() {
    final SOURCE = '''
    class MyClassTest {
    }
    '''
    assertSingleViolation(SOURCE, 2, 'class MyClassTest {')
  }

  @Test
  void testMultipleViolations() {
    final SOURCE = '''
    class MyClassTest {
    }

    @Category(UnitTest)
    class MySecondClassTest {
    }
    '''
    assertViolations(SOURCE,
        [lineNumber:2, sourceLineText:'class MyClassTest {', messageText:'@Category annotation not present on test class.'],
            [lineNumber:6, sourceLineText:'class MySecondClassTest {', messageText:'@Category annotation imported from wrong package.'])
  }

  @Override
  protected EnforceCategoryAnnotationPresentInTestFilesRule createRule() {
    new EnforceCategoryAnnotationPresentInTestFilesRule()
  }
}