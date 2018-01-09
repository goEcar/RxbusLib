package rxbus.ecaray.com.rxbuslib.rxbus;

public class RxBusEvent {

    private String tag;
    private Object obj;

    public RxBusEvent(Object obj, String tag) {
        this.tag = tag;
        this.obj = obj;
    }

    public String getTag() {
        return tag;
    }

    public Object getObj() {
        return obj;
    }
}
