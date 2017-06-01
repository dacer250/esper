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
package com.espertech.esper.event.map;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventPropertyGetterSPI;
import com.espertech.esper.event.bean.BeanEventPropertyGetter;

import java.util.List;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Getter for one or more levels deep nested properties of maps.
 */
public class MapNestedPropertyGetterMixedType implements MapEventPropertyGetter {
    private final EventPropertyGetterSPI[] getterChain;

    /**
     * Ctor.
     *
     * @param getterChain        is the chain of getters to retrieve each nested property
     * @param eventAdaperService is a factory for POJO bean event types
     */
    public MapNestedPropertyGetterMixedType(List<EventPropertyGetterSPI> getterChain,
                                            EventAdapterService eventAdaperService) {
        this.getterChain = getterChain.toArray(new EventPropertyGetterSPI[getterChain.size()]);
    }

    public Object getMap(Map<String, Object> map) throws PropertyAccessException {
        Object result = ((MapEventPropertyGetter) getterChain[0]).getMap(map);
        return handleGetterTrailingChain(result);
    }

    private String getMapCodegen(CodegenContext context) throws PropertyAccessException {
        return context.addMethod(Object.class, Map.class, "map", this.getClass())
                .declareVar(Object.class, "result", getterChain[0].codegenUnderlyingGet(ref("map"), context))
                .methodReturn(localMethod(handleGetterTrailingChainCodegen(context), ref("result")));
    }

    public boolean isMapExistsProperty(Map<String, Object> map) {
        if (!((MapEventPropertyGetter) getterChain[0]).isMapExistsProperty(map)) {
            return false;
        }
        Object result = ((MapEventPropertyGetter) getterChain[0]).getMap(map);
        return handleIsExistsTrailingChain(result);
    }

    private String isMapExistsPropertyCodegen(CodegenContext context) throws PropertyAccessException {
        return context.addMethod(boolean.class, Map.class, "map", this.getClass())
                .ifConditionReturnConst(getterChain[0].codegenUnderlyingExists(ref("map"), context), false)
                .declareVar(Object.class, "result", getterChain[0].codegenUnderlyingGet(ref("map"), context))
                .methodReturn(localMethod(handleIsExistsTrailingChainCodegen(context), ref("result")));
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        Object result = getterChain[0].get(eventBean);
        return handleGetterTrailingChain(result);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        if (!getterChain[0].isExistsProperty(eventBean)) {
            return false;
        }
        Object result = getterChain[0].get(eventBean);
        return handleIsExistsTrailingChain(result);
    }

    public CodegenExpression codegenEventBeanGet(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingGet(castUnderlying(Map.class, beanExpression), context);
    }

    public CodegenExpression codegenEventBeanExists(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingExists(castUnderlying(Map.class, beanExpression), context);
    }

    public CodegenExpression codegenEventBeanFragment(CodegenExpression beanExpression, CodegenContext context) {
        return constantNull();
    }

    public CodegenExpression codegenUnderlyingGet(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getMapCodegen(context), underlyingExpression);
    }

    public CodegenExpression codegenUnderlyingExists(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(isMapExistsPropertyCodegen(context), underlyingExpression);
    }

    public CodegenExpression codegenUnderlyingFragment(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantNull();
    }

    private boolean handleIsExistsTrailingChain(Object result) {
        for (int i = 1; i < getterChain.length; i++) {
            if (result == null) {
                return false;
            }

            EventPropertyGetter getter = getterChain[i];

            if (i == getterChain.length - 1) {
                if (getter instanceof BeanEventPropertyGetter) {
                    return ((BeanEventPropertyGetter) getter).isBeanExistsProperty(result);
                } else if (result instanceof Map && getter instanceof MapEventPropertyGetter) {
                    return ((MapEventPropertyGetter) getter).isMapExistsProperty((Map) result);
                } else if (result instanceof EventBean) {
                    return getter.isExistsProperty((EventBean) result);
                } else {
                    return false;
                }
            }

            if (getter instanceof BeanEventPropertyGetter) {
                result = ((BeanEventPropertyGetter) getter).getBeanProp(result);
            } else if (result instanceof Map && getter instanceof MapEventPropertyGetter) {
                result = ((MapEventPropertyGetter) getter).getMap((Map) result);
            } else if (result instanceof EventBean) {
                result = getter.get((EventBean) result);
            } else {
                return false;
            }
        }
        return false;
    }

    private String handleIsExistsTrailingChainCodegen(CodegenContext context) {
        CodegenBlock block = context.addMethod(boolean.class, Object.class, "result", this.getClass());
        for (int i = 1; i < getterChain.length - 1; i++) {
            block.ifRefNullReturnFalse("result");
            EventPropertyGetterSPI getter = getterChain[i];
            CodegenBlock blockBean = block.ifInstanceOf("result", EventBean.class);
            blockBean.assignRef("result", getter.codegenEventBeanGet(cast(EventBean.class, ref("result")), context));

            if (getter instanceof BeanEventPropertyGetter) {
                Class type = ((BeanEventPropertyGetter) getter).getTargetType();
                blockBean.blockElse()
                        .assignRef("result", getter.codegenUnderlyingGet(cast(type, ref("result")), context))
                        .blockEnd();
            } else if (getter instanceof MapEventPropertyGetter) {
                blockBean.blockElse()
                        .ifRefNotTypeReturnConst("result", Map.class, false)
                        .assignRef("result", getter.codegenUnderlyingGet(cast(Map.class, ref("result")), context))
                        .blockEnd();
            } else {
                blockBean.blockElse().blockReturn(constantFalse());
            }
        }

        EventPropertyGetterSPI getter = getterChain[getterChain.length - 1];
        if (getter instanceof BeanEventPropertyGetter) {
            BeanEventPropertyGetter beanGetter = (BeanEventPropertyGetter) getter;
            return block.methodReturn(getter.codegenUnderlyingExists(cast(beanGetter.getTargetType(), ref("result")), context));
        } else if (getter instanceof MapEventPropertyGetter) {
            return block.methodReturn(getter.codegenUnderlyingExists(cast(Map.class, ref("result")), context));
        } else {
            block.ifInstanceOf("result", EventBean.class)
                    .blockReturn(getter.codegenEventBeanExists(cast(EventBean.class, ref("result")), context));
            return block.methodReturn(constantFalse());
        }
    }

    public Object getFragment(EventBean eventBean) {
        return null;
    }

    private Object handleGetterTrailingChain(Object result) {

        for (int i = 1; i < getterChain.length; i++) {
            if (result == null) {
                return null;
            }
            EventPropertyGetter getter = getterChain[i];
            if (result instanceof EventBean) {
                result = getter.get((EventBean) result);
            } else if (getter instanceof BeanEventPropertyGetter) {
                result = ((BeanEventPropertyGetter) getter).getBeanProp(result);
            } else if (result instanceof Map && getter instanceof MapEventPropertyGetter) {
                result = ((MapEventPropertyGetter) getter).getMap((Map) result);
            } else {
                return null;
            }
        }
        return result;
    }

    private String handleGetterTrailingChainCodegen(CodegenContext context) {
        CodegenBlock block = context.addMethod(Object.class, Object.class, "result", this.getClass());
        for (int i = 1; i < getterChain.length; i++) {
            block.ifRefNullReturnNull("result");
            EventPropertyGetterSPI getter = getterChain[i];
            CodegenBlock blockBean = block.ifInstanceOf("result", EventBean.class);
            blockBean.assignRef("result", getter.codegenEventBeanGet(cast(EventBean.class, ref("result")), context));
            if (getter instanceof BeanEventPropertyGetter) {
                Class type = ((BeanEventPropertyGetter) getter).getTargetType();
                blockBean.blockElse()
                        .assignRef("result", getter.codegenUnderlyingGet(cast(type, ref("result")), context))
                        .blockEnd();
            } else if (getter instanceof MapEventPropertyGetter) {
                blockBean.blockElse()
                        .ifRefNotTypeReturnConst("result", Map.class, null)
                        .assignRef("result", getter.codegenUnderlyingGet(cast(Map.class, ref("result")), context))
                        .blockEnd();
            } else {
                blockBean.blockElse().blockReturn(constantNull());
            }
        }
        return block.methodReturn(ref("result"));
    }
}
