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
package org.lilyproject.repository.api;

import java.util.Map;
import java.util.UUID;

import org.lilyproject.bytes.api.DataInput;

/**
 * IdGenerator is the factory to create {@link RecordId}s.
 */
public interface IdGenerator {

    /**
     * Creates a new record id containing a generated unique ID.
     */
    RecordId newRecordId();

    /**
     * Creates a new {@link RecordId} containing a master RecordId and variant properties.
     * This {@link RecordId} is to be used for variant records.
     */
    RecordId newRecordId(RecordId masterRecordId, Map<String, String> variantProperties);

    /**
     * Creates a new record id containing a new generated master record id and the given variant
     * properties.
     *
     * <p>This is a shortcut for IdGenerator.newRecordId(IdGenerator.newRecordId(), variantProperties).
     */
    RecordId newRecordId(Map<String, String> variantProperties);

    /**
     * Creates a new record id based on a string provided by the user.
     *
     * <p>If this record id will be used to create a new record, it is the user's responsibility to assure the
     * uniqueness of the ID.
     */
    RecordId newRecordId(String userProvidedId);

    /**
     * Creates a new record id based on a string provided by the user, and with the given
     * variant properties.
     *
     * <p>This is a shortcut for IdGenerator.newRecordId(IdGenerator.newRecordId(userProvidedId), variantProperties).
     */
    RecordId newRecordId(String userProvidedId, Map<String, String> variantProperties);

    /**
     * Creates a RecordId based on the provided byte array.
     *
     * @param bytes well-formed byte representation of the {@link RecordId}, this should have been generated by
     *              calling {@link RecordId#toBytes()}
     */
    RecordId fromBytes(byte[] bytes);

    /**
     * Creates a RecordId based on the provided DataInput.
     *
     * @param dataInput DataInput based on the well-formed byte representation of the {@link RecordId}
     */
    RecordId fromBytes(DataInput dataInput);

    /**
     * Creates a RecordId based on the provided String.
     *
     * <p>The format of the string is described at {@link RecordId#toString}. The parsing is however a
     * bit more lenient: it is not required that the variant properties are specified in lexicographic order,
     * and whitespace around the individual components will be stripped.
     *
     * @param recordIdString well-formed String representation of the {@link RecordId}, as is generated
     *                       by calling {@link RecordId#toString()}
     */
    RecordId fromString(String recordIdString);

    /**
     * Creates a SchemaId based on the provided byte[] representation of the id.
     * @return a SchemaId
     */
    SchemaId getSchemaId(byte[] id);
    
    /**
     * Creates a SchemaId based on the provided String representation of the id.
     * @return a SchemaId
     */
    SchemaId getSchemaId(String id);
    
    /**
     * Creates a SchemaId based on the provided UUID representation of the id.
     * 
     * <p>Important : Should only be used in test cases.
     *  
     * @return a SchemaId
     */
    SchemaId getSchemaId(UUID id);
}