package com.spaghetti.interfaces;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface ReplicationDelay {

	long toClient() default 0;

	long toServer() default 0;

}
