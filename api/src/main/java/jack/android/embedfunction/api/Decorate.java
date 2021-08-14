package jack.android.embedfunction.api;

/**
 * Created on 2021/8/7.
 *
 * @author Jack Chen
 */
public @interface Decorate {
    String value() default "";
    Class<?> target() default Void.class;
}
