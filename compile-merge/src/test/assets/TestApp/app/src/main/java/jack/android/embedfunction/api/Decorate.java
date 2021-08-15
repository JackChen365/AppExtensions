package jack.android.embedfunction.api;

public @interface Decorate {
    String value() default "";
    Class<?> target() default Void.class;
}
