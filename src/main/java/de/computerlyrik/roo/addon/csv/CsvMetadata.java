package de.computerlyrik.roo.addon.csv;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.ItdTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.uaa.client.util.Assert;

/**
 * This type produces metadata for a new ITD. It uses an {@link ItdTypeDetailsBuilder} provided by 
 * {@link AbstractItdTypeDetailsProvidingMetadataItem} to register a field in the ITD and a new method.
 * 
 * @since 1.1.0
 */
public class CsvMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

	//Logger
	private static Logger logger = HandlerUtils.getLogger(CsvMetadata.class);
    // Constants
    private static final String PROVIDES_TYPE_STRING = CsvMetadata.class.getName();
    private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

	private static final JavaType ARRAY_UTILS = new JavaType("org.apache.commons.lang3.ArrayUtils");
	private static final JavaType ARRAYS = new JavaType("java.util.Arrays");
	private static final JavaType LIST_TYPE = new JavaType("java.util.ArrayList");
	private static final JavaType LIST = new JavaType("java.util.List");
	
	
	@AutoPopulate private String toCsvMethod = "toCsv";
	@AutoPopulate private String toCsvHeaderMethod = "toCsvHeader";
//	@AutoPopulate private String toCsvEntryMethod = "toCsvEntry";
	@AutoPopulate private String[] excludeFields;
	@AutoPopulate private String[] order;
	
	private List<MethodMetadata> locatedAccessors;
	
    public static final String getMetadataIdentiferType() {
        return PROVIDES_TYPE;
    }
    
    public static final String createIdentifier(JavaType javaType, LogicalPath path) {
        return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
    }

    public static final JavaType getJavaType(String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
    }

    public static final LogicalPath getPath(String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
    }

    public static boolean isValid(String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
    }
    
    
	public CsvMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, List<MethodMetadata> locatedAccessors) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		
		// Process values from the annotation, if present
		//logger.warning("before annotation processing");
		AnnotationMetadata annotation = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, new JavaType(RooCsv.class.getName()));
		if (annotation != null) {
			AutoPopulationUtils.populate(this, annotation);
		}
		//logger.warning("after annotation processing");
		//logger.warning("working for "+aspectName);
		this.locatedAccessors = locatedAccessors;
		//logger.warning("located accessors have "+String.valueOf((locatedAccessors.size())+" elements"));
		builder.addMethod(getToCsvMethod());
		builder.addMethod(getCsvHeaderMethod());
		//builder.addMethod(getToCsvEntryMethod());
		//logger.warning("sucessfully across");
		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}
	
    
	private MethodMetadata getCsvHeaderMethod()
	{
		// Compute the relevant method name
		JavaSymbolName methodName = new JavaSymbolName("toCsvHeader");
		if (!this.toCsvHeaderMethod.equals("")) {
			methodName = new JavaSymbolName(this.toCsvHeaderMethod);
		}

		// See if the type itself declared the method
		MethodMetadata result = MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, null);
		if (result != null) {
			return result;
		}
		// Decide whether we need to produce the toString method
		if (this.toCsvHeaderMethod.equals("")) {
			return null;
		}
		
		builder.getImportRegistrationResolver().addImport(ARRAYS);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

		/** Key: field name, Value: accessor name */
		Map<String, List<String>> map = new LinkedHashMap<String, List<String>>();
		/** get excluded fields by user-modified Annotation */
		Set<String> excludeFieldsSet = new LinkedHashSet<String>();
		if (excludeFields != null && excludeFields.length > 0) {
			Collections.addAll(excludeFieldsSet, excludeFields);
		}
		
		//logger.warning("for the class: "+governorTypeDetails.getName().getSimpleTypeName());
		
        bodyBuilder.appendFormalLine(LIST_TYPE.getSimpleTypeName()+"<String> line = new "+LIST_TYPE.getSimpleTypeName()+"<String>();");
        //String[] a = new String[line.size()];
		for (MethodMetadata accessor : locatedAccessors) {
			//AnnotatedJavaType.convertFromJavaType(accessor.getReturnType()).getAnnotations().contains(new AnnotationMetadata("die"));
			String accessorName = accessor.getMethodName().getSymbolName();
			String fieldName = BeanInfoUtils.getPropertyNameForJavaBeanMethod(accessor).getSymbolName();
			logger.info("processing Field "+fieldName);
			List<String> fieldLogic = new ArrayList<String>();
			if (!excludeFieldsSet.contains(fieldName) && !map.containsKey(fieldName)) {
				if (accessor.getReturnType().isCommonCollectionType()|| accessor.getReturnType().isArray()) {
					if(accessor.getReturnType().getParameters().get(0).isPrimitive())
						fieldLogic.add("line.add(\""+governorTypeDetails.getName().getSimpleTypeName()+"."+fieldName+"\");");
					else
						fieldLogic.add("line.addAll("+ARRAYS.getSimpleTypeName()+".asList(" + accessor.getReturnType().getParameters().get(0)+".toCsvHeader()));");
				}
				else
				{
					fieldLogic.add("line.add(\""+governorTypeDetails.getName().getSimpleTypeName()+"."+fieldName+"\");");
				}
	
				map.put(fieldName, fieldLogic);
			}
		}
		
		map = sortMethodPieces(map);
		
		for(Entry<String, List<String>> e : map.entrySet())
		{
			bodyBuilder.appendFormalLine("//Adding for field "+e.getKey());
			for ( String s : e.getValue())
				bodyBuilder.appendFormalLine(s);
		}
		
		bodyBuilder.appendFormalLine("String[] arr = new String[line.size()];");
		bodyBuilder.appendFormalLine("line.toArray(arr);");
		bodyBuilder.appendFormalLine("return arr;");

		//logger.warning("1 - toCsvHeaderMethod");

		JavaType returnType = new JavaType("java.lang.String", 1, DataType.TYPE, null, null);
		//logger.warning("2 - toCsvHeaderMethod - name: "+methodName);
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC | Modifier.STATIC, methodName, returnType, bodyBuilder);
		//logger.warning("3 - toCsvHeaderMethod");
		return methodBuilder.build(); 

	}
