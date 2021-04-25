package com.spaghetti.interfaces;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD })
public @interface ReplicationDelay {

	long value() default 0;

}
