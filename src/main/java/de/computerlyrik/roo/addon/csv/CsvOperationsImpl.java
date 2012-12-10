package de.computerlyrik.roo.addon.csv;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.PhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.DependencyScope;
import org.springframework.roo.project.DependencyType;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Repository;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.springframework.uaa.client.util.Assert;
import org.w3c.dom.Element;

/**
 * Implementation of operations this add-on offers.
 * 
 * @since 1.1
 */
@Component
// Use these Apache Felix annotations to register your commands class in the Roo
// container
@Service
public class CsvOperationsImpl implements CsvOperations {

	private static Logger logger = HandlerUtils
			.getLogger(CsvOperationsImpl.class);
	@Reference
	private MetadataService metadataService;
	@Reference
	private PhysicalTypeMetadataProvider physicalTypeMetadataProvider;

	/**
	 * Use ProjectOperations to install new dependencies, plugins, properties,
	 * etc into the project configuration
	 */
	@Reference
	private ProjectOperations projectOperations;

	/**
	 * Use TypeLocationService to find types which are annotated with a given
	 * annotation in the project
	 */
	@Reference
	private TypeLocationService typeLocationService;

	/**
	 * Use TypeManagementService to change types
	 */
	@Reference
	private TypeManagementService typeManagementService;

	/** {@inheritDoc} */
	public boolean isCommandAvailable() {
		// Check if a project has been created
		return projectOperations.isFocusedProjectAvailable();
	}

	/** {@inheritDoc} */
	public void annotateType(JavaType javaType) {
		Assert.notNull(javaType, "Java type required");

		
		/**
		 * Add maven deps
		 */
		List<Dependency> deps = new ArrayList<Dependency>();
		for (Element dependencyElement : XmlUtils.findElements(
				"/configuration/dependencies/dependency",
				XmlUtils.getConfiguration(getClass()))) {
			deps.add(new Dependency(dependencyElement));
		}
		logger.warning(deps.toString());

		//Disabled temporarily
		//projectOperations.addDependencies(PhysicalTypeIdentifier.getMetadataIdentiferType(),deps);
		
		// Obtain ClassOrInterfaceTypeDetails for this java type
		ClassOrInterfaceTypeDetails existing = typeLocationService.getTypeDetails(javaType);

		existing.getDeclaredFields();

		if (existing != null && MemberFindingUtils.getAnnotationOfType( existing.getAnnotations(), new JavaType(RooCsv.class.getName())) == null) {

			
			ClassOrInterfaceTypeDetailsBuilder classOrInterfaceTypeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(
					existing);

			// Create JavaType instance for the add-ons trigger annotation
			JavaType rooRooCsv = new JavaType(RooCsv.class.getName());

			// Create Annotation metadata
			AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(
					rooRooCsv);

			// Add annotation to target type
			classOrInterfaceTypeDetailsBuilder.addAnnotation(annotationBuilder
					.build());

			// Save changes to disk
			typeManagementService
					.createOrUpdateTypeOnDisk(classOrInterfaceTypeDetailsBuilder
							.build());
		}
	}

	/** {@inheritDoc} */
	public void annotateAll() {
		// Use the TypeLocationService to scan project for all types with a
		// specific annotation
		for (JavaType type : typeLocationService
				.findTypesWithAnnotation(new JavaType(
						"org.springframework.roo.addon.javabean.RooJavaBean"))) {
			annotateType(type);
		}
	}

	/** {@inheritDoc} */
	public void setup() {
		// Install the add-on Google code repository needed to get the
		// annotation
		/*
		projectOperations.addRepository("", new Repository(
				"roo-csv-addon-repository", "Csv Roo add-on repository",
				"https://Repo"));
		*/
		List<Dependency> dependencies = new ArrayList<Dependency>();

		// Install the dependency on the add-on jar (
		dependencies.add(new Dependency("de.computerlyrik.roo.addon.csv",
				"de.computerlyrik.roo.addon.csv", "0.1",
				DependencyType.JAR, DependencyScope.PROVIDED));

		// Install dependencies defined in external XML file
		for (Element dependencyElement : XmlUtils.findElements(
				"/configuration/batch/dependencies/dependency",
				XmlUtils.getConfiguration(getClass()))) {
			dependencies.add(new Dependency(dependencyElement));
		}

		// Add all new dependencies to pom.xml
		projectOperations.addDependencies("", dependencies);
	}
}