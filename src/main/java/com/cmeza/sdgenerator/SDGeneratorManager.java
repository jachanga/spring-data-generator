package com.cmeza.sdgenerator;

import com.cmeza.sdgenerator.annotation.SDGenerator;
import com.cmeza.sdgenerator.support.ManagerTemplateSupport;
import com.cmeza.sdgenerator.support.RepositoryTemplateSupport;
import com.cmeza.sdgenerator.support.ScanningConfigurationSupport;
import com.cmeza.sdgenerator.util.GeneratorUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

import java.util.Collection;

/**
 * Created by carlos on 08/04/17.
 */
public class SDGeneratorManager implements ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware {

    private final static Log logger = LogFactory.getLog(SDGeneratorManager.class);
    private Environment environment;
    private ResourceLoader resourceLoader;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {
        Assert.notNull(annotationMetadata, "AnnotationMetadata must not be null!");
        Assert.notNull(beanDefinitionRegistry, "BeanDefinitionRegistry must not be null!");

        if(annotationMetadata.getAnnotationAttributes(SDGenerator.class.getName()) != null) {

            AnnotationAttributes attributes = new AnnotationAttributes(annotationMetadata.getAnnotationAttributes(SDGenerator.class.getName()));

            String repositoryPackage = attributes.getString("repositoryPackage");
            String managerPackage = attributes.getString("managerPackage");

            if (!managerPackage.isEmpty() && repositoryPackage.isEmpty()) {
                logger.error("Repositories must be generated before generating managers");
                return;
            }

            if (!repositoryPackage.isEmpty() || !managerPackage.isEmpty()){
                ScanningConfigurationSupport configurationSource = new ScanningConfigurationSupport(annotationMetadata, attributes, this.environment);

                Collection<BeanDefinition> candidates = configurationSource.getCandidates(resourceLoader);

                String absolutePath = GeneratorUtils.getAbsolutePath();
                if (!repositoryPackage.isEmpty()){

                    String repositoriesPath;
                    if (absolutePath != null){
                        repositoriesPath = absolutePath + repositoryPackage.replace(".", "/");
                    } else {
                        logger.error("Could not define the absolute path of the repositories");
                        return;
                    }

                    RepositoryTemplateSupport repositoryTemplateSupport = new RepositoryTemplateSupport(attributes);
                    repositoryTemplateSupport.initializeCreation(repositoriesPath, repositoryPackage, candidates);
                }

                if (!repositoryPackage.isEmpty() && !managerPackage.isEmpty()) {

                    String managerPath;
                    if (absolutePath != null){
                        managerPath = absolutePath + managerPackage.replace(".", "/");
                    } else {
                        logger.error("Could not define the absolute path of the manager");
                        return;
                    }

                    String repositoryPostfix = attributes.getString("repositoryPostfix");

                    ManagerTemplateSupport managerTemplateSupport = new ManagerTemplateSupport(attributes, repositoryPackage, repositoryPostfix);
                    managerTemplateSupport.initializeCreation(managerPath, managerPackage, candidates);
                }

            }

        }
    }

}