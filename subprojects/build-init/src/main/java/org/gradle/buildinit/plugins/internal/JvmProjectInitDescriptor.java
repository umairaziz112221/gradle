/*
 * Copyright 2019 the original author or authors.
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
import org.gradle.buildinit.plugins.internal.model.Description;
import org.gradle.buildinit.plugins.internal.modifiers.BuildInitTestFramework;
import org.gradle.buildinit.plugins.internal.modifiers.Language;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class JvmProjectInitDescriptor extends LanguageLibraryProjectInitDescriptor {

    protected final Description description;

    protected final TemplateLibraryVersionProvider libraryVersionProvider;
    private final DocumentationRegistry documentationRegistry;

    public JvmProjectInitDescriptor(Description description, TemplateLibraryVersionProvider libraryVersionProvider, DocumentationRegistry documentationRegistry) {
        this.description = description;
        this.libraryVersionProvider = libraryVersionProvider;
        this.documentationRegistry = documentationRegistry;
    }

    @Override
    public String getId() {
        return getLanguage().getName() + "-" + getComponentType().toString();
    }

    @Override
    public Language getLanguage() {
        return description.getLanguage();
    }

    @Override
    public boolean supportsPackage() {
        return true;
    }

    @Override
    public BuildInitTestFramework getDefaultTestFramework() {
        return description.getDefaultTestFramework();
    }

    @Override
    public Set<BuildInitTestFramework> getTestFrameworks() {
        return description.getSupportedTestFrameworks();
    }

    @Override
    public Optional<String> getFurtherReading() {
        return description.getUserManualId() == null ? Optional.empty() :
            Optional.of(documentationRegistry.getDocumentationFor(description.getUserManualId()));
    }

    @Override
    public void generateProjectBuildScript(String projectName, InitSettings settings, BuildScriptBuilder buildScriptBuilder) {
        addJcenter(buildScriptBuilder);
        String languagePlugin = description.getPluginName();
        if (languagePlugin != null) {
            String pluginVersionProperty = description.getPluginVersionProperty();
            String pluginVersion = pluginVersionProperty == null ? null : libraryVersionProvider.getVersion(pluginVersionProperty);
            buildScriptBuilder.plugin("Apply the " + languagePlugin + " Plugin to add support for " + getLanguage(), languagePlugin, pluginVersion);
        }

        buildScriptBuilder.fileComment("This generated file contains a sample " + getLanguage() + " " + getComponentType() + " project to get you started.");
        if (description.getUserManualId() != null) {
            buildScriptBuilder.fileComment("For more details take a look at the " + description.getChapterName() + " chapter in the Gradle")
                .fileComment("User Manual available at " + documentationRegistry.getDocumentationFor(description.getUserManualId()));
        }
        addTestFrameworkAndStandardDependencies(settings.getTestFramework(), buildScriptBuilder);
    }

    @Override
    public void generateConventionPluginBuildScript(String conventionPluginName, InitSettings settings, BuildScriptBuilder buildScriptBuilder) {
    }

    @Override
    public void generateSources(InitSettings settings, TemplateFactory templateFactory) {
        for (String subproject : settings.getSubprojects()) {
            List<String> sourceTemplates = new ArrayList<>();
            List<String> testSourceTemplates = new ArrayList<>();
            sourceTemplates(subproject, settings, templateFactory, sourceTemplates);
            testSourceTemplates(subproject, settings, templateFactory, testSourceTemplates);
            templateFactory.whenNoSourcesAvailable(subproject, Stream.concat(
                sourceTemplates.stream().map(t -> templateFactory.fromSourceTemplate(templatePath(t), "main", subproject, templateLanguage(t))),
                testSourceTemplates.stream().map(t -> templateFactory.fromSourceTemplate(templatePath(t), "test", subproject, templateLanguage(t)))
            ).collect(Collectors.toList())).generate();
        }
    }

    private String templatePath(String baseFileName) {
        return getLanguage().getName() + getComponentType().toString() + "/" + baseFileName
            + "." + templateLanguage(baseFileName).getExtension() +".template";
    }

    private Language templateLanguage(String baseFileName) {
        if (baseFileName.startsWith("groovy/")) {
            return Language.GROOVY;
        }
        return getLanguage();
    }

    protected abstract void sourceTemplates(String subproject, InitSettings settings, TemplateFactory templateFactory, List<String> templates);

    protected abstract void testSourceTemplates(String subproject, InitSettings settings, TemplateFactory templateFactory, List<String> templates);

    protected void applyApplicationPlugin(BuildScriptBuilder buildScriptBuilder) {
        buildScriptBuilder.plugin(
            "Apply the application plugin to add support for building a CLI application in Java.",
            "application");
    }

    protected void applyLibraryPlugin(BuildScriptBuilder buildScriptBuilder) {
        buildScriptBuilder.plugin(
            "Apply the java-library plugin for API and implementation separation.",
            "java-library");
    }

    private void addJcenter(BuildScriptBuilder buildScriptBuilder) {
        buildScriptBuilder.repositories().jcenter("Use JCenter for resolving dependencies.");
    }

    private void addTestFrameworkAndStandardDependencies(BuildInitTestFramework testFramework, BuildScriptBuilder buildScriptBuilder) {
        switch (testFramework) {
            case SPOCK:
                if (getLanguage() == Language.GROOVY) {
                    buildScriptBuilder.implementationDependency("Use the latest Groovy version for building this library", "org.codehaus.groovy:groovy-all:" + libraryVersionProvider.getVersion("groovy"));
                } else {
                    buildScriptBuilder
                        .plugin("Apply the groovy plugin to also add support for Groovy (needed for Spock)", "groovy")
                        .testImplementationDependency("Use the latest Groovy version for Spock testing",
                            "org.codehaus.groovy:groovy-all:" + libraryVersionProvider.getVersion("groovy"));
                }
                buildScriptBuilder.testImplementationDependency("Use the awesome Spock testing and specification framework even with Java",
                        "org.spockframework:spock-core:" + libraryVersionProvider.getVersion("spock"),
                        "junit:junit:" + libraryVersionProvider.getVersion("junit"));
                break;
            case TESTNG:
                buildScriptBuilder
                    .testImplementationDependency(
                        "Use TestNG framework, also requires calling test.useTestNG() below",
                        "org.testng:testng:" + libraryVersionProvider.getVersion("testng"))
                    .taskMethodInvocation(
                        "Use TestNG for unit tests.",
                        "test", "Test", "useTestNG");
                break;
            case JUNIT_JUPITER:
                buildScriptBuilder
                    .testImplementationDependency(
                        "Use JUnit Jupiter API for testing.",
                        "org.junit.jupiter:junit-jupiter-api:" + libraryVersionProvider.getVersion("junit-jupiter")
                    ).testRuntimeOnlyDependency(
                    "Use JUnit Jupiter Engine for testing.",
                    "org.junit.jupiter:junit-jupiter-engine:" + libraryVersionProvider.getVersion("junit-jupiter")
                ).taskMethodInvocation(
                    "Use junit platform for unit tests.",
                    "test", "Test", "useJUnitPlatform"
                );
                break;
            case SCALATEST:
                String scalaVersion = libraryVersionProvider.getVersion("scala");
                String scalaLibraryVersion = libraryVersionProvider.getVersion("scala-library");
                buildScriptBuilder.implementationDependency("Use Scala " + scalaVersion + " in our library project", "org.scala-lang:scala-library:" + scalaLibraryVersion);

                String scalaTestVersion = libraryVersionProvider.getVersion("scalatest");
                String scalaTestPlusJunitVersion = libraryVersionProvider.getVersion("scalatestplus-junit");
                String junitVersion = libraryVersionProvider.getVersion("scala-junit");
                String scalaXmlVersion = libraryVersionProvider.getVersion("scala-xml");
                buildScriptBuilder.testImplementationDependency("Use Scalatest for testing our library",
                    "junit:junit:" + junitVersion,
                    "org.scalatest:scalatest_" + scalaVersion + ":" + scalaTestVersion,
                    "org.scalatestplus:junit-4-12_" + scalaVersion + ":" + scalaTestPlusJunitVersion)
                    .testRuntimeOnlyDependency("Need scala-xml at test runtime",
                        "org.scala-lang.modules:scala-xml_" + scalaVersion + ":" + scalaXmlVersion);
                break;
            case KOTLINTEST:
                buildScriptBuilder.dependencies().platformDependency("implementation", "Align versions of all Kotlin components", "org.jetbrains.kotlin:kotlin-bom");
                buildScriptBuilder.implementationDependency("Use the Kotlin JDK 8 standard library.", "org.jetbrains.kotlin:kotlin-stdlib-jdk8");

                buildScriptBuilder
                    .testImplementationDependency("Use the Kotlin test library.", "org.jetbrains.kotlin:kotlin-test")
                    .testImplementationDependency("Use the Kotlin JUnit integration.", "org.jetbrains.kotlin:kotlin-test-junit");
                break;
            default:
                buildScriptBuilder.testImplementationDependency("Use JUnit test framework.", "junit:junit:" + libraryVersionProvider.getVersion("junit"));
                break;
        }
    }
}
