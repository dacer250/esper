/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.codegen.model.expression;

import java.util.Map;
import java.util.Set;

public class CodegenExpressionExprDotUnderlying implements CodegenExpression {
    private final CodegenExpression expression;

    public CodegenExpressionExprDotUnderlying(CodegenExpression expression) {
        this.expression = expression;
    }

    public void render(StringBuilder builder, Map<Class, String> imports) {
        if (expression instanceof CodegenExpressionRef) {
            expression.render(builder, imports);
        } else {
            builder.append("(");
            expression.render(builder, imports);
            builder.append(")");
        }
        builder.append(".getUnderlying()");
    }

    public void mergeClasses(Set<Class> classes) {
        expression.mergeClasses(classes);
    }
}
