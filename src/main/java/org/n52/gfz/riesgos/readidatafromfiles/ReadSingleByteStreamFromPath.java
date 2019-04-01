package org.n52.gfz.riesgos.readidatafromfiles;

/*
 * Copyright (C) 2019 GFZ German Research Centre for Geosciences
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the Licence is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the Licence for the specific language governing permissions and
 *  limitations under the Licence.
 *
 *
 */

import org.n52.gfz.riesgos.cmdexecution.IExecutionContext;
import org.n52.gfz.riesgos.exceptions.ConvertToIDataException;
import org.n52.gfz.riesgos.functioninterfaces.IConvertByteArrayToIData;
import org.n52.gfz.riesgos.functioninterfaces.IReadIDataFromFiles;
import org.n52.wps.io.data.IData;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Implementation that reads a single byte array for one single file.
 * Can be used with every IConvertByteArrayToIData implementation
 */
public class ReadSingleByteStreamFromPath implements IReadIDataFromFiles {

    private final IConvertByteArrayToIData converter;

    /**
     * Implementation that just uses a function to convert the
     * byte[] to IData and just reads from the single given file.
     *
     * Can be used in all situations where one file produces the iData
     */
    public ReadSingleByteStreamFromPath(final IConvertByteArrayToIData converter) {
        this.converter = converter;
    }

    @Override
    public IData readFromFiles(IExecutionContext context, String workingDirectory, String path) throws ConvertToIDataException, IOException {

        final byte[] content = context.readFromFile(Paths.get(workingDirectory, path).toString());
        return converter.convertToIData(content);
    }
}