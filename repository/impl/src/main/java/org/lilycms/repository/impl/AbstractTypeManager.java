package org.lilycms.repository.impl;

import java.util.HashMap;
import java.util.Map;

import org.lilycms.repository.api.FieldType;
import org.lilycms.repository.api.FieldTypeEntry;
import org.lilycms.repository.api.IdGenerator;
import org.lilycms.repository.api.PrimitiveValueType;
import org.lilycms.repository.api.QName;
import org.lilycms.repository.api.RecordType;
import org.lilycms.repository.api.Scope;
import org.lilycms.repository.api.TypeManager;
import org.lilycms.repository.api.ValueType;
import org.lilycms.repository.impl.primitivevaluetype.BlobValueType;
import org.lilycms.repository.impl.primitivevaluetype.BooleanValueType;
import org.lilycms.repository.impl.primitivevaluetype.DateValueType;
import org.lilycms.repository.impl.primitivevaluetype.IntegerValueType;
import org.lilycms.repository.impl.primitivevaluetype.LinkValueType;
import org.lilycms.repository.impl.primitivevaluetype.LongValueType;
import org.lilycms.repository.impl.primitivevaluetype.StringValueType;
import org.lilycms.util.ArgumentValidator;

public abstract class AbstractTypeManager implements TypeManager {
	protected Map<String, PrimitiveValueType> primitiveValueTypes = new HashMap<String, PrimitiveValueType>();
	protected IdGenerator idGenerator;

	public RecordType newRecordType(String recordTypeId) {
	    ArgumentValidator.notNull(recordTypeId, "recordTypeId");
	    return new RecordTypeImpl(recordTypeId);
	}

	public FieldTypeEntry newFieldTypeEntry(String fieldTypeId, boolean mandatory) {
	    ArgumentValidator.notNull(fieldTypeId, "fieldTypeId");
	    ArgumentValidator.notNull(mandatory, "mandatory");
	    return new FieldTypeEntryImpl(fieldTypeId, mandatory);
	}

	public FieldType newFieldType(ValueType valueType, QName name, Scope scope) {
	    return newFieldType(null, valueType, name, scope);
	}

	public FieldType newFieldType(String id, ValueType valueType, QName name,
			Scope scope) {
			    ArgumentValidator.notNull(valueType, "valueType");
			    ArgumentValidator.notNull(name, "name");
			    ArgumentValidator.notNull(scope, "scope");
			    return new FieldTypeImpl(id, valueType, name, scope);
			}

	public void registerPrimitiveValueType(PrimitiveValueType primitiveValueType) {
	    primitiveValueTypes.put(primitiveValueType.getName(), primitiveValueType);
	}

	public ValueType getValueType(String primitiveValueTypeName, boolean multivalue, boolean hierarchy) {
	    return new ValueTypeImpl(primitiveValueTypes.get(primitiveValueTypeName), multivalue, hierarchy);
	}
	
	protected void initialize() {
		registerDefaultValueTypes();
	}
	
	// TODO get this from some configuration file
    protected void registerDefaultValueTypes() {
        registerPrimitiveValueType(new StringValueType());
        registerPrimitiveValueType(new IntegerValueType());
        registerPrimitiveValueType(new LongValueType());
        registerPrimitiveValueType(new BooleanValueType());
        registerPrimitiveValueType(new DateValueType());
        registerPrimitiveValueType(new LinkValueType(idGenerator));
        registerPrimitiveValueType(new BlobValueType());
    }
}