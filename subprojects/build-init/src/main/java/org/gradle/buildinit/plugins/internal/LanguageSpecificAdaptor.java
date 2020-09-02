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

import org.gradle.buildinit.plugins.internal.modifiers.BuildInitDsl;
import org.gradle.buildinit.plugins.internal.modifiers.BuildInitTestFramework;
import org.gradle.buildinit.plugins.internal.modifiers.ComponentType;
import org.gradle.buildinit.plugins.internal.modifiers.Language;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class LanguageSpecificAdaptor implements ProjectGenerator {
    private final BuildScriptBuilderFactory scriptBuilderFactory;
    private final TemplateOperationFactory templateOperationFactory;
    private final LanguageSpecificProjectGenerator descriptor;

    public LanguageSpecificAdaptor(LanguageSpecificProjectGenerator descriptor, BuildScriptBuilderFactory scriptBuilderFactory, TemplateOperationFactory templateOperationFactory) {
        this.scriptBuilderFactory = scriptBuilderFactory;
        this.descriptor = descriptor;
        this.templateOperationFactory = templateOperationFactory;
    }

    @Override
    public String getId() {
        return descriptor.getId();
    }

    @Override
    public ComponentType getComponentType() {
        return descriptor.getComponentType();
    }

    @Override
    public Language getLanguage() {
        return descriptor.getLanguage();
    }

    @Override
    public boolean supportsModularization() {
        return descriptor.supportsModularization();
    }

    @Override
    public Optional<String> getFurtherReading() {
        return descriptor.getFurtherReading();
    }

    @Override
    public BuildInitDsl getDefaultDsl() {
        if (descriptor.getLanguage().equals(Language.KOTLIN)) {
            return BuildInitDsl.KOTLIN;
        }
        return BuildInitDsl.GROOVY;
    }

    @Override
    public Set<BuildInitTestFramework> getTestFrameworks() {
        return descriptor.getTestFrameworks();
    }

    @Override
    public BuildInitTestFramework getDefaultTestFramework() {
        return descriptor.getDefaultTestFramework();
    }

    @Override
    public boolean supportsPackage() {
        return descriptor.supportsPackage();
    }

    public Map<String, List<String>> generateWithExternalComments(InitSettings settings) {
        HashMap<String, List<String>> comments = new HashMap<>();
        for(BuildScriptBuilder buildScriptBuilder : allBuildScriptBuilder(settings)) {
            buildScriptBuilder.withExternalComments().create(settings.getTarget()).generate();
            comments.put(buildScriptBuilder.getFileNameWithoutExtension(), buildScriptBuilder.extractComments());
        }
        return comments;
    }

    @Override
    public void generate(InitSettings settings) {
        for(BuildScriptBuilder buildScriptBuilder : allBuildScriptBuilder(settings)) {
            buildScriptBuilder.create(settings.getTarget()).generate();
        }
    }

    private List<BuildScriptBuilder> allBuildScriptBuilder(InitSettings settings) {
        List<BuildScriptBuilder> operations = new ArrayList<>();
        if (settings.isModularized()) {
            generateBuildSrcSetup(settings);

            operations.add(conventionPluginScriptBuilder("common", settings));
            operations.add(conventionPluginScriptBuilder("application", settings));
            operations.add(conventionPluginScriptBuilder("library", settings));
        }
        for (String subproject : settings.getSubprojects()) {
            operations.add(buildScriptBuilder(subproject, settings, subproject + "/build"));
        }

        TemplateFactory templateFactory = new TemplateFactory(settings, descriptor.getLanguage(), templateOperationFactory);
        descriptor.generateSources(settings, templateFactory);

        return operations;
    }

    private void generateBuildSrcSetup(InitSettings settings) {
        BuildScriptBuilder buildSrcScriptBuilder = scriptBuilderFactory.script(settings.getDsl(), "buildSrc/build");
        buildSrcScriptBuilder.conventionPluginSupport("Support convention plugins written in " + settings.getDsl().toString() + ". Convention plugins are build scripts in 'src/main' that automatically become available as plugins in the main build.");
        buildSrcScriptBuilder.create(settings.getTarget()).generate();
    }

    private BuildScriptBuilder conventionPluginScriptBuilder(String convention, InitSettings settings) {
        return buildScriptBuilder(convention, settings,
            "buildSrc/src/main/" + settings.getDsl().name().toLowerCase() + "/" + settings.getPackageName() + "." + getLanguage().getName() + "-" + convention + "-conventions");
    }

    private BuildScriptBuilder buildScriptBuilder(String projectOrConventionName, InitSettings settings, String buildFile) {
        BuildScriptBuilder buildScriptBuilder = scriptBuilderFactory.script(settings.getDsl(), buildFile);
        descriptor.generateBuildScript(projectOrConventionName, settings, buildScriptBuilder);
        return buildScriptBuilder;
    }
}
