package com.solubris.pmd;

import net.sourceforge.pmd.lang.rule.Rule;
import net.sourceforge.pmd.test.RuleTst;

import java.util.Collections;
import java.util.List;

public abstract class AbstractRuleTst extends RuleTst {
    @Override
    protected List<Rule> getRules() {
        String ruleName = getClass().getSimpleName().replaceFirst("Test$", "");
        String rulesetXml = "com/solubris/java.xml";
        Rule rule = findRule(rulesetXml, ruleName);
        return Collections.singletonList(rule);
    }
}