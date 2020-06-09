package com.wujiuye.jcg.tree;

import com.wujiuye.jcg.AnnotationRule;
import com.wujiuye.jcg.AnnotationRuleRegister;
import com.wujiuye.jcg.util.StringUtils;

import java.lang.annotation.ElementType;
import java.util.List;

/**
 * 注解规则应用工具类
 *
 * @author wujiuye 解析映射规则
 */
public class AnnotationRuleApplyUtils {

    /**
     * 映射注解
     *
     * @param dynamicClass 解析json生成的类
     */
    public static void applyAnnotationRule(DynamicClass dynamicClass) {
        List<AnnotationRule> rules = AnnotationRuleRegister.getRules(dynamicClass.getClassName());
        if (rules == null || rules.isEmpty()) {
            return;
        }
        rules.parallelStream().forEach(rule -> {
            ElementType elementType = rule.getElementType();
            if (elementType == ElementType.TYPE) {
                applyAnnotationRuleByType(dynamicClass, rule);
            } else {
                applyAnnotationRuleByField(dynamicClass, rule);
            }
        });
    }

    private static void applyAnnotationRuleByType(DynamicClass dynamicClass, AnnotationRule rule) {
        String path = rule.getPath();
        // 应用在根类上
        if (StringUtils.isEmpty(path)) {
            dynamicClass.addAnnotation(toNode(rule));
            return;
        }
        String[] childPath = path.split("\\.");
        DynamicClass chilClass = dynamicClass;
        for (String chil : childPath) {
            if (chilClass.getFields() == null || chilClass.getFields().size() == 0) {
                throw new RuntimeException("映射规则路径配置错误，找不到路径！");
            }
            for (FieldNode fieldNode : chilClass.getFields()) {
                if (fieldNode.getFieldName().equals(chil)) {
                    chilClass = fieldNode.getDynamicClass();
                    if (chilClass == null) {
                        throw new RuntimeException("不是字节码生成的类，不支持！");
                    }
                    break;
                }
            }
        }
        chilClass.addAnnotation(toNode(rule));
    }

    private static void applyAnnotationRuleByField(DynamicClass dynamicClass, AnnotationRule rule) {
        if (StringUtils.isEmpty(rule.getPath())) {
            throw new RuntimeException("路径错误！");
        }
        String[] childPath = rule.getPath().split("\\.");
        DynamicClass chilClass = dynamicClass;
        FieldNode tag = null;
        for (String chil : childPath) {
            if (chilClass == null) {
                throw new RuntimeException("不是字节码生成的类，不支持！");
            }
            if (chilClass.getFields() == null || chilClass.getFields().size() == 0) {
                throw new RuntimeException("映射规则路径配置错误，找不到路径！");
            }
            for (FieldNode fieldNode : chilClass.getFields()) {
                if (fieldNode.getFieldName().equals(chil)) {
                    tag = fieldNode;
                    chilClass = fieldNode.getDynamicClass();
                    break;
                }
            }
        }
        if (tag == null) {
            throw new RuntimeException("未找到路径！");
        }
        tag.addAnnotation(toNode(rule));
    }


    private static AnnotationNode toNode(AnnotationRule rule) {
        AnnotationNode annotationNode = new AnnotationNode(rule.getAnnoClas());
        if (rule.getAnnoAttrs() != null && rule.getAnnoAttrs().size() > 0) {
            rule.getAnnoAttrs().entrySet().forEach(entry -> annotationNode.putAttr(entry.getKey(), entry.getValue()));
        }
        return annotationNode;
    }

}