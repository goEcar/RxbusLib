package rxbus.ecaray.com.rxbuslib.rxbus;


public class RxBusStategy {


    /**
     * OnNext events are written without any buffering or dropping.
     * Downstream has to deal with any overflow.
     * <p>Useful when one applies one of the custom-parameter onBackpressureXXX operators.
     */
    public static final String MISSING = "MISSING";
    /**
     * Signals a MissingBackpressureException in case the downstream can't keep up.
     */
    public static final String ERROR = "ERROR";
    /**
     * Buffers <em>all</em> onNext values until the downstream consumes it.
     */
    public static final String BUFFER = "BUFFER";
    /**
     * Drops the most recent onNext value if the downstream can't keep up.
     */
    public static final String DROP = "DROP";
    /**
     * Keeps only the latest onNext value, overwriting any previous value if the
     * downstream can't keep up.
     */
    public static final String LATEST = "LATEST";

    /**
     * default strategy,do nothing
     */
    public static final String DEFAULT = "DEFAULT";
}
