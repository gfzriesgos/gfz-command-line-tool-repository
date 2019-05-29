package org.n52.gfz.riesgos.repository.modules;

/*
 * Copyright (C) 2019 GFZ German Research Centre for Geosciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

import org.apache.commons.io.IOUtils;
import org.n52.gfz.riesgos.algorithm.BaseGfzRiesgosService;
import org.n52.gfz.riesgos.algorithm.TransformDataFormatProcess;
import org.n52.gfz.riesgos.configuration.ConfigurationFactory;
import org.n52.gfz.riesgos.configuration.IConfiguration;
import org.n52.gfz.riesgos.configuration.parse.IParseConfiguration;
import org.n52.gfz.riesgos.configuration.parse.json.ParseJsonConfigurationImpl;
import org.n52.gfz.riesgos.exceptions.ParseConfigurationException;
import org.n52.gfz.riesgos.formats.IMimeTypeAndSchemaConstants;
import org.n52.gfz.riesgos.formats.quakeml.binding.QuakeMLXmlDataBinding;
import org.n52.gfz.riesgos.formats.shakemap.binding.ShakemapXmlDataBinding;
import org.n52.gfz.riesgos.functioninterfaces.ICheckDataAndGetErrorMessage;
import org.n52.gfz.riesgos.repository.GfzRiesgosRepository;
import org.n52.gfz.riesgos.validators.XmlBindingWithAllowedSchema;
import org.n52.wps.io.data.IComplexData;
import org.n52.wps.server.IAlgorithm;
import org.n52.wps.server.ProcessDescription;
import org.n52.wps.webapp.api.AlgorithmEntry;
import org.n52.wps.webapp.api.ClassKnowingModule;
import org.n52.wps.webapp.api.ConfigurationCategory;
import org.n52.wps.webapp.api.FormatEntry;
import org.n52.wps.webapp.api.types.ConfigurationEntry;
import org.n52.wps.webapp.api.types.StringConfigurationEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Configuration module for the gfz riesgos repository.
 *
 * The aim is to provide all the information about the process configurations here, so it should be possible to
 * add a process description on runtime and to execute it immediately.
 */
public class GfzRiesgosRepositoryCM extends ClassKnowingModule {

    private static final String CONFIG_KEY = "json_configuration";

    private static final Logger LOGGER = LoggerFactory.getLogger(GfzRiesgosRepositoryCM.class);

    /*
     * This is the default folder that is used to read the json configuration files from
     */
    private static final String DEFAULT_CONFIGURATION_FOLDER =
            "/usr/share/riesgos/json-configurations";

    private static final String MODULE_NAME = "GFZ RIESGOS Configuration Module";
    private static final String CLASS_NAME_OF_REPOSITORY_TO_CONFIG = GfzRiesgosRepository.class.getName();
    private static final ConfigurationCategory CATEGORY = ConfigurationCategory.REPOSITORY;


    private final List<? extends ConfigurationEntry<?>> configurationEntries;
    private final ConfigurationEntry<String> jsonConfigurationFolder;
    private boolean isActive;

    /**
     * Default constructor
     */
    public GfzRiesgosRepositoryCM() {
        isActive = true;
        jsonConfigurationFolder = new StringConfigurationEntry(CONFIG_KEY, "JSON Process Configuration Folder",
                "Folder that contains the json files to add / remove / configure the wps processes that use the " +
                "skeleton to run command line processes in docker.",
                true, DEFAULT_CONFIGURATION_FOLDER);
        configurationEntries = Collections.singletonList(jsonConfigurationFolder);
    }

    @Override
    public String getClassName() {
        return CLASS_NAME_OF_REPOSITORY_TO_CONFIG;
    }

    @Override
    public String getModuleName() {
        return MODULE_NAME;
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public ConfigurationCategory getCategory() {
        return CATEGORY;
    }

    @Override
    public List<? extends ConfigurationEntry<?>> getConfigurationEntries() {
        return configurationEntries;
    }

    @Override
    public List<AlgorithmEntry> getAlgorithmEntries() {
        return parseConfigToAlgorithmEntries().stream().map(AlgorithmData::toAlgorithmEntry).collect(Collectors.toList());
    }

    @Override
    public List<FormatEntry> getFormatEntries() {
        // this is no configuration module for formats
        return null;
    }


    private List<IConfiguration> createPredefinedConfigurations() {
        return Arrays.asList(
                ConfigurationFactory.createQuakeledger(),
                ConfigurationFactory.createShakyground()
        );
    }

    private List<AlgorithmData> parseConfigToAlgorithmEntries() {


        final List<AlgorithmData> result = new ArrayList<>();

        // first, insert all the data format transformation processes
        addAlgorithmsOfFormatTransformations(result::add);

        // then load all the configurations for the custom processes
        // using this approach the predefined services
        // can be overwritten by improved ones on server runtime
        final Map<String, IConfiguration> configurationProcesses = new HashMap<>();

        // step 1: the predefined ones
        for(final IConfiguration predefinedConfig : createPredefinedConfigurations()) {
            configurationProcesses.put(
                    predefinedConfig.getIdentifier(),
                    predefinedConfig
            );
        }

        // others can be added by using the folder
        addConfigurationsFromFolder(this::getFileNamesFromConfig, configurationProcesses::put);

        // than add all to the result
        configurationProcesses.values().stream()
                .map(this::configurationToAlgorithm)
                .forEach(result::add);


        return result;
    }

    private void addAlgorithmsOfFormatTransformations(final Consumer<AlgorithmData> adder) {
        for(ClassTransformationProcess transformationProcess : Arrays.asList(
                new ClassTransformationProcess(QuakeMLXmlDataBinding.class, "QuakeMLTransformationProcess", new XmlBindingWithAllowedSchema(IMimeTypeAndSchemaConstants.SCHEMA_QUAKE_ML)),
                new ClassTransformationProcess(ShakemapXmlDataBinding.class, "ShakemapTransformationProcess", new XmlBindingWithAllowedSchema(IMimeTypeAndSchemaConstants.SCHEMA_SHAKEMAP))
        )) {
            final String processName = transformationProcess.getProcessName();
            final AlgorithmData algorithmData = new AlgorithmData(
                    processName,
                    new TransformDataFormatProcess(
                            processName,
                            transformationProcess.getClazz(),
                            LoggerFactory.getLogger(processName),
                            transformationProcess.getValidator()));
            adder.accept(algorithmData);
        }
    }

    private AlgorithmData configurationToAlgorithm(final IConfiguration configuration) {
        return new AlgorithmData(configuration.getIdentifier(),
                new BaseGfzRiesgosService(configuration,
                        LoggerFactory.getLogger(configuration.getFullQualifiedIdentifier())));
    }

    private Collection<String> getFileNamesFromConfig() {
        return getFileNamesFrom(new File(jsonConfigurationFolder.getValue()), this::filterJsonFiles);
    }

    private boolean filterJsonFiles(final File file) {
        return file.isFile() && file.getAbsolutePath().toLowerCase().endsWith(".json");
    }

    private Collection<String> getFileNamesFrom(final File folder, final FileFilter fileFilter) {
        if(! folder.exists()) {
            LOGGER.error("The folder '" + folder.getAbsolutePath() + "' does not exist.");
            return Collections.emptyList();
        } else if(! folder.isDirectory()) {
            LOGGER.error("The file '" + folder.getAbsolutePath() + "' is not a folder.");
            return Collections.emptyList();
        }

        final File[] files = folder.listFiles(fileFilter);
        final File[] filesNotNull = Optional.ofNullable(files).orElseGet(this::emptyFileArray);

        return Stream.of(filesNotNull).map(File::getAbsolutePath).collect(Collectors.toList());
    }

    private File[] emptyFileArray() {
        return new File[]{};
    }


    private void addConfigurationsFromFolder(final Supplier<Collection<String>> fileProvider, BiConsumer<String, IConfiguration> adder) {
        final IParseConfiguration parser = new ParseJsonConfigurationImpl();

        for(final String fileName : fileProvider.get()) {
            try(final FileInputStream inputStream = new FileInputStream(fileName)) {
                final String content = new String(IOUtils.toByteArray(inputStream));
                final IConfiguration configuration = parser.parse(content);

                adder.accept(configuration.getIdentifier(), configuration);

            } catch(final IOException ioException) {
                LOGGER.error("Can't read the content from file '" + fileName + "': " + ioException);
            } catch(final ParseConfigurationException parseConfigException) {
                LOGGER.error("Can't parse the content of file '" + fileName + "': " + parseConfigException);
            }
        }
    }

    /**
     *
     * @return Set with all the algorithm names
     */
    public Set<String> getAlgorithmNames() {
        return getAlgorithmEntries().stream().map(AlgorithmEntry::getAlgorithm).collect(Collectors.toSet());
    }

    /**
     * Lookup of the algorithms
     *
     * This just works with the algorithms that are generated by using the configuration
     *
     * @param processIdentifier identifier to access the algorithm
     * @return IAlgorithm
     */
    public IAlgorithm getAlgorithm(final String processIdentifier) {
        final Map<String, AlgorithmData> map = parseConfigToAlgorithmEntries().stream().collect(Collectors.toMap(AlgorithmData::getAlgorithmName, Function.identity()));
        if(map.containsKey(processIdentifier)) {
            return map.get(processIdentifier).getAlgorithm();
        }
        return null;
    }

    /**
     * Lookup of the process description
     *
     * This just works with the algorithms that are generated by using the configuration
     * @param processIdentifier identifier to access the algorithm
     * @return ProcessDescription
     */
    public ProcessDescription getProcessDescription(final String processIdentifier) {
        final Map<String, AlgorithmData> map = parseConfigToAlgorithmEntries().stream().collect(Collectors.toMap(AlgorithmData::getAlgorithmName, Function.identity()));
        if(map.containsKey(processIdentifier)) {
            return map.get(processIdentifier).getAlgorithm().getDescription();
        }
        return null;
    }


    /*
     * Inner class to access the name and the algorithm.
     * All the generated algorithms are active.
     */
    private static class AlgorithmData {
        private final String algorithmName;
        private final IAlgorithm algorithm;

        AlgorithmData(final String algorithmName, final IAlgorithm algorithm) {
            // strConfiguration is at the moment only the Name of the class
            this.algorithmName = algorithmName;
            this.algorithm = algorithm;
        }

        AlgorithmEntry toAlgorithmEntry() {
            return new AlgorithmEntry(algorithmName, true);
        }

        String getAlgorithmName() {
            return algorithmName;
        }

        IAlgorithm getAlgorithm() {
            return algorithm;
        }
    }

    private static class ClassTransformationProcess {
        private final Class<? extends IComplexData> clazz;
        private final String processName;
        private final ICheckDataAndGetErrorMessage validator;

        ClassTransformationProcess(
                final Class<? extends IComplexData> clazz,
                final String processName,
                final ICheckDataAndGetErrorMessage validator) {
            this.clazz = clazz;
            this.processName = processName;
            this.validator = validator;
        }

        Class<? extends IComplexData> getClazz() {
            return clazz;
        }

        String getProcessName() {
            return processName;
        }

        ICheckDataAndGetErrorMessage getValidator() {
            return validator;
        }
    }
}
