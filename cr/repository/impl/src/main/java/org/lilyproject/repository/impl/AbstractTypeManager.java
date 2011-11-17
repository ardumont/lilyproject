/*
 * Copyright 2010 Outerthought bvba
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lilyproject.repository.impl;

import java.util.*;

import org.apache.commons.logging.Log;
import org.lilyproject.repository.api.*;
import org.lilyproject.repository.impl.valuetype.*;
import org.lilyproject.util.ArgumentValidator;
import org.lilyproject.util.zookeeper.ZooKeeperItf;

public abstract class AbstractTypeManager implements TypeManager {
    protected Log log;

    protected Map<String, ValueTypeFactory> valueTypeFactories = new HashMap<String, ValueTypeFactory>();
    protected IdGenerator idGenerator;
    
    protected ZooKeeperItf zooKeeper;

    protected SchemaCache schemaCache;
    
    public AbstractTypeManager(ZooKeeperItf zooKeeper) {
        this.zooKeeper = zooKeeper;
    }
    
    @Override
    public FieldTypes getFieldTypesSnapshot() throws InterruptedException {
        return schemaCache.getFieldTypesSnapshot();
    }
    
    @Override
    abstract public List<FieldType> getFieldTypesWithoutCache() throws RepositoryException, InterruptedException;
    @Override
    abstract public List<RecordType> getRecordTypesWithoutCache() throws RepositoryException, InterruptedException;
    
    protected void updateFieldTypeCache(FieldType fieldType) throws TypeException, InterruptedException {
        schemaCache.updateFieldType(fieldType);
    }
    
    protected void updateRecordTypeCache(RecordType recordType) throws TypeException, InterruptedException {
        schemaCache.updateRecordType(recordType);
    }
    
    @Override
    public Collection<RecordType> getRecordTypes() throws InterruptedException {
        return schemaCache.getRecordTypes();
    }
    
    @Override
    public List<FieldType> getFieldTypes() throws TypeException, InterruptedException {
        return schemaCache.getFieldTypes();
    }

    protected RecordType getRecordTypeFromCache(QName name) throws InterruptedException {
        return schemaCache.getRecordType(name);
    }

    protected RecordType getRecordTypeFromCache(SchemaId id) {
        return schemaCache.getRecordType(id);
    }
    
    @Override
    public RecordType getRecordTypeById(SchemaId id, Long version) throws RecordTypeNotFoundException, TypeException, RepositoryException, InterruptedException {
        ArgumentValidator.notNull(id, "id");
        RecordType recordType = getRecordTypeFromCache(id);
        if (recordType == null) {
            throw new RecordTypeNotFoundException(id, version);
        }
        // The cache only keeps the latest (known) RecordType
        if (version != null && !version.equals(recordType.getVersion())) {
            recordType = getRecordTypeByIdWithoutCache(id, version);
        }
        if (recordType == null) {
            throw new RecordTypeNotFoundException(id, version);
        }
        return recordType.clone();
    }
    
    @Override
    public RecordType getRecordTypeByName(QName name, Long version) throws RecordTypeNotFoundException, TypeException, RepositoryException, InterruptedException {
        ArgumentValidator.notNull(name, "name");
        RecordType recordType = getRecordTypeFromCache(name);
        if (recordType == null) {
            throw new RecordTypeNotFoundException(name, version);
        }
        // The cache only keeps the latest (known) RecordType
        if (version != null && !version.equals(recordType.getVersion())) {
            recordType = getRecordTypeByIdWithoutCache(recordType.getId(), version);
        }
        if (recordType == null) {
            throw new RecordTypeNotFoundException(name, version);
        }
        return recordType.clone();
    }
    
    abstract protected RecordType getRecordTypeByIdWithoutCache(SchemaId id, Long version) throws RepositoryException, InterruptedException;
    
    @Override
    public FieldType getFieldTypeById(SchemaId id) throws TypeException, InterruptedException {
        return schemaCache.getFieldType(id);
    }
    
    @Override
    public FieldType getFieldTypeByName(QName name) throws InterruptedException, TypeException {
        return schemaCache.getFieldType(name);
    }
    
    //
    // Object creation methods
    //
    @Override
    public RecordType newRecordType(QName name) {
        return new RecordTypeImpl(null, name);
    }
    
    @Override
    public RecordType newRecordType(SchemaId recordTypeId, QName name) {
        return new RecordTypeImpl(recordTypeId, name);
    }

    @Override
    public FieldType newFieldType(ValueType valueType, QName name, Scope scope) {
        return newFieldType(null, valueType, name, scope);
    }

    @Override
    public FieldType newFieldType(String valueType, QName name, Scope scope) throws RepositoryException,
            InterruptedException {
        return newFieldType(null, getValueType(valueType), name, scope);
    }

    @Override
    public FieldTypeEntry newFieldTypeEntry(SchemaId fieldTypeId, boolean mandatory) {
        ArgumentValidator.notNull(fieldTypeId, "fieldTypeId");
        ArgumentValidator.notNull(mandatory, "mandatory");
        return new FieldTypeEntryImpl(fieldTypeId, mandatory);
    }


    @Override
    public FieldType newFieldType(SchemaId id, ValueType valueType, QName name, Scope scope) {
        return new FieldTypeImpl(id, valueType, name, scope);
    }

    @Override
    public RecordTypeBuilder recordTypeBuilder() throws TypeException {
        return new RecordTypeBuilderImpl(this);
    }

    @Override
    public FieldTypeBuilder fieldTypeBuilder() throws TypeException {
        return new FieldTypeBuilderImpl(this);
    }

    //
    // Value types
    //
    @Override
    public void registerValueType(String valueTypeName, ValueTypeFactory valueTypeFactory) {
        valueTypeFactories.put(valueTypeName, valueTypeFactory);
    }

    @Override
    public ValueType getValueType(String valueTypeSpec) throws RepositoryException, InterruptedException {
        ValueType valueType;

        int indexOfParams = valueTypeSpec.indexOf("<");
        if (indexOfParams == -1) {
            valueType = valueTypeFactories.get(valueTypeSpec).getValueType(null);
        } else {
            if (!valueTypeSpec.endsWith(">")) {
                throw new IllegalArgumentException("Invalid value type string, no closing angle bracket: '" +
                        valueTypeSpec + "'");
            }

            String arg = valueTypeSpec.substring(indexOfParams + 1, valueTypeSpec.length() - 1);

            if (arg.length() == 0) {
                throw new IllegalArgumentException("Invalid value type string, type arg is zero length: '" +
                        valueTypeSpec + "'");
            }

            valueType = valueTypeFactories.get(valueTypeSpec.substring(0, indexOfParams)).getValueType(arg);
        }

        return valueType;
    }
    
    // TODO get this from some configuration file
    protected void registerDefaultValueTypes() {
        //
        // Important:
        //
        // When adding a type below, please update the list of built-in
        // types in the javadoc of the method TypeManager.getValueType.
        //

        // TODO or rather use factories?
        registerValueType(StringValueType.NAME, StringValueType.factory());
        registerValueType(IntegerValueType.NAME, IntegerValueType.factory());
        registerValueType(LongValueType.NAME, LongValueType.factory());
        registerValueType(DoubleValueType.NAME, DoubleValueType.factory());
        registerValueType(DecimalValueType.NAME, DecimalValueType.factory());
        registerValueType(BooleanValueType.NAME, BooleanValueType.factory());
        registerValueType(DateValueType.NAME, DateValueType.factory());
        registerValueType(DateTimeValueType.NAME, DateTimeValueType.factory());
        registerValueType(LinkValueType.NAME, LinkValueType.factory(idGenerator, this));
        registerValueType(BlobValueType.NAME, BlobValueType.factory());
        registerValueType(UriValueType.NAME, UriValueType.factory());
        registerValueType(ListValueType.NAME, ListValueType.factory(this));
        registerValueType(PathValueType.NAME, PathValueType.factory(this));
        registerValueType(RecordValueType.NAME, RecordValueType.factory(this));
    }
}
