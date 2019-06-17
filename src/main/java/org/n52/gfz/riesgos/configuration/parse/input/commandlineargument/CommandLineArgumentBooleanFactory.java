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

package org.n52.gfz.riesgos.configuration.parse.input.commandlineargument;

import org.n52.gfz.riesgos.configuration.IInputParameter;
import org.n52.gfz.riesgos.configuration.InputParameterFactory;
import org.n52.gfz.riesgos.configuration.parse.ParseUtils;
import org.n52.gfz.riesgos.exceptions.ParseConfigurationException;

import java.util.List;

/**
 * Implementation to create a command line argument with boolean.
 */
public class CommandLineArgumentBooleanFactory
        implements IAsCommandLineArgumentFactory {

    /**
     * Checks some attributes and delegates the creation.
     * @param identifier identifier of the data
     * @param isOptional true if the input is optional
     * @param optionalAbstract optional description of the parameter
     * @param defaultCommandLineFlag optional default command line flag
     * @param defaultValue optional default value
     * @param allowedValues optional list with allowed values
     * @param supportedCrs optional list with supported crs
     * @param schema optional schema
     * @return input parameter
     * @throws ParseConfigurationException exception that may be thrown
     * if a argument is used that is not supported for this type.
     */
    @Override
    public IInputParameter create(
            final String identifier,
            final boolean isOptional,
            final String optionalAbstract,
            final String defaultCommandLineFlag,
            final String defaultValue,
            final List<String> allowedValues,
            final List<String> supportedCrs,
            final String schema) throws ParseConfigurationException {
        if (ParseUtils.listHasValue(supportedCrs)) {
            throw new ParseConfigurationException(
                    "crs are not supported for boolean types");
        }
        if (ParseUtils.listHasValue(allowedValues)) {
            throw new ParseConfigurationException(
                    "allowed values are not supported for booleans");
        }
        if (ParseUtils.strHasValue(schema)) {
            throw new ParseConfigurationException(
                    "schema is not supported for booleans");
        }
        if (ParseUtils.strHasNoValue(defaultCommandLineFlag)) {
            throw new ParseConfigurationException(
                    "flag is necessary for boolean type");
        }
        return InputParameterFactory.INSTANCE.createCommandLineArgumentBoolean(
                identifier,
                isOptional,
                optionalAbstract,
                defaultCommandLineFlag,
                defaultValue
        );
    }
}
