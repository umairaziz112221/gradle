/*
 * Copyright 2020 the original author or authors.
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

package gradlebuild.docs.samples;

import org.gradle.api.file.Directory;
import org.gradle.buildinit.plugins.internal.CompositeProjectInitDescriptor;
import org.gradle.buildinit.plugins.internal.InitSettings;
import org.gradle.buildinit.plugins.internal.ProjectLayoutSetupRegistry;
import org.gradle.buildinit.plugins.internal.modifiers.BuildInitDsl;
import org.gradle.buildinit.plugins.internal.modifiers.BuildInitTestFramework;
import org.gradle.buildinit.plugins.internal.modifiers.ComponentType;
import org.gradle.buildinit.plugins.internal.modifiers.Language;

import java.util.List;
import java.util.stream.Collectors;

public class SamplesGenerator {

    public static void generate(String type, boolean modularized, Directory target, ProjectLayoutSetupRegistry projectLayoutSetupRegistry) {
        CompositeProjectInitDescriptor descriptor = (CompositeProjectInitDescriptor) projectLayoutSetupRegistry.get(type);

        String projectName = "demo";
        String packageName = descriptor.supportsPackage() ? projectName : null;
        BuildInitTestFramework testFramework = modularized ? BuildInitTestFramework.JUNIT_JUPITER : descriptor.getDefaultTestFramework();

        InitSettings groovyDslSettings = new InitSettings(projectName, descriptor.getComponentType().getDefaultProjectNames(), modularized, BuildInitDsl.GROOVY, packageName, testFramework, target.dir("groovy"));
        InitSettings kotlinDslSettings = new InitSettings(projectName, descriptor.getComponentType().getDefaultProjectNames(), modularized, BuildInitDsl.KOTLIN, packageName, testFramework, target.dir("kotlin"));

        List<String> comments = descriptor.generateWithExternalComments(groovyDslSettings).values().iterator().next();
        descriptor.generateWithExternalComments(kotlinDslSettings);

        String specificContentId;
        if (descriptor.getLanguage() == Language.CPP || descriptor.getLanguage() == Language.SWIFT) {
            specificContentId = "native-" + descriptor.getComponentType().toString();
        } else {
            specificContentId = descriptor.getComponentType().toString();
        }

        generateReadmeFragment("common-body", groovyDslSettings, comments, descriptor, projectLayoutSetupRegistry);
        generateReadmeFragment(specificContentId + "-body", groovyDslSettings, comments, descriptor, projectLayoutSetupRegistry);
        if (descriptor.getLanguage() == Language.JAVA && descriptor.getComponentType() == ComponentType.LIBRARY) {
            generateReadmeFragment(specificContentId + "-api-docs", groovyDslSettings, comments, descriptor, projectLayoutSetupRegistry);
        }
        generateReadmeFragment("common-summary", groovyDslSettings, comments, descriptor, projectLayoutSetupRegistry);
        generateReadmeFragment(specificContentId + "-summary", groovyDslSettings, comments, descriptor, projectLayoutSetupRegistry);

        generateOutput(specificContentId, groovyDslSettings, descriptor, projectLayoutSetupRegistry);
    }

    private static void generateReadmeFragment(String templateFragment, InitSettings settings, List<String> comments,
                                               CompositeProjectInitDescriptor descriptor, ProjectLayoutSetupRegistry projectLayoutSetupRegistry) {
        List<Language> languages = projectLayoutSetupRegistry.getLanguagesFor(descriptor.getComponentType());

        String exampleClass = descriptor.getComponentType() == ComponentType.LIBRARY ? "Library" : "App";
        String testFileSuffix = descriptor.getLanguage() == Language.SCALA ? "Suite" : "Test";

        String sourceFile;
        String testSourceFile;
        String sourceFileTree;
        String testSourceFileTree;
        if (descriptor.getLanguage() == Language.CPP) {
            exampleClass = descriptor.getComponentType() == ComponentType.LIBRARY ? "Hello" : "Greeter";
            sourceFile = (descriptor.getComponentType() == ComponentType.LIBRARY ? "hello" : "app") + ".cpp";
            testSourceFile = (descriptor.getComponentType() == ComponentType.LIBRARY ? "hello" : "app") + "_test.cpp";
            sourceFileTree =     "        │   │   └── " + sourceFile + "\n" +
                "        │   └── headers  \n" +
                "        │       └── app.h";
            testSourceFileTree = "                └── " + testSourceFile;
        } else if (descriptor.getLanguage() == Language.SWIFT) {
            exampleClass = descriptor.getComponentType() == ComponentType.LIBRARY ? "Hello" : "Greeter";
            sourceFile = (descriptor.getComponentType() == ComponentType.LIBRARY ? "Hello" : "main") + ".swift";
            testSourceFile = exampleClass + "Tests.swift";
            sourceFileTree =     "        │       └── " + sourceFile;
            testSourceFileTree = "                └── " + testSourceFile + "\n" +
                "                └── LinuxMain.swift";
        } else {
            sourceFile = "demo/" + exampleClass + "." + descriptor.getLanguage().getExtension();
            testSourceFile = "demo/" + exampleClass + testFileSuffix + "." + descriptor.getLanguage().getExtension();
            sourceFileTree =     "        │       └── demo\n" +
                "        │           └── " + exampleClass + "." + descriptor.getLanguage().getExtension();
            testSourceFileTree = "        │       └── demo\n" +
                "        │           └── " + exampleClass + testFileSuffix + "." + descriptor.getLanguage().getExtension();
        }

        String buildFileComments = comments.stream().map(c ->
            "<" + (comments.indexOf(c) + 1) + "> " + c).collect(Collectors.joining("\n"));

        String testFrameworkChoice = "";
        if (descriptor.getTestFrameworks().size() > 1) {
            testFrameworkChoice = "\n" +
                "Select test framework:\n" +
                "  1: JUnit 4\n" +
                "  2: TestNG\n" +
                "  3: Spock\n" +
                "  4: JUnit Jupiter\n" +
                "Enter selection (default: JUnit 4) [1..4]\n";
        }

        String packageNameChoice = descriptor.supportsPackage() ? "Source package (default: demo):\n" : "";
        String furtherReading = "";
        if (descriptor.getFurtherReading().isPresent()) {
            furtherReading = "\n> Task :init\n" + descriptor.getFurtherReading().get();
        }
        String toolChain = "";
        if (descriptor.getLanguage() == Language.SWIFT) {
            toolChain = "* An installed Swift compiler. See which link:{userManualPath}/building_swift_projects.html#sec:swift_supported_tool_chain[Swift tool chains] are supported by Gradle.";
        }
        if (descriptor.getLanguage() == Language.CPP) {
            toolChain = "* An installed {cpp} compiler. See which link:{userManualPath}/building_cpp_projects.html#sec:cpp_supported_tool_chain[{cpp} tool chains] are supported by Gradle.";
        }

        projectLayoutSetupRegistry.getTemplateOperationFactory().newTemplateOperation()
            .withTemplate("readme/" + templateFragment + ".adoc.template")
            .withTarget(settings.getTarget().file("../README.adoc").getAsFile())
            .withBinding("language", descriptor.getLanguage().toString())
            .withBinding("languageLC", descriptor.getLanguage().getName().toLowerCase())
            .withBinding("languageIndex", "" + (languages.indexOf(descriptor.getLanguage()) + 1))
            .withBinding("componentType", descriptor.getComponentType().name().toLowerCase())
            .withBinding("componentTypeIndex", "" + (descriptor.getComponentType().ordinal() + 1))
            .withBinding("packageNameChoice", packageNameChoice)
            .withBinding("furtherReading", furtherReading)
            .withBinding("subprojectName", settings.getSubprojects().get(0))
            .withBinding("toolChain", toolChain)
            .withBinding("exampleClass", exampleClass)
            .withBinding("sourceFile", sourceFile)
            .withBinding("testSourceFile", testSourceFile)
            .withBinding("sourceFileTree", sourceFileTree)
            .withBinding("testSourceFileTree", testSourceFileTree)
            .withBinding("testFramework", descriptor.getDefaultTestFramework() == null ? "" : "_" + descriptor.getDefaultTestFramework().toString() + "_")
            .withBinding("buildFileComments", buildFileComments)
            .withBinding("testFrameworkChoice", testFrameworkChoice)

            .create().generate();
    }

    private static void generateOutput(String templateFragment, InitSettings baseSettings, CompositeProjectInitDescriptor descriptor, ProjectLayoutSetupRegistry projectLayoutSetupRegistry) {
        String languageName = descriptor.getLanguage().getName().substring(0, 1).toUpperCase() + descriptor.getLanguage().getName().substring(1);

        String extraCompileJava = "> Task :" + baseSettings.getSubprojects().get(0) + ":compileJava NO-SOURCE\n";
        String extraCompileTestJava = "> Task :" + baseSettings.getSubprojects().get(0) + ":compileTestJava NO-SOURCE\n";
        if (descriptor.getLanguage().toString().equals("Java")) {
            extraCompileJava = "";
            extraCompileTestJava = "";
        }
        String nativeTestTaskPrefix = descriptor.getLanguage() == Language.SWIFT ? "xc" : "run";
        int tasksExecuted = descriptor.getComponentType() == ComponentType.LIBRARY ? 4 : 7;
        if (descriptor.getLanguage() == Language.KOTLIN) {
            tasksExecuted++;
        }
        String classesUpToDate = descriptor.getLanguage() == Language.KOTLIN ? " UP-TO-DATE" : "";
        String inspectClassesForKotlinICTask = descriptor.getLanguage() == Language.KOTLIN ? "> Task :" + baseSettings.getSubprojects().get(0) + ":inspectClassesForKotlinIC\n" : "";

        projectLayoutSetupRegistry.getTemplateOperationFactory().newTemplateOperation()
            .withTemplate("readme/" + templateFragment + "-build.out.template")
            .withTarget(baseSettings.getTarget().file("../tests/build.out").getAsFile())
            .withBinding("language", languageName)
            .withBinding("subprojectName", baseSettings.getSubprojects().get(0))
            .withBinding("extraCompileJava", extraCompileJava)
            .withBinding("extraCompileTestJava", extraCompileTestJava)
            .withBinding("nativeTestTaskPrefix", nativeTestTaskPrefix)
            .withBinding("tasksExecuted", "" + tasksExecuted)
            .withBinding("classesUpToDate", "" + classesUpToDate)
            .withBinding("inspectClassesForKotlinICTask", "" + inspectClassesForKotlinICTask)

            .create().generate();

        projectLayoutSetupRegistry.getTemplateOperationFactory().newTemplateOperation()
            .withTemplate("readme/build.sample.conf")
            .withTarget(baseSettings.getTarget().file("../tests/build.sample.conf").getAsFile())
            .create().generate();
    }
}
