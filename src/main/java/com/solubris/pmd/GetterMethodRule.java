package com.solubris.pmd;

import net.sourceforge.pmd.lang.java.ast.ASTBlock;
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTStatement;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;
import net.sourceforge.pmd.properties.PropertyDescriptor;
import net.sourceforge.pmd.reporting.RuleContext;

import java.text.MessageFormat;

import static net.sourceforge.pmd.lang.java.ast.JModifier.PUBLIC;
import static net.sourceforge.pmd.properties.PropertyFactory.booleanProperty;
import static net.sourceforge.pmd.properties.PropertyFactory.intProperty;

public class GetterMethodRule extends AbstractJavaRule {
    private static final PropertyDescriptor<Integer> ALLOWED_PARAMETERS =
            intProperty("allowedParameters")
                    .desc("Maximum number of parameters allowed in getter methods (-1 to disable)")
                    .defaultValue(0)
                    .build();
    private static final PropertyDescriptor<Integer> ALLOWED_STATEMENTS =
            intProperty("allowedStatements")
                    .desc("Maximum number of statements allowed in getter methods")
                    .defaultValue(1)
                    .build();
    private static final PropertyDescriptor<Boolean> IGNORE_STATIC =
            booleanProperty("ignoreStatic")
                    .desc("Ignore static getter methods completely (do not check them)")
                    .defaultValue(false)
                    .build();
    private static final PropertyDescriptor<Boolean> CHECK_NOT_FINAL =
            booleanProperty("checkNotFinal")
                    .desc("Check if getter methods are not final (they shouldn't be)")
                    .defaultValue(true)
                    .build();
    private static final PropertyDescriptor<Boolean> CHECK_PUBLIC =
            booleanProperty("checkPublic")
                    .desc("Check if getter methods are public (they should be)")
                    .defaultValue(true)
                    .build();

    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    public GetterMethodRule() {
        definePropertyDescriptor(ALLOWED_PARAMETERS);
        definePropertyDescriptor(ALLOWED_STATEMENTS);
        definePropertyDescriptor(IGNORE_STATIC);
        definePropertyDescriptor(CHECK_NOT_FINAL);
        definePropertyDescriptor(CHECK_PUBLIC);
    }

    @Override
    public Object visit(ASTMethodDeclaration node, Object data) {
        checkMethod(node, asCtx(data));
        return null;
    }

    private void checkMethod(ASTMethodDeclaration node, RuleContext context) {
        String methodName = node.getName();
        if (!isGetterMethod(methodName)) return;

        if (node.isStatic()) {
            boolean ignoreStatic = getProperty(IGNORE_STATIC);
            if (ignoreStatic) {
                return;
            } else {
                context.addViolation(node,
                        methodName,
                        "should not be static. Consider making it an instance method or renaming if it's a utility method");
            }
        }

        checkNotFinal(node, context, methodName, getProperty(CHECK_NOT_FINAL));
        checkPublic(node, context, methodName, getProperty(CHECK_PUBLIC));
        checkParameters(node, context, methodName, getProperty(ALLOWED_PARAMETERS));
        checkStatements(node, context, methodName, getProperty(ALLOWED_STATEMENTS));
    }

    private static void checkNotFinal(ASTMethodDeclaration node,
                                      RuleContext context,
                                      String methodName,
                                      boolean check) {
        if (!check) return;
        if (!node.isFinal()) return;

        context.addViolation(node,
                methodName,
                "should not be final. Getters should be overridable unless there's a specific reason");
    }

    private static void checkPublic(ASTMethodDeclaration node,
                                    RuleContext context,
                                    String methodName,
                                    boolean check) {
        if (!check) return;
        if (node.hasModifiers(PUBLIC)) return;

        context.addViolation(node,
                methodName,
                "should be public. Getters should be accessible from outside the class");
    }

    private static void checkParameters(ASTMethodDeclaration node,
                                        RuleContext context,
                                        String methodName,
                                        int max) {
        if (max < 0) return;

        if (node.getFormalParameters().size() > max) {
            String message = MessageFormat.format(
                    "has {0} parameters but should have at most {1}. " +
                            "Consider renaming to determine/produce/find/extract/retrieve/compute/collect/gather if it needs parameters",
                    node.getFormalParameters().size(),
                    max
            );
            context.addViolation(node, methodName, message);
        }
    }

    private static void checkStatements(ASTMethodDeclaration node,
                                        RuleContext context,
                                        String methodName,
                                        int max) {
        if (max < 0) return;

        ASTBlock block = node.getBody();
        // null block means the method is abstract. What to do in this case?
        // Does it make sense to have abstract getters?
        if (block == null) return;

        int statements = block.descendants(ASTStatement.class).count();
        if (statements <= max) return;

        String message = MessageFormat.format(
                "has {0} statements but should have at most {1}. " +
                        "Consider renaming to determine/produce/find/extract/retrieve/compute/collect/gather if it performs complex logic",
                statements,
                max
        );
        context.addViolation(node, methodName, message);
    }

    private static boolean isGetterMethod(String methodName) {
        if (methodName == null) return false;
        if (methodName.length() <= 3) return false;
        if (!methodName.startsWith("get")) return false;
        return Character.isUpperCase(methodName.charAt(3));
    }
}