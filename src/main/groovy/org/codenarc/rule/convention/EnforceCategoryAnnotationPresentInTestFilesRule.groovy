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

import org.codehaus.groovy.ast.ClassNode
import org.codenarc.rule.AbstractAstVisitor
import org.codenarc.rule.AbstractAstVisitorRule
import org.codenarc.util.AstUtil

/**
 * Enforce that all test classes are annotated with @Category
 *
 * @author Mustafa Munjid
 */
class EnforceCategoryAnnotationPresentInTestFilesRule extends AbstractAstVisitorRule {

  String name = 'EnforceCategoryAnnotationPresentInTestFiles'
  int priority = 2
  Class astVisitorClass = EnforceCategoryAnnotationPresentInTestFilesAstVisitor
}

class EnforceCategoryAnnotationPresentInTestFilesAstVisitor extends AbstractAstVisitor {

  @Override
  protected void visitClassEx(ClassNode node) {
    if (node.name.endsWith("Test")) {
      if (!AstUtil.hasAnnotation(node, "Category")) {
        addViolation node, "@Category annotation not present on test class."
      } else if (!node.module.imports.any {
        importNode -> importNode.className == "org.junit.experimental.categories.Category"
      }) {
        addViolation node, "@Category annotation imported from wrong package."
      }
    }
    super.visitClassComplete(node)
  }
}
