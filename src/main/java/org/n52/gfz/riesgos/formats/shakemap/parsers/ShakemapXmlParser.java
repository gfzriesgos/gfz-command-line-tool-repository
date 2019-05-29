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

package org.n52.gfz.riesgos.formats.shakemap.parsers;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.n52.gfz.riesgos.formats.IMimeTypeAndSchemaConstants;
import org.n52.gfz.riesgos.formats.shakemap.binding.ShakemapXmlDataBinding;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.datahandler.parser.AbstractParser;
import org.n52.wps.webapp.api.FormatEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Xml Parser for shakemaps.
 * This is the default format.
 */
public class ShakemapXmlParser extends AbstractParser implements IMimeTypeAndSchemaConstants {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShakemapXmlParser.class);

    public ShakemapXmlParser() {
        super();

        supportedIDataTypes.add(ShakemapXmlDataBinding.class);
        supportedFormats.add(MIME_TYPE_XML);
        supportedSchemas.add(SCHEMA_SHAKEMAP);
        supportedEncodings.add(DEFAULT_ENCODING);
        formats.add(new FormatEntry(MIME_TYPE_XML, SCHEMA_SHAKEMAP, DEFAULT_ENCODING, true));
    }

    @Override
    public IData parse(final InputStream stream, final String mimeType, final String schema) {
        try {
            final XmlObject xmlObject = XmlObject.Factory.parse(stream);
            return ShakemapXmlDataBinding.fromXml(xmlObject);
        } catch(final XmlException xmlException) {
            LOGGER.error("Can't parse the provided xml because of a XMLException");
            LOGGER.error(xmlException.toString());
            throw new RuntimeException(xmlException);
        } catch(final IOException ioException) {
            LOGGER.error("Can't parse the provided xml because of an IOException");
            LOGGER.error(ioException.toString());
            throw new RuntimeException(ioException);
        }
    }
}