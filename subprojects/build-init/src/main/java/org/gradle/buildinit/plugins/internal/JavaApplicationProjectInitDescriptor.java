/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.buildinit.plugins.internal;

import org.gradle.api.internal.DocumentationRegistry;
import org.gradle.buildinit.plugins.internal.modifiers.ComponentType;
import org.gradle.buildinit.plugins.internal.modifiers.Language;

public class JavaApplicationProjectInitDescriptor extends JavaProjectInitDescriptor {
    private final TemplateLibraryVersionProvider libraryVersionProvider;

    public JavaApplicationProjectInitDescriptor(TemplateLibraryVersionProvider libraryVersionProvider, DocumentationRegistry documentationRegistry) {
        super(libraryVersionProvider, documentationRegistry);
        this.libraryVersionProvider = libraryVersionProvider;
    }

    @Override
    public String getId() {
        return "java-application";
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.APPLICATION;
    }

    @Override
    public boolean supportsModularization() {
        return true;
    }

    @Override
    public void generateBuildScript(String projectOrConventionName, InitSettings settings, BuildScriptBuilder buildScriptBuilder) {
        super.generateBuildScript(projectOrConventionName, settings, buildScriptBuilder);

        if (settings.isModularized()) {
            if ("app".equals(projectOrConventionName)) {
                buildScriptBuilder
                    .plugin(null, settings.getPackageName() + ".java-application-conventions")
                    .block(null, "application", b -> b.propertyAssignment("Define the main class for the application.", "mainClass", withPackage(settings, "app.App"), false))
                    .dependencies().projectDependency("implementation", null, ":utilities");
            } else {
                if ("utilities".equals(projectOrConventionName)) {
                    buildScriptBuilder
                        .plugin(null, settings.getPackageName() + ".java-library-conventions")
                        .dependencies().projectDependency("api", null, ":list");
                } else if ("list".equals(projectOrConventionName)) {
                    buildScriptBuilder
                        .plugin(null, settings.getPackageName() + ".java-library-conventions");
                }
            }
        }
    }

    @Override
    public void generateSources(InitSettings settings, TemplateFactory templateFactory) {
        super.generateSources(settings, templateFactory);

        if (settings.isModularized()) {
            templateFactory.whenNoSourcesAvailable(
                templateFactory.fromSourceTemplate("javaapp/multi/app/App.java.template", "main"),
                templateFactory.fromSourceTemplate("javaapp/multi/app/MessageUtils.java.template", "main"),
                templateFactory.fromSourceTemplate("javaapp/multi/app/junit5/MessageUtilsTest.java.template", "test")
            ).generate();

            templateFactory.whenNoSourcesAvailable("list",
                templateFactory.fromSourceTemplate("javaapp/multi/list/LinkedList.java.template", "main", "list"),
                templateFactory.fromSourceTemplate("javaapp/multi/list/junit5/LinkedListTest.java.template", "test", "list")
            ).generate();

            templateFactory.whenNoSourcesAvailable("utilities",
                templateFactory.fromSourceTemplate("javaapp/multi/utilities/JoinUtils.java.template", "main", "utilities"),
                templateFactory.fromSourceTemplate("javaapp/multi/utilities/SplitUtils.java.template", "main", "utilities"),
                templateFactory.fromSourceTemplate("javaapp/multi/utilities/StringUtils.java.template", "main", "utilities")
            ).generate();
        }
    }

    @Override
    protected void configureBuildScript(InitSettings settings, BuildScriptBuilder buildScriptBuilder) {
        configureApplicationBuildScript(buildScriptBuilder);

        buildScriptBuilder
            .block(null, "application", b -> b.propertyAssignment("Define the main class for the application.", "mainClass", withPackage(settings, "App"), false))
            .implementationDependency("This dependency is used by the application.",
                "com.google.guava:guava:" + libraryVersionProvider.getVersion("guava"));
    }

    @Override
    protected TemplateOperation sourceTemplateOperation(InitSettings settings, TemplateFactory templateFactory) {
        if (settings.isModularized()) {
            return () -> {};
        } else {
            return templateFactory.fromSourceTemplate("javaapp/App.java.template", "main");
        }
    }

    @Override
    protected TemplateOperation testTemplateOperation(InitSettings settings, TemplateFactory templateFactory) {
        if (settings.isModularized()) {
            return () -> {};
        }
        switch (settings.getTestFramework()) {
            case SPOCK:
                return templateFactory.fromSourceTemplate("groovyapp/AppTest.groovy.template", "test", settings.getSubprojects().get(0), Language.GROOVY);
            case TESTNG:
                return templateFactory.fromSourceTemplate("javaapp/testng/AppTest.java.template", "test");
            case JUNIT:
                return templateFactory.fromSourceTemplate("javaapp/AppTest.java.template", "test");
            case JUNIT_JUPITER:
                return templateFactory.fromSourceTemplate("javaapp/junitjupiter/AppTest.java.template", "test");
            default:
                throw new IllegalArgumentException();
        }
    }
}
