/*
 * Copyright 2017 the original author or authors.
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
package org.codenarc.rule.formatting

import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.SwitchStatement
import org.codenarc.rule.AbstractAstVisitor
import org.codenarc.rule.AbstractAstVisitorRule

/**
 * Check indentation for class and method declarations
 *
 * @author Chris Mair
 */
class IndentationRule extends AbstractAstVisitorRule {

    String name = 'Indentation'
    int priority = 3
    Class astVisitorClass = IndentationAstVisitor
    int spacesPerIndentLevel = 4

}

class IndentationAstVisitor extends AbstractAstVisitor {

    // Limitations -- does not check: comments, line-continuations, Map entry expressions

    private int indentLevel = 0
    private final Set<Integer> ignoreLineNumbers = []
    private final Set<BlockStatement> nestedBlocks = []

    @Override
    protected void visitClassEx(ClassNode node) {
        indentLevel = nestingLevelForClass(node)

        boolean isInnerClass = node instanceof InnerClassNode
        boolean isAnonymous = isInnerClass && node.anonymous
        if (!isAnonymous) {
            checkForCorrectColumn(node, "class ${node.getNameWithoutPackage()}")
        }
        indentLevel++
        super.visitClassEx(node)
    }

    @Override
    protected void visitMethodEx(MethodNode node) {
        checkForCorrectColumn(node, "method ${node.name} in class ${currentClassName}")
        ignoreLineNumbers << node.lineNumber
        super.visitMethodEx(node)
    }

    @Override
    void visitConstructor(ConstructorNode node) {
        checkForCorrectColumn(node, "constructor in class ${currentClassName}")
        ignoreLineNumbers << node.lineNumber
        super.visitConstructor(node)
    }

    @Override
    void visitClosureExpression(ClosureExpression expression) {
        ignoreLineNumbers << expression.lineNumber
        super.visitClosureExpression(expression)
    }

    @Override
    void visitField(FieldNode node) {
        if (!ignoreLineNumbers.contains(node.lineNumber)) {
            ignoreLineNumbers << node.lineNumber
            checkForCorrectColumn(node, "field ${node.name} in class ${currentClassName}")
        }
        super.visitField(node)
    }

    @Override
    void visitBlockStatement(BlockStatement block) {
        int addToIndentLevel = nestedBlocks.contains(block) ? 0 : 1        // finally blocks have extra level of nested BlockStatement
        indentLevel += addToIndentLevel
        block.statements.each { statement ->
            // Skip statements on the same line as another statement or a field declaration
            if (!ignoreLineNumbers.contains(statement.lineNumber)) {
                ignoreLineNumbers << statement.lineNumber

                // Ignore nested BlockStatement (e.g. finally blocks)
                boolean isNestedBlockStatement = statement instanceof BlockStatement
                if (isNestedBlockStatement) {
                    nestedBlocks << statement
                }

                // Ignore super/this constructor calls -- they have messed up column numbers
                boolean isConstructorCall = (statement instanceof ExpressionStatement) && (statement.expression instanceof ConstructorCallExpression)
                boolean isSuperConstructorCall = isConstructorCall && statement.expression.superCall
                boolean isThisConstructorCall = isConstructorCall && statement.expression.thisCall

                boolean ignoreStatement = isNestedBlockStatement || isSuperConstructorCall || isThisConstructorCall
                if (!ignoreStatement) {
                    checkForCorrectColumn(statement, "statement on line ${statement.lineNumber} in class ${currentClassName}")
                }
            }
        }
        super.visitBlockStatement(block)
        indentLevel -= addToIndentLevel
    }

    @Override
    void visitSwitch(SwitchStatement statement) {
        statement.caseStatements.each { caseStatement ->
            ignoreLineNumbers << caseStatement.lineNumber
        }
        ignoreLineNumbers << statement.defaultStatement.lineNumber
        indentLevel++
        super.visitSwitch(statement)
        indentLevel--
    }

    @Override
    void visitMapEntryExpression(MapEntryExpression expression) {
        // Skip Map entry expressions
        //super.visitMapEntryExpression(expression)
    }

    //------------------------------------------------------------------------------------
    // Helper methods
    //------------------------------------------------------------------------------------

    private void checkForCorrectColumn(ASTNode node, String description) {
        int expectedColumn = columnForIndentLevel(indentLevel)
        if (node.columnNumber != expectedColumn) {
            addViolation(node, "The $description is at the incorrect indent level: Expected column $expectedColumn but was ${node.columnNumber}")
        }
    }

    private int nestingLevelForClass(ClassNode node) {
        // If this is a nested class, then add one to the outer class level
        int level = node.outerClass ? nestingLevelForClass(node.outerClass) + 1 : 0

        // If this class is defined within a method, add one to the level
        level += node.enclosingMethod ? 1 : 0

        return level
    }

    private int columnForIndentLevel(int indentLevel) {
        return indentLevel * rule.spacesPerIndentLevel + 1
    }

}
