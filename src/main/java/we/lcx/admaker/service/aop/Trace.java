package we.lcx.admaker.service.aop;

/**
 * Created by Lin Chenxiao on 2019-12-31
 **/
public @interface Trace {
    String value() default "";
    boolean loop() default false;
    boolean approve() default false;
    boolean main() default false;
}
