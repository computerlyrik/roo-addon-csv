package de.computerlyrik.roo.addon.csv;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Trigger annotation for this add-on.
 
 * @since 1.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooCsv {
	
	String toCsvMethod() default "toCsv";
	String toCsvHeaderMethod() default "toCsvHeader";
	
	String[] excludeFields() default "";
	String[] order() default "";
}

