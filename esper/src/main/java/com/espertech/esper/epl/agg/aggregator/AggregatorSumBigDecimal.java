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
package com.espertech.esper.epl.agg.aggregator;

import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenCtor;
import com.espertech.esper.collection.RefCountedSet;
import com.espertech.esper.codegen.base.CodegenMembersColumnized;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprForge;

import java.math.BigDecimal;
import java.util.function.Consumer;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.epl.agg.aggregator.AggregatorCodegenUtil.sumAndCountBigApplyEnterCodegen;
import static com.espertech.esper.epl.agg.aggregator.AggregatorCodegenUtil.sumAndCountBigApplyLeaveCodegen;

/**
 * Sum for BigInteger values.
 */
public class AggregatorSumBigDecimal implements AggregationMethod {
    protected BigDecimal sum;
    protected long cnt;

    /**
     * Ctor.
     */
    public AggregatorSumBigDecimal() {
        sum = new BigDecimal(0.0);
    }

    public static void rowMemberCodegen(boolean distinct, int column, CodegenCtor ctor, CodegenMembersColumnized membersColumnized) {
        membersColumnized.addMember(column, BigDecimal.class, "sum");
        membersColumnized.addMember(column, long.class, "cnt");
        ctor.getBlock().assignRef(refCol("sum", column), newInstance(BigDecimal.class, constant(0d)));
        if (distinct) {
            membersColumnized.addMember(column, RefCountedSet.class, "distinctSet");
            ctor.getBlock().assignRef(refCol("distinctSet", column), newInstance(RefCountedSet.class));
        }
    }

    public void enter(Object object) {
        if (object == null) {
            return;
        }
        cnt++;
        sum = sum.add((BigDecimal) object);
    }

    public static void applyEnterCodegen(boolean distinct, boolean hasFilter, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        sumAndCountBigApplyEnterCodegen(BigDecimal.class, distinct, hasFilter, column, method, symbols, forges, classScope);
    }

    public void leave(Object object) {
        if (object == null) {
            return;
        }
        if (cnt <= 1) {
            clear();
        } else {
            cnt--;
            sum = sum.subtract((BigDecimal) object);
        }
    }

    public static void applyLeaveCodegen(boolean distinct, boolean hasFilter, int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        sumAndCountBigApplyLeaveCodegen(clearCode(column), BigDecimal.class, distinct, hasFilter, column, method, symbols, forges, classScope);
    }

    public void clear() {
        sum = new BigDecimal(0.0);
        cnt = 0;
    }

    public static void clearCodegen(boolean distinct, int column, CodegenMethodNode method) {
        method.getBlock().apply(clearCode(column));
        if (distinct) {
            method.getBlock().exprDotMethod(refCol("distinctSet", column), "clear");
        }
    }

    public Object getValue() {
        if (cnt == 0) {
            return null;
        }
        return sum;
    }

    public static void getValueCodegen(int column, CodegenMethodNode method) {
        method.getBlock().ifCondition(equalsIdentity(refCol("cnt", column), constant(0)))
                .blockReturn(constantNull())
                .methodReturn(refCol("sum", column));
    }

    private static Consumer<CodegenBlock> clearCode(int stateNumber) {
        return block -> block.assignRef(refCol("sum", stateNumber), newInstance(BigDecimal.class, constant(0d)))
                .assignRef(refCol("cnt", stateNumber), constant(0));
    }
}
