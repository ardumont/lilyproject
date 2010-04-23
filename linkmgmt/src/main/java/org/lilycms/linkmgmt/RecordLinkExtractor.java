package org.lilycms.linkmgmt;

import org.lilycms.repository.api.*;
import org.lilycms.repository.api.exception.RepositoryException;

import java.util.List;
import java.util.Map;

public class RecordLinkExtractor {
    /**
     * Extracts the links from a record. The provided Record object should
     * be "fully loaded" (= contain all fields).
     */
    public static void extract(Record record, LinkCollector collector, TypeManager typeManager) throws RepositoryException {
        for (Map.Entry<QName, Object> field : record.getFields().entrySet()) {
            // TODO once field type ID is available in record, use that to retrieve the field type instead of the name
            FieldType fieldType = typeManager.getFieldTypeByName(field.getKey());
            ValueType valueType = fieldType.getValueType();
            Object value = field.getValue();

            if (valueType.getPrimitive().getName().equals("LINK")) {
                extract(value, collector, fieldType.getId());
            } else if (valueType.getPrimitive().getName().equals("BLOB")) {
                // TODO implement link extraction from blob fields
            }
        }
    }

    private static void extract(Object value, LinkCollector collector, String fieldTypeId) {
        if (value instanceof List) {
            List list = (List)value;
            for (Object item : list) {
                extract(item, collector, fieldTypeId);
            }
        } else if (value instanceof HierarchyPath) {
            HierarchyPath path = (HierarchyPath)value;
            for (Object item : path.getElements()) {
                extract(item, collector, fieldTypeId);
            }
        } else if (value instanceof RecordId) {
            collector.addLink((RecordId)value, fieldTypeId);
        } else {
            throw new RuntimeException("Encountered an unexpected kind of object from a link field: " + value.getClass().getName());
        }
    }
}