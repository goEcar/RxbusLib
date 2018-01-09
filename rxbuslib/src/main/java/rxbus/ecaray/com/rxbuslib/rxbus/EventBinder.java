package rxbus.ecaray.com.rxbuslib.rxbus;

/**
 * Created by Administrator on 2018/1/9 0009.
 */

public interface EventBinder<T> {
    void register(T target);
    void unRegister();
}