/**
 * @return List of all lines with fields encapsulated in String[]
 */
	private MethodMetadata getToCsvMethod() {


		// Compute the relevant method name
		JavaSymbolName methodName = new JavaSymbolName("toCsv");
		if (!this.toCsvMethod.equals("")) {
			methodName = new JavaSymbolName(this.toCsvMethod);
		}

		// See if the type itself declared the method
		MethodMetadata result = MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, null);
		if (result != null) {
			return result;
		}

		// Decide whether we need to produce the toString method
		if (this.toCsvMethod.equals("")) {
			return null;
		}
		
		builder.getImportRegistrationResolver().addImport(ARRAY_UTILS);
		builder.getImportRegistrationResolver().addImport(LIST_TYPE);
		builder.getImportRegistrationResolver().addImport(LIST);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		
		/** Key: field name, Value: accessor name */
		Map<String, List<String>> map = new LinkedHashMap<String, List<String>>();
		
		/** get excluded fields by user-modified Annotation */
		Set<String> excludeFieldsSet = new LinkedHashSet<String>();
		if (excludeFields != null && excludeFields.length > 0) {
			Collections.addAll(excludeFieldsSet, excludeFields);
		}
		bodyBuilder.appendFormalLine(LIST.getSimpleTypeName()+"<String[]> lines = new "+LIST_TYPE.getSimpleTypeName()+"<String[]>();");
		//bodyBuilder.appendFormalLine("lines.add(new String[]{\"\"});");
		bodyBuilder.appendFormalLine(LIST.getSimpleTypeName()+"<String[]> newlines;");
		//logger.warning("1");
		for (MethodMetadata accessor : locatedAccessors) {
			//AnnotatedJavaType.convertFromJavaType(accessor.getReturnType()).getAnnotations().contains(new AnnotationMetadata("die"));
			String accessorName = accessor.getMethodName().getSymbolName();
			//logger.warning("2 - accessor "+accessorName);
			String fieldName = BeanInfoUtils.getPropertyNameForJavaBeanMethod(accessor).getSymbolName();
			logger.info("processing Field "+fieldName);
			List<String> fieldLogic = new ArrayList<String>();
			if (!excludeFieldsSet.contains(fieldName) && !map.containsKey(fieldName)) {
				String accessorText = accessorName + "()";
				fieldLogic.add("newlines = new "+LIST_TYPE.getSimpleTypeName()+"<String[]>();");
				fieldLogic.add("if (lines.isEmpty()) {");
					if (accessor.getReturnType().isCommonCollectionType()|| accessor.getReturnType().isArray()) {
	
						fieldLogic.add("	for ("+accessor.getReturnType().getParameters().get(0)+" o : "+accessorText+") {");
						fieldLogic.add("		for( String[] inner_line : o.toCsv()) {");
						fieldLogic.add("			newlines.add(inner_line); }}");
						
					}
					else if (Calendar.class.getName().equals(accessor.getReturnType().getFullyQualifiedTypeName())) {
						fieldLogic.add("	newlines.add( new String[]{"+accessorText+"== null ? \"\" : "+accessorText+".getTime()}); ");
					}
					else //if (accessor.getReturnType().getClass().isPrimitive())
					{
						fieldLogic.add("	newlines.add(new String[]{"+accessorText+"== null ? \"\" : "+accessorText+".toString()});");
					}
					fieldLogic.add("}");
					fieldLogic.add("else {");

					fieldLogic.add("	for (String[] line : lines) {");
					if (accessor.getReturnType().isCommonCollectionType()|| accessor.getReturnType().isArray()) {
	
						fieldLogic.add("		for ("+accessor.getReturnType().getParameters().get(0)+" o : "+accessorText+") {");
						fieldLogic.add("			for( String[] inner_line : o.toCsv()) {");
						fieldLogic.add("				newlines.add((String[])"+ARRAY_UTILS.getSimpleTypeName()+".addAll(line, inner_line)); }}");
					}
					else if (Calendar.class.getName().equals(accessor.getReturnType().getFullyQualifiedTypeName())) {
						fieldLogic.add("		newlines.add((String[])"+ARRAY_UTILS.getSimpleTypeName()+".addAll(line, new String[]{"+accessorText+"== null ? \"\" : "+accessorText+".getTime()})); ");
					}
					else //if (accessor.getReturnType().getClass().isPrimitive())
					{
						fieldLogic.add("		newlines.add((String[])"+ARRAY_UTILS.getSimpleTypeName()+".addAll(line, new String[]{"+accessorText+"== null ? \"\" : "+accessorText+".toString()}));");
					}
					//else {
					//	fieldLogic.add("	newlines.add("+ARRAY_UTILS.toString()+".addAll(line, "+fieldName+"); ");
					//}
					fieldLogic.add("	}");
				fieldLogic.add("}");
				fieldLogic.add("lines = newlines;");
				map.put(fieldName, fieldLogic);
			}
		}
		//logger.warning("4 map - "+String.valueOf(map.size())+" elements");
		//logger.warning("4.2 order == null? - "+String.valueOf(order == null));
		//sort by field values
		map = sortMethodPieces(map);
		//logger.warning("4 - "+String.valueOf(map.size())+" elements");
		for(Entry<String, List<String>> e : map.entrySet())
		{
			bodyBuilder.appendFormalLine("//Adding for field "+e.getKey());
			for ( String s : e.getValue())
				bodyBuilder.appendFormalLine(s);
		}
		bodyBuilder.appendFormalLine("return lines;");
		List<JavaType> typeParams = new ArrayList<JavaType>();
		typeParams.add(new JavaType("java.lang.String", 1, DataType.TYPE, null, null));
		JavaType returnType = new JavaType("java.util.List", 0, DataType.TYPE, null, typeParams);
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, bodyBuilder);
		//methodBuilder.putCustomData(CustomDataCsvTags.TO_CSV_METHOD, null); 
		return methodBuilder.build(); 
	}
