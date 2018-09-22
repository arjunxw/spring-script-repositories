package com.company.rnd.scriptrepo.repository.config;

import com.company.rnd.scriptrepo.repository.factory.ScriptRepositoryFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptRepositoryConfigurationParser implements BeanDefinitionParser {

    private static final Logger log = LoggerFactory.getLogger(ScriptRepositoryConfigurationParser.class);

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) throws BeanCreationException {
        List<String> basePackages;
        Map<Class<? extends Annotation>, ScriptInfo>  customAnnotationsConfig;
        try {
            basePackages = getPackagesToScan(element);
            customAnnotationsConfig = getCustomAnnotationsConfig(element);
        } catch (ClassNotFoundException e) {
            throw new BeanCreationException(String.format("Error parsing bean definitions: %s", e.getMessage()), e);
        }

        if (!parserContext.getRegistry().containsBeanDefinition(ScriptRepositoryFactoryBean.NAME)) {
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ScriptRepositoryFactoryBean.class);
            builder.addConstructorArgValue(basePackages);
            builder.addConstructorArgValue(customAnnotationsConfig);
            AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
            parserContext.getRegistry().registerBeanDefinition(ScriptRepositoryFactoryBean.NAME, beanDefinition);
            return beanDefinition;
        } else {
            BeanDefinition definition = parserContext.getRegistry().getBeanDefinition(ScriptRepositoryFactoryBean.NAME);
            List<String> basePackagesArg = (List<String>)definition.getConstructorArgumentValues().getArgumentValue(0, List.class).getValue();
            basePackagesArg.addAll(basePackages);
            Map<Class<? extends Annotation>, ScriptInfo> customAnnotationsArg =
                    (Map<Class<? extends Annotation>, ScriptInfo>)definition.getConstructorArgumentValues().getArgumentValue(0, Map.class).getValue();
            customAnnotationsArg.putAll(customAnnotationsConfig);
            return definition;
        }
    }

    private static List<String> getPackagesToScan(Element element){
        log.trace("Reading packages to be scanned to find Script Repositories");
        Element basePackagesEl = DomUtils.getChildElementByTagName(element, "base-packages");
        List<Element> packageNames = DomUtils.getChildElementsByTagName(basePackagesEl, "base-package");
        int basePackagesCount = packageNames.size();
        List<String> result = new ArrayList<>(basePackagesCount);
        for (Element el : packageNames) {
            result.add(el.getTextContent());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<Class<? extends Annotation>, ScriptInfo> getCustomAnnotationsConfig(Element element) throws ClassNotFoundException {
        log.trace("Reading annotations configurations to create script methods later");
        Map<Class<? extends Annotation>, ScriptInfo> result = new HashMap<>();
        Element annotConfigEl = DomUtils.getChildElementByTagName(element, "annotations-config");
        if (annotConfigEl == null){
            return result;
        }
        List<Element> annotConfig = DomUtils.getChildElementsByTagName(annotConfigEl, "annotation-mapping");
        for (Element el : annotConfig) {
            String providerBeanName = el.getAttribute("provider-bean-name");
            String executorBeanName = el.getAttribute("executor-bean-name");
            Class<? extends Annotation> annotationClass = (Class<? extends Annotation>)Class.forName(el.getAttribute("annotation-class"));
            result.put(annotationClass, new ScriptInfo(annotationClass, providerBeanName, executorBeanName));
        }
        return result;
    }
}
