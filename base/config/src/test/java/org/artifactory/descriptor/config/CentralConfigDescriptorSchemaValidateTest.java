package org.artifactory.descriptor.config;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.*;

/**
 * @author Noam Shemesh
 */
public class CentralConfigDescriptorSchemaValidateTest {

    @Test
    public void validateAgainLatestSchema() throws Exception {
        URL schemaUrl = CentralConfigDescriptorImpl.class.getClassLoader().getResource("artifactory.xsd");
        XStream xStream = new XStream();
        xStream.alias("xs:schema", Map.class);
        xStream.alias("xs:complexType", Map.class);
        xStream.registerConverter(new MapEntryConverter());
        Object unmarshal = xStream.fromXML(schemaUrl);
        System.out.println(unmarshal);
    }


    private interface Rule {

    }

    private static class Size implements Rule {
        int min;
        int max;
    }

    private static class IdRef implements Rule {
        String id;
    }

    public static class MapEntryConverter implements Converter {

        @Override
        public boolean canConvert(Class clazz) {
            return AbstractMap.class.isAssignableFrom(clazz);
        }

        @Override
        public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            Map<String, List<Object>> map = new HashMap<>();

            while (reader.hasMoreChildren()) {
                reader.moveDown();

                String key = reader.getNodeName();
                String value = reader.getValue();
                map.computeIfAbsent(key, k -> new LinkedList<>()).add(value);

                reader.moveUp();
            }

            return map;
        }

    }

}