/*
	private MethodMetadata getToCsvEntryMethod() {
		// Compute the relevant method name
		JavaSymbolName methodName = new JavaSymbolName("toCsvEntry");
		if (!this.toCsvEntryMethod.equals("")) {
			methodName = new JavaSymbolName(this.toCsvEntryMethod);
		}

		// See if the type itself declared the method
		MethodMetadata result = MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, null);
		if (result != null) {
			return result;
		}

		// Decide whether we need to produce the toString method
		if (this.toCsvEntryMethod.equals("")) {
			return null;
		}
		

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC | Modifier.STATIC, methodName, JavaType.VOID_PRIMITIVE, bodyBuilder);
		//methodBuilder.putCustomData(CustomDataCsvTags.TO_CSV_ENTRY_METHOD, null);
		return methodBuilder.build();
	}

*/
	private Map<String, List<String>> sortMethodPieces(Map<String, List<String>> map)
	{
		Set<String> orderSet = new LinkedHashSet<String>();
		if (order != null && order.length > 0) {
			//logger.warning("4.3 order - "+String.valueOf(order.length)+" elements");

			Collections.addAll(orderSet, order);
		}
		//logger.warning("4.3 orderSet - "+String.valueOf(orderSet.size())+" elements");

		if(!orderSet.isEmpty())
		{
			Map<String, List<String>> newmap = new LinkedHashMap<String, List<String>>();
			for ( String str : orderSet)
			{
				//logger.warning(str);
				String fname = str;
				if(map.containsKey(fname)) {
					newmap.put(fname, map.get(fname)); 
					map.remove(fname);
				}
			}
			newmap.putAll(map);
			map=newmap;
		}
		return map;
	}
    
    /**
     * Create metadata for a field definition. 
     *
     * @return a FieldMetadata object
     */
    private FieldMetadata getSampleField() {
        // Note private fields are private to the ITD, not the target type, this is undesirable if a dependent method is pushed in to the target type
        int modifier = 0;
        
        // Using the FieldMetadataBuilder to create the field definition. 
        final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), // Metadata ID provided by supertype
            modifier, // Using package protection rather than private
            new ArrayList<AnnotationMetadataBuilder>(), // No annotations for this field
            new JavaSymbolName("sampleField"), // Field name
            JavaType.STRING); // Field type
        
        return fieldBuilder.build(); // Build and return a FieldMetadata instance
    }
    
    private MethodMetadata getSampleMethod() {
        // Specify the desired method name
        JavaSymbolName methodName = new JavaSymbolName("sampleMethod");
        
        // Check if a method with the same signature already exists in the target type
        final MethodMetadata method = methodExists(methodName, new ArrayList<AnnotatedJavaType>());
        if (method != null) {
            // If it already exists, just return the method and omit its generation via the ITD
            return method;
        }
        
        // Define method annotations (none in this case)
        List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        
        // Define method throws types (none in this case)
        List<JavaType> throwsTypes = new ArrayList<JavaType>();
        
        // Define method parameter types (none in this case)
        List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
        
        // Define method parameter names (none in this case)
        List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
        
        // Create the method body
        InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("System.out.println(\"Hello World\");");
        
        // Use the MethodMetadataBuilder for easy creation of MethodMetadata
        MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, parameterTypes, parameterNames, bodyBuilder);
        methodBuilder.setAnnotations(annotations);
        methodBuilder.setThrowsTypes(throwsTypes);
        
        return methodBuilder.build(); // Build and return a MethodMetadata instance
    }
        
    private MethodMetadata methodExists(JavaSymbolName methodName, List<AnnotatedJavaType> paramTypes) {
        // We have no access to method parameter information, so we scan by name alone and treat any match as authoritative
        // We do not scan the superclass, as the caller is expected to know we'll only scan the current class
        for (MethodMetadata method : governorTypeDetails.getDeclaredMethods()) {
            if (method.getMethodName().equals(methodName) && method.getParameterTypes().equals(paramTypes)) {
                // Found a method of the expected name; we won't check method parameters though
                return method;
            }
        }
        return null;
    }
    
    // Typically, no changes are required beyond this point
    
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("identifier", getId());
        builder.append("valid", valid);
        builder.append("aspectName", aspectName);
        builder.append("destinationType", destination);
        builder.append("governor", governorPhysicalTypeMetadata.getId());
        builder.append("itdTypeDetails", itdTypeDetails);
        return builder.toString();
    }
}
