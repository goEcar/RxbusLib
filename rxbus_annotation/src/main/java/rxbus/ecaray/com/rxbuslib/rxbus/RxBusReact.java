package rxbus.ecaray.com.rxbuslib.rxbus;



import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RxBusReact {
    String defaultTag = "default";

    Class clazz() default Object.class;

    String tag() default defaultTag;

//    @RxBusScheduler.Theme
    String subscribeOn() default RxBusScheduler.IO;


    String observeOn() default RxBusScheduler.MAIN_THREAD;

    String strategy() default RxBusStaregy.DEFAULT;
}
