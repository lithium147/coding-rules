package com.solubris.pmd;

import net.sourceforge.pmd.lang.java.ast.ASTExpression;
import net.sourceforge.pmd.lang.java.ast.ASTMethodCall;
import net.sourceforge.pmd.lang.java.ast.QualifiableExpression;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;
import net.sourceforge.pmd.reporting.RuleContext;

import java.util.Optional;

public class UnmodifiableSetCollectorRule extends AbstractJavaRule {
    @Override
    public Object visit(ASTMethodCall node, Object data) {
        Optional.of(node)
                .filter(n -> n.getMethodName().equals("collect"))
                .map(ASTMethodCall::getArguments)
                .filter(args -> args.size() == 1)
                .map(args -> args.getFirstChild())
                .map(UnmodifiableSetCollectorRule::unwrapMethodCall)
                .filter(argCall -> argCall.getMethodName().equals("toSet"))
                .map(QualifiableExpression::getQualifier)
//                .filter(ASTVariableAccess.class::isInstance)
                .filter(qualifier -> "Collectors".equals(qualifier.getFirstToken().getImage()))
                .ifPresent(qualifier -> addViolation(node, asCtx(data)));

        return data;
/*
        // Check method name is 'collect'
        if (!"collect".equals(node.getMethodName())) return data;

        ASTList<?> args = node.getArguments();
        if (args.size() != 1) {
            return data;
        }

        Object arg = args.get(0);
        ASTMethodCall argCall = unwrapMethodCall(arg);
        if (argCall == null) return null;
        ASTExpression qualifier = argCall.getQualifier();
        if ("toSet".equals(argCall.getMethodName())
//                && qualifier instanceof ASTVariableAccess
                && "Collectors".equals(qualifier.getFirstToken().getImage())) {

            addViolation(node, data);
        }

        return data;

 */
    }

    private static void addViolation(ASTMethodCall node, RuleContext ctx) {
        ctx.addViolation(node,
                ".collect(Collectors.toSet()) should be replaced with .collect(toUnmodifiableSet())");
    }

    // Recursively unwraps ASTExpression nodes to find an ASTMethodCall
    private static ASTMethodCall unwrapMethodCall(Object node) {
        if (node instanceof ASTMethodCall) {
            return (ASTMethodCall) node;
        }

        if (node instanceof ASTExpression expr && expr.getNumChildren() == 1) {
            return unwrapMethodCall(expr.getChild(0));
        }

        return null;
    }
}