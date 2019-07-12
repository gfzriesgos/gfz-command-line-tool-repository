package org.n52.gfz.riesgos.algorithm;

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

import net.opengis.wps.x100.ProcessDescriptionsDocument;
import org.apache.commons.io.IOUtils;
import org.n52.gfz.riesgos.cache.ICacher;
import org.n52.gfz.riesgos.cache.IDataRecreator;
import org.n52.gfz.riesgos.cache.RecreateFromByteArray;
import org.n52.gfz.riesgos.cache.RecreateFromExitValue;
import org.n52.gfz.riesgos.cache.hash.IHasher;
import org.n52.gfz.riesgos.cmdexecution.IExecutionContext;
import org.n52.gfz.riesgos.cmdexecution.IExecutionContextManager;
import org.n52.gfz.riesgos.cmdexecution.IExecutionRun;
import org.n52.gfz.riesgos.cmdexecution.IExecutionRunResult;
import org.n52.gfz.riesgos.cmdexecution.util.IExecutionContextManagerFactory;
import org.n52.gfz.riesgos.configuration.IConfiguration;
import org.n52.gfz.riesgos.configuration.IInputParameter;
import org.n52.gfz.riesgos.configuration.IOutputParameter;
import org.n52.gfz.riesgos.exceptions.ConvertToBytesException;
import org.n52.gfz.riesgos.exceptions.ConvertToIDataException;
import org.n52.gfz.riesgos.exceptions.ConvertToStringCmdException;
import org.n52.gfz.riesgos.exceptions.NonEmptyStderrException;
import org.n52.gfz.riesgos.exceptions.NonZeroExitValueException;
import org.n52.gfz.riesgos.functioninterfaces.ICheckDataAndGetErrorMessage;
import org.n52.gfz.riesgos.functioninterfaces.IConvertByteArrayToIData;
import org.n52.gfz.riesgos.functioninterfaces.IConvertIDataToByteArray;
import org.n52.gfz.riesgos.functioninterfaces.IConvertIDataToCommandLineParameter;
import org.n52.gfz.riesgos.functioninterfaces.IExitValueHandler;
import org.n52.gfz.riesgos.functioninterfaces.IConvertExitValueToIData;
import org.n52.gfz.riesgos.functioninterfaces.IReadIDataFromFiles;
import org.n52.gfz.riesgos.functioninterfaces.IStderrHandler;
import org.n52.gfz.riesgos.functioninterfaces.IStdoutHandler;
import org.n52.gfz.riesgos.functioninterfaces.IWriteIDataToFiles;
import org.n52.gfz.riesgos.processdescription.IProcessDescriptionGenerator;
import org.n52.gfz.riesgos.processdescription.IProcessDescriptionGeneratorOutputData;
import org.n52.gfz.riesgos.processdescription.impl.ProcessDescriptionGeneratorDataConfigImpl;
import org.n52.gfz.riesgos.processdescription.impl.ProcessDescriptionGeneratorImpl;
import org.n52.gfz.riesgos.util.Tuple;
import org.n52.wps.io.data.IData;
import org.n52.wps.server.AbstractSelfDescribingAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.ProcessDescription;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Base class that can be used for an more
 * configuration like approach for a branch of command line
 * applications that run inside of docker.
 *
 * The processes should be created by creating an instance of this class.
 */
public class BaseGfzRiesgosService extends AbstractSelfDescribingAlgorithm implements ICachableProcess {

    private final IHasher hasher;
    private final ICacher cache;
    private final IExecutionContextManagerFactory executionContextFactory;

    private final IConfiguration configuration;
    private final Logger logger;
    private final List<IInputParameter> inputIdentifiers;
    private final List<IOutputParameter> outputIdentifiers;
    private final Map<String, Class<?>> mapInputDataTypes;
    private final Map<String, Class<?>> mapOutputDataTypes;


    /**
     * Constructor that only gets a configuration and a logger
     * @param configuration configuration to use for the executable
     * @param logger logger to log some messages
     * @param cache implementation of the cache
     */
    public BaseGfzRiesgosService(
            final IConfiguration configuration,
            final Logger logger,
            final IHasher hasher,
            final ICacher cache,
            final IExecutionContextManagerFactory executionContextFactory) {

        this.hasher = hasher;
        this.cache = cache;
        this.executionContextFactory = executionContextFactory;

        this.configuration = configuration;
        this.logger = logger;

        this.inputIdentifiers = configuration.getInputIdentifiers();
        this.outputIdentifiers = configuration.getOutputIdentifiers();
        this.mapInputDataTypes = extractMapInputDataTypes(configuration);
        this.mapOutputDataTypes = extractMapOutputDataTypes(configuration);
    }

    public ICacher getCache() {
        return cache;
    }

    public List<IProcessDescriptionGeneratorOutputData> getOutputDataForProcessGeneration() {
        return new ProcessDescriptionGeneratorDataConfigImpl(configuration).getOutputData();
    }


    /*
     * transforms the input data to a map to lookup the types in a predefined fast way
     */
    private Map<String, Class<?>> extractMapInputDataTypes(final IConfiguration configuration) {
        return configuration
                .getInputIdentifiers()
                .stream()
                .collect(Collectors.toMap(IInputParameter::getIdentifier, IInputParameter::getBindingClass));
    }

    /*
     * transforms the output data to a map to lookup the types in a predefined fast way
     */
    private Map<String, Class<?>> extractMapOutputDataTypes(final IConfiguration configuration) {
        return configuration
                .getOutputIdentifiers()
                .stream()
                .collect(Collectors.toMap(IOutputParameter::getIdentifier, IOutputParameter::getBindingClass));
    }

    /**
     *
     * @return List with the names of the input identifiers
     */
    @Override
    public List<String> getInputIdentifiers() {
        return inputIdentifiers.stream().map(IInputParameter::getIdentifier).collect(Collectors.toList());
    }

    /**
     *
     * @return List with the names of the output identifiers
     */
    @Override
    public List<String> getOutputIdentifiers() {
        return outputIdentifiers.stream().map(IOutputParameter::getIdentifier).collect(Collectors.toList());
    }

    /**
     * method for all the work of the algorithm
     * extracts the input data
     * runs the executable
     * returns the output data
     * @param inputDataFromMethod input data from the wps service
     * @return Map with IData as results
     * @throws ExceptionReport maybe a ExceptionReport is thrown to handle errors in the service
     */
    @Override
    public Map<String, IData> run(final Map<String, List<IData>> inputDataFromMethod) throws ExceptionReport {

        final String hash = hasher.hash(configuration, inputDataFromMethod);

        logger.info("Cache-Hash: " + hash);

        final Optional<Map<String, IDataRecreator>> cachedResult = cache.getCachedResult(hash);

        if(cachedResult.isPresent()) {
            logger.info("Read the results from cache");

            return cachedResult.get().entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().recreate()
            ));
        }

        logger.info("There is no result in the cache");

        final InnerRunContext innerRunContext = new InnerRunContext(inputDataFromMethod);
        final Map<String, Tuple<IData, IDataRecreator>> innerResult = innerRunContext.run();

        final Map<String, IDataRecreator> dataToStoreInCache = innerResult.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().getSecond()
        ));

        cache.insertResultIntoCache(hash, dataToStoreInCache);

        return innerResult.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().getFirst()
        ));
    }

    /**
     * Lookup for the binding class of the input data
     * @param id identifier of the input dataset
     * @return binding class (for example GenericXMLBinding or LiteralStringBinding)
     */
    @Override
    public Class<?> getInputDataType(final String id) {
        if(mapInputDataTypes.containsKey(id)) {
            return mapInputDataTypes.get(id);
        }
        return null;
    }

    /**
     * Lookup for the binding class for the output data
     * @param id identifier of the output dataset
     * @return binding class (for example GenericXMLBinding or LiteralStringBinding)
     */
    @Override
    public Class<?> getOutputDataType(final String id) {
        if(mapOutputDataTypes.containsKey(id)) {
            return mapOutputDataTypes.get(id);
        }
        return null;
    }

    /**
     * Generates the process description by using the configuration
     * @return ProcessDescription of the process (xml)
     */
    @Override
    public ProcessDescription getDescription() {

        final IProcessDescriptionGenerator generator = new ProcessDescriptionGeneratorImpl(
                new ProcessDescriptionGeneratorDataConfigImpl(configuration));
        final ProcessDescriptionsDocument description = generator.generateProcessDescription();
        ProcessDescription processDescription = new ProcessDescription();
        processDescription.addProcessDescriptionForVersion(description.getProcessDescriptions().getProcessDescriptionArray(0), "1.0.0");
        return processDescription;
    }

    /**
     * InnerRunContext
     * here are the Maps with the input and output data for each run
     */
    private class InnerRunContext {

        private final Map<String, IData> inputData;
        private final Map<String, Tuple<IData, IDataRecreator>> outputData;

        /**
         * Constructor with the original input data
         * @param originalInputData Map with input data from the service
         * @throws ExceptionReport maybe a ExceptionReport is thrown to handle the errors in the process
         */
        private InnerRunContext(final Map<String, List<IData>> originalInputData) throws ExceptionReport {
            this.inputData = getInputFields(originalInputData);
            this.outputData = new HashMap<>();
        }

        /**
         * Set the input fields and runs the script
         * @return Map with IData as the results
         * @throws ExceptionReport maybe a ExceptionReport is thrown to handle the errors in the process
         */
        private Map<String, Tuple<IData, IDataRecreator>> run() throws ExceptionReport {
            logger.debug("Start run");
            runExecutable();
            return outputData;
        }

        /*
         * extracts the input fields
         */
        private Map<String, IData> getInputFields(final Map<String, List<IData>> originalInputData) throws ExceptionReport {

            logger.debug("Start getInputFields");
            final Map<String, IData> result = new HashMap<>();

            for(final IInputParameter inputValue : inputIdentifiers) {
                final String identifier = inputValue.getIdentifier();
                if(! originalInputData.containsKey(identifier)) {
                    if(inputValue.isOptional()) {
                        continue;
                    } else {
                        throw new ExceptionReport("There is no data for the identifier '" + identifier + "'", ExceptionReport.MISSING_PARAMETER_VALUE);
                    }
                }
                final List<IData> list = originalInputData.get(inputValue.getIdentifier());
                if(list.isEmpty()) {
                    if(inputValue.isOptional()) {
                        // if the value is optional it is fine if there is none
                        continue;
                    } else {
                        throw new ExceptionReport("There is just an empty list for the identifier '" + identifier + "'", ExceptionReport.MISSING_PARAMETER_VALUE);
                    }
                }
                final IData firstElement = list.get(0);

                final Class<? extends IData> bindingClass = inputValue.getBindingClass();
                if(! bindingClass.isInstance(firstElement)) {
                    throw new ExceptionReport("There is not the expected Binding class for the identifier '" + identifier + "'", ExceptionReport.INVALID_PARAMETER_VALUE);
                }
                final Optional<ICheckDataAndGetErrorMessage> optionalValidator = inputValue.getValidator();
                if(optionalValidator.isPresent()) {
                    final ICheckDataAndGetErrorMessage validator = optionalValidator.get();
                    @SuppressWarnings("unchecked")
                    final Optional<String> errorMessage = validator.check(firstElement);
                    if(errorMessage.isPresent()) {
                        throw new ExceptionReport("There is invalid input for the identifier '" + identifier + "'. " + errorMessage.get(), ExceptionReport.INVALID_PARAMETER_VALUE);
                    }
                }

                result.put(identifier, firstElement);
            }

            return result;
        }

        /*
         * runs the executable inside a container
         */
        private void runExecutable() throws ExceptionReport {

            final String workingDirectory = configuration.getWorkingDirectory();
            final List<String> cmd = createCommandToExecute();

            logger.debug("List with cmd-arguments: " + cmd);

            final IExecutionContextManager contextManager = executionContextFactory.createExecutionContext(configuration);

            try(final IExecutionContext context = contextManager.createExecutionContext(workingDirectory, cmd)) {
                logger.debug("Context container created");
                runExecutableInContext(context);
            }
            logger.debug("Context container removed");
        }

        /*
         * creates a list of the executable and the arguments
         * uses a list and not a single string argument to make things more safe
         */
        private List<String> createCommandToExecute() throws ExceptionReport {
            final List<String> result = new ArrayList<>();
            result.addAll(configuration.getCommandToExecute());

            result.addAll(configuration.getDefaultCommandLineFlags());

            try {
                for (final IInputParameter inputValue : inputIdentifiers) {
                    final Optional<IConvertIDataToCommandLineParameter> functionToTransformToCmd = inputValue.getFunctionToTransformToCmd();
                    if (functionToTransformToCmd.isPresent() && inputData.containsKey(inputValue.getIdentifier())) {
                        @SuppressWarnings("unchecked")
                        final List<String> args = functionToTransformToCmd.get().convertToCommandLineParameter(inputData.get(inputValue.getIdentifier()));
                        result.addAll(args);
                    }
                }
            } catch(final ConvertToStringCmdException exception) {
                throw new ExceptionReport("It is not valid to use the command line arguments", ExceptionReport.INVALID_PARAMETER_VALUE, exception);
            }

            return result;
        }

        /*
         * runs the process and handles input and output
         */
        private void runExecutableInContext(final IExecutionContext context) throws ExceptionReport {
            copyInput(context);
            logger.debug("Files copied into container");

            try {
                final IExecutionRun run = context.run();
                final PrintStream stdinStreamToWrite = run.getStdin();
                logger.debug("Executable started");

                writeToStdin(stdinStreamToWrite);

                stdinStreamToWrite.close();

                try {
                    final IExecutionRunResult result = run.waitForCompletion();

                    logger.debug("Executable finished");

                    handleStderr(result.getStderrResult());
                    handleExitValue(result.getExitValue());
                    handleStdout(result.getStdoutResult());

                    logger.debug("Handling of stderr/exitValue/stdout finished");

                    readFromOutputFiles(context);

                    logger.debug("Getting files out of the container finished");
                } catch(final InterruptedException interruptedException) {
                    throw new ExceptionReport("Can't wait for process termination", ExceptionReport.REMOTE_COMPUTATION_ERROR, interruptedException);
                }
            } catch(final IOException ioException) {
                throw new ExceptionReport("Can't handle input and output", ExceptionReport.REMOTE_COMPUTATION_ERROR, ioException);
            }
        }

        /*
         * copies all the input files into the container
         */
        private void copyInput(final IExecutionContext context) throws ExceptionReport {

            try {
                for (final IInputParameter inputValue : inputIdentifiers) {
                    // if there is no data for that identifier it was optional
                    // so no need to copy the input
                    if(inputData.containsKey(inputValue.getIdentifier())) {
                        final Optional<String> optionalPath = inputValue.getPathToWriteToOrReadFromFile();
                        final Optional<IWriteIDataToFiles> optionalWriteIDataToFiles = inputValue.getFunctionToWriteIDataToFiles();

                        if (optionalPath.isPresent() && optionalWriteIDataToFiles.isPresent()) {
                            final String path = optionalPath.get();
                            final IWriteIDataToFiles writeIDataToFiles = optionalWriteIDataToFiles.get();
                            //noinspection unchecked
                            writeIDataToFiles.writeToFiles(inputData.get(inputValue.getIdentifier()), context,
                                    configuration.getWorkingDirectory(), path);
                        }
                    }
                }
            } catch(final IOException ioException) {
                throw new ExceptionReport("Files could not be copied to the working directory", ExceptionReport.REMOTE_COMPUTATION_ERROR, ioException);
            } catch(final ConvertToBytesException convertToBytesException) {
                throw new ExceptionReport("Data could not be converted to an input file", ExceptionReport.REMOTE_COMPUTATION_ERROR, convertToBytesException);
            }
        }

        /*
         * writes input to the stdin stream
         */
        private void writeToStdin(final PrintStream stdin) throws ExceptionReport {
            try {
                for (final IInputParameter inputValue : inputIdentifiers) {
                    if(inputData.containsKey(inputValue.getIdentifier())) {
                        final Optional<IConvertIDataToByteArray> optionalFunctionToWriteToStdin = inputValue.getFunctionToWriteToStdin();
                        if (optionalFunctionToWriteToStdin.isPresent()) {
                            final IConvertIDataToByteArray functionToWriteToStdin = optionalFunctionToWriteToStdin.get();
                            @SuppressWarnings("unchecked")
                            final byte[] content = functionToWriteToStdin.convertToBytes(inputData.get(inputValue.getIdentifier()));
                            IOUtils.write(content, stdin);
                        }
                    }
                }
            } catch(final IOException exception) {
                throw new ExceptionReport("Can't write to stdin", ExceptionReport.REMOTE_COMPUTATION_ERROR, exception);
            } catch(final ConvertToBytesException convertToBytesException) {
                throw new ExceptionReport("Data could not be converted to an text for stdin", ExceptionReport.REMOTE_COMPUTATION_ERROR, convertToBytesException);
            }
        }

        /*
         * handles the stderr stream output (indicating error, logging, use it as output)
         */
        private void handleStderr(final String stderr) throws ExceptionReport {

            final Optional<IStderrHandler> mainStderrHandler = configuration.getStderrHandler();
            if (mainStderrHandler.isPresent()) {
                try {
                    mainStderrHandler.get().handleStderr(stderr, logger::debug);
                } catch (final NonEmptyStderrException exception) {
                    throw new ExceptionReport("There is an error on stderr", ExceptionReport.REMOTE_COMPUTATION_ERROR, exception);
                }
            }
            try {
                for (final IOutputParameter outputValue : outputIdentifiers) {
                    final Optional<IConvertByteArrayToIData> stderrHandler = outputValue.getFunctionToHandleStderr();

                    try {
                        if (stderrHandler.isPresent()) {
                            final byte[] bytes = stderr.getBytes();
                            final IConvertByteArrayToIData converter = stderrHandler.get();

                            final IData iData = converter.convertToIData(bytes);
                            putIntoOutput(outputValue, iData, new RecreateFromByteArray(bytes, converter, outputValue.getBindingClass()));
                        }
                    } catch (final ConvertToIDataException convertException) {
                        if(outputValue.isOptional()) {
                            logger.info("Can't read from stderr.");
                            logger.info("But since '" + outputValue.getIdentifier() + "' is optional, we can ignore it.");
                        } else {
                            throw convertException;
                        }
                    }
                }
            } catch (final ConvertToIDataException convertException) {
                throw new ExceptionReport("Can't read from stderr", ExceptionReport.REMOTE_COMPUTATION_ERROR, convertException);
            }

        }

        /*
         * insert the data into the output map
         * if there is an validator, then it is used here
         */
        private void putIntoOutput(final IOutputParameter outputValue, final IData iData, final IDataRecreator dataRecreator) throws ExceptionReport {
            final Optional<ICheckDataAndGetErrorMessage> optionalValidator = outputValue.getValidator();
            if(optionalValidator.isPresent()) {
                final ICheckDataAndGetErrorMessage validator = optionalValidator.get();

                @SuppressWarnings("unchecked")
                final Optional<String> optionalErrorMessage = validator.check(iData);
                if(optionalErrorMessage.isPresent()) {
                    final String errorMessage = optionalErrorMessage.get();
                    throw new ExceptionReport("The output for '" + outputValue.getIdentifier() + "' is not valid:\n" + errorMessage, ExceptionReport.REMOTE_COMPUTATION_ERROR);
                }
            }
            outputData.put(outputValue.getIdentifier(), new Tuple<>(iData, dataRecreator));
        }

        /*
         * handles the exit value (indicating error, logging, use as output)
         */
        private void handleExitValue(final int exitValue) throws ExceptionReport {
            final Optional<IExitValueHandler> mainExitValueHandler = configuration.getExitValueHandler();
            if (mainExitValueHandler.isPresent()) {
                try {
                    mainExitValueHandler.get().handleExitValue(exitValue, logger::debug);
                } catch( final NonZeroExitValueException exception){
                    throw new ExceptionReport("There is a non empty exit value", ExceptionReport.REMOTE_COMPUTATION_ERROR, exception);
                }
            }
            try {
                for (final IOutputParameter outputValue : outputIdentifiers) {
                    try {
                        final Optional<IConvertExitValueToIData> exitValueHandler = outputValue.getFunctionToHandleExitValue();
                        if (exitValueHandler.isPresent()) {
                            final IConvertExitValueToIData converter = exitValueHandler.get();
                            final IData iData = converter.convertToIData(exitValue);
                            putIntoOutput(outputValue, iData, new RecreateFromExitValue(exitValue, converter, outputValue.getBindingClass()));
                        }
                    } catch (final ConvertToIDataException convertException) {
                        if(outputValue.isOptional()) {
                            logger.info("Can't read from exit value.");
                            logger.info("But since '" + outputValue.getIdentifier() + "' is optional, we can ignore it.");
                        } else {
                            throw convertException;
                        }
                    }

                }
            } catch(final ConvertToIDataException convertException) {
                throw new ExceptionReport("Can't read from exit value", ExceptionReport.REMOTE_COMPUTATION_ERROR, convertException);
            }
        }

        /*
         * handles stdout stream output (logging, use as output)
         */
        private void handleStdout(final String stdout) throws ExceptionReport {
            final Optional<IStdoutHandler> mainStdoutHandler = configuration.getStdoutHandler();
            mainStdoutHandler.ifPresent(handler -> handler.handleStdout(stdout));

            try {
                for (final IOutputParameter outputValue : outputIdentifiers) {
                    try {
                        final Optional<IConvertByteArrayToIData> stdoutHandler = outputValue.getFunctionToHandleStdout();
                        if (stdoutHandler.isPresent()) {
                            final byte[] bytes = stdout.getBytes();
                            final IConvertByteArrayToIData converter = stdoutHandler.get();
                            final IData iData = converter.convertToIData(bytes);
                            putIntoOutput(outputValue, iData, new RecreateFromByteArray(bytes, converter, outputValue.getBindingClass()));
                        }
                    } catch(final ConvertToIDataException convertException) {
                        if(outputValue.isOptional()) {
                            logger.info("Can't read from stdout.");
                            logger.info("But since '" + outputValue.getIdentifier() + "' is optional, we can ignore it.");
                        } else {
                            throw convertException;
                        }
                    }
                }
            } catch(final ConvertToIDataException convertException) {
                throw new ExceptionReport("Can't read from stdout", ExceptionReport.REMOTE_COMPUTATION_ERROR, convertException);
            }
        }

        /*
         * reads output files from the container
         */
        private void readFromOutputFiles(final IExecutionContext context) throws ExceptionReport {

            try {
                for(final IOutputParameter outputValue : outputIdentifiers) {
                    try {
                        final Optional<String> optionalPath = outputValue.getPathToWriteToOrReadFromFile();
                        final Optional<IReadIDataFromFiles> optionalFunctionToReadFromFiles = outputValue.getFunctionToReadIDataFromFiles();
                        if(optionalPath.isPresent() && optionalFunctionToReadFromFiles.isPresent()) {
                            final String path = optionalPath.get();
                            final IReadIDataFromFiles functionToReadFromFiles = optionalFunctionToReadFromFiles.get();
                            final Tuple<IData, IDataRecreator> readResult = functionToReadFromFiles.readFromFiles(context, configuration.getWorkingDirectory(), path);
                            putIntoOutput(outputValue, readResult.getFirst(), readResult.getSecond());
                        }
                    } catch(final IOException | ConvertToIDataException exception) {
                        if(outputValue.isOptional()) {
                            logger.info("Can't read from output file.");
                            logger.info("But since '" + outputValue.getIdentifier() + "' is optional, we can ignore it.");
                        } else {
                            throw exception;
                        }
                    }

                }
            } catch(final IOException ioException) {
                throw new ExceptionReport("Files could not be read", ExceptionReport.REMOTE_COMPUTATION_ERROR, ioException);
            } catch(final ConvertToIDataException convertException) {
                throw new ExceptionReport("Data could not be converted", ExceptionReport.REMOTE_COMPUTATION_ERROR, convertException);
            }
        }
    }
}
