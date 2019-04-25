package org.n52.gfz.riesgos.formats.quakeml;

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

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.junit.Test;
import org.n52.gfz.riesgos.exceptions.ConvertFormatException;
import org.n52.gfz.riesgos.formats.quakeml.impl.QuakeMLXmlImpl;

import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

/**
 * This is the test class for the xml implementation
 */
public class TestQuakeMLXmlImpl {

    @Test
    public void testWithOneEvent() {
        final String xmlRawContent =
                "<eventParameters namespace=\"http://quakeml.org/xmlns/quakeml/1.2\">\n" +
                        "  <event publicID=\"84945\">\n" +
                        "    <preferredOriginID>84945</preferredOriginID>\n" +
                        "    <preferredMagnitudeID>84945</preferredMagnitudeID>\n" +
                        "    <type>earthquake</type>\n" +
                        "    <description>\n" +
                        "      <text>stochastic</text>\n" +
                        "    </description>\n" +
                        "    <origin publicID=\"84945\">\n" +
                        "      <time>\n" +
                        "        <value>16773-01-01T00:00:00.000000Z</value>\n" +
                        "        <uncertainty>nan</uncertainty>\n" +
                        "      </time>\n" +
                        "      <latitude>\n" +
                        "        <value>-30.9227</value>\n" +
                        "        <uncertainty>nan</uncertainty>\n" +
                        "      </latitude>\n" +
                        "      <longitude>\n" +
                        "        <value>-71.49875</value>\n" +
                        "        <uncertainty>nan</uncertainty>\n" +
                        "      </longitude>\n" +
                        "      <depth>\n" +
                        "        <value>34.75117</value>\n" +
                        "        <uncertainty>nan</uncertainty>\n" +
                        "      </depth>\n" +
                        "      <creationInfo>\n" +
                        "        <value>GFZ</value>\n" +
                        "      </creationInfo>\n" +
                        "    </origin>\n" +
                        "    <originUncertainty>\n" +
                        "      <horizontalUncertainty>nan</horizontalUncertainty>\n" +
                        "      <minHorizontalUncertainty>nan</minHorizontalUncertainty>\n" +
                        "      <maxHorizontalUncertainty>nan</maxHorizontalUncertainty>\n" +
                        "      <azimuthMaxHorizontalUncertainty>nan</azimuthMaxHorizontalUncertainty>\n" +
                        "    </originUncertainty>\n" +
                        "    <magnitude publicID=\"84945\">\n" +
                        "      <mag>\n" +
                        "        <value>8.35</value>\n" +
                        "        <uncertainty>nan</uncertainty>\n" +
                        "      </mag>\n" +
                        "      <type>MW</type>\n" +
                        "      <creationInfo>\n" +
                        "        <value>GFZ</value>\n" +
                        "      </creationInfo>\n" +
                        "    </magnitude>\n" +
                        "    <focalMechanism publicID=\"84945\">\n" +
                        "      <nodalPlanes>\n" +
                        "        <nodalPlane1>\n" +
                        "          <strike>\n" +
                        "            <value>7.310981</value>\n" +
                        "            <uncertainty>nan</uncertainty>\n" +
                        "          </strike>\n" +
                        "          <dip>\n" +
                        "            <value>16.352970000000003</value>\n" +
                        "            <uncertainty>nan</uncertainty>\n" +
                        "          </dip>\n" +
                        "          <rake>\n" +
                        "            <value>90.0</value>\n" +
                        "            <uncertainty>nan</uncertainty>\n" +
                        "          </rake>\n" +
                        "        </nodalPlane1>\n" +
                        "        <preferredPlane>nodalPlane1</preferredPlane>\n" +
                        "      </nodalPlanes>\n" +
                        "    </focalMechanism>\n" +
                        "  </event>\n" +
                        "</eventParameters>";
        try {
            final XmlObject xmlContent = XmlObject.Factory.parse(xmlRawContent);

            final IQuakeML quakeML = QuakeML.fromXml(xmlContent);

            final List<IQuakeMLEvent> events = quakeML.getEvents();

            assertEquals("The list has one element", 1, events.size());

            final IQuakeMLEvent event = events.get(0);

            assertEquals("The publicID is as expected", "84945", event.getPublicID());
            assertEquals("The preferredOriginID is as expected", "84945", event.getPreferredOriginID().get());
            assertEquals("The preferredMagnitudeID is as expected", "84945", event.getPreferredMagnitudeID().get());
            assertEquals("The type is as expected", "earthquake", event.getType().get());
            assertEquals("The descriptionText is as expected", "stochastic", event.getDescription().get());
            assertEquals("The originPublicID is as expected", "84945", event.getOriginPublicID().get());
            assertEquals("The originTimeValue is as expeced", "16773-01-01T00:00:00.000000Z", event.getOriginTimeValue().get());
            assertEquals("The originTimeUncertainty is as expected", "nan", event.getOriginTimeUncertainty().get());
            assertTrue("The latitudeValue is as expected", Math.abs(-30.9227 - event.getOriginLatitudeValue()) < 0.001);
            assertEquals("The latitudeUncertainty is as expected", "nan", event.getOriginLatitudeUncertainty().get());
            assertTrue("The longitudeValue is as expected", Math.abs(-71.49875 - event.getOriginLongitudeValue()) < 0.001);
            assertEquals("The longitudeUncertainty is as expected", "nan", event.getOriginLongitudeUncertainty().get());
            assertEquals("The depthValue is as expected", "34.75117", event.getOriginDepthValue().get());
            assertEquals("The depthUncertainty is as expected", "nan", event.getOriginDepthUncertainty().get());
            assertEquals("The originCreationInfoValue is as expected", "GFZ", event.getOriginCreationInfoValue().get());
            assertEquals("The horizontalUncertainty is as expected", "nan", event.getOriginUncertaintyHorizontalUncertainty().get());
            assertEquals("The minHorizontalUncertainty is as expected", "nan", event.getOriginUncertaintyMinHorizontalUncertainty().get());
            assertEquals("The maxHorizontalUncertainty is as expected", "nan", event.getOriginUncertaintyMaxHorizontalUncertainty().get());
            assertEquals("The azimuthMaxHorizontalUncertainty is as expected", "nan", event.getOriginUncertaintyAzimuthMaxHorizontalUncertainty().get());
            assertEquals("The magnitude publicID is as expected", "84945", event.getMagnitudePublicID().get());
            assertEquals("The magnitude value is as expected", "8.35", event.getMagnitudeMagValue().get());
            assertEquals("The magnitude uncertainty is as expected", "nan", event.getMagnitudeMagUncertainty().get());
            assertEquals("The magnitude type is as expected", "MW", event.getMagnitudeType().get());
            assertEquals("The magnitude creationInfo is as expected", "GFZ", event.getMagnitudeCreationInfoValue().get());
            assertEquals("The focal mechanism publicID is as expected", "84945", event.getFocalMechanismPublicID().get());
            assertEquals("The strike value is as expected", "7.310981", event.getFocalMechanismNodalPlanesNodalPlane1StrikeValue().get());
            assertEquals("The strike uncertainty is as expected", "nan", event.getFocalMechanismNodalPlanesNodalPlane1StrikeUncertainty().get());
            assertEquals("The dip value is as expected", "16.352970000000003", event.getFocalMechanismNodalPlanesNodalPlane1DipValue().get());
            assertEquals("The dip uncertainty is as expected", "nan", event.getFocalMechanismNodalPlanesNodalPlane1DipUncertainty().get());
            assertEquals("The rake value is as expected", "90.0", event.getFocalMechanismNodalPlanesNodalPlane1RakeValue().get());
            assertEquals("The rake uncertainty is as expected", "nan", event.getFocalMechanismNodalPlanesNodalPlane1RakeUncertainty().get());
            assertEquals("The preferred plane is as expected", "nodalPlane1", event.getFocalMechanismNodalPlanesPreferredNodalPlane().get());

        } catch(final XmlException exception) {
            fail("There should be no xml exception");
        } catch(final ConvertFormatException exception) {
            fail("There should be no exception on converting");
        }
    }
}