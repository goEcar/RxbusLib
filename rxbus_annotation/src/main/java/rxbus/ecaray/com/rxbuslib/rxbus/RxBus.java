package rxbus.ecaray.com.rxbuslib.rxbus;


import java.util.HashMap;
import java.util.Map;

import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.operators.flowable.FlowableOnBackpressureError;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;


public class RxBus {

    private static RxBus sBus;

    private FlowableProcessor<RxBusEvent> mSubject;
//    private ReplayProcessor<RxBusEvent> mStickySubject;

    private RxBus() {
        mSubject = PublishProcessor.<RxBusEvent>create().toSerialized();
//        mStickySubject = ReplayProcessor.createWithSize(1);
    }

    public static RxBus getDefault() {
        if (sBus == null) {
            sBus = new RxBus();
        }
        return sBus;
    }

    private static Map<String, EventBinder<Object>> eventBinders = new HashMap<>();


    public static final String ANDROID_PREFIX = "android.";
    public static final String JAVA_PREFIX = "java.";
    public void register(Object target) {
        Class<?> aClass = target.getClass();
        String clsName = target.getClass().getName();
        EventBinder<Object> eventBinder = eventBinders.get(clsName);

        Class<?> eventBindingClass = null;
        if (eventBinder == null) {
            eventBindingClass = findClass(aClass);
            if(eventBindingClass == null){
                return;
            }
            try {
                eventBinder = (EventBinder) eventBindingClass.newInstance();
                eventBinders.put(clsName, eventBinder);
                eventBinder.register(target);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private Class<?> findClass(Class clazz) {
        Class<?> aClass;
        String clazzNmae = clazz.getName();
        if (clazzNmae.startsWith(ANDROID_PREFIX) || clazzNmae.startsWith(JAVA_PREFIX)) {
//                if (debug) Log.d(TAG, "MISS: Reached framework class. Abandoning search.");
            return null;
        }
        try {
            aClass = Class.forName(clazz.getName() + "$$BindEvent");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            aClass = findClass(clazz.getSuperclass());
        }
        return aClass;
    }


    public void unregister(Object object) {
        if (eventBinders != null && object != null) {
            String clsName = object.getClass().getName();
            EventBinder<Object> objectEventBinder = eventBinders.remove(clsName);
            if (objectEventBinder != null) {
                objectEventBinder.unRegister();
            }
        }

    }


    public void post(Object event, String tag) {
        if (mSubject != null) {
            mSubject.onNext(new RxBusEvent(event, tag));
        }
    }

    public Flowable<RxBusEvent> getObservable() {
//        return mSubject.asObservable().mergeWith(mStickySubject.asObservable());
//        return mSubject.asObservable().mergeWith(mStickySubject.asObservable());
        return mSubject;

    }

    public FlowableProcessor<RxBusEvent> get() {
        return mSubject;
    }

    public boolean hasObservers() {
        return mSubject.hasSubscribers();
    }

    public Disposable register(Consumer<RxBusEvent> consumer, final Class clazz, final String tag, Scheduler subscribeOn, Scheduler observeOn, String strategy) {
        if (consumer != null) {

            Flowable<RxBusEvent> rxBusEventFlowable = getObservable()
                    .subscribeOn(subscribeOn)
                    .filter(new Predicate<RxBusEvent>() {
                        @Override
                        public boolean test(RxBusEvent rxBusEvent) throws Exception {
                            return clazz.equals(rxBusEvent.getObj().getClass()) &&
                                    tag.equals(rxBusEvent.getTag());
                        }
                    })
                    .observeOn(observeOn);
            return handleStrategy(rxBusEventFlowable, strategy).subscribe(consumer);
        }
        return null;
    }

    public Flowable<RxBusEvent> handleStrategy(Flowable<RxBusEvent> o, String strategy) {
        switch (strategy) {
            case RxBusStategy.DROP:
                o = o.onBackpressureDrop();
            case RxBusStategy.LATEST:
                o = o.onBackpressureLatest();
            case RxBusStategy.MISSING:
                o = o;
            case RxBusStategy.ERROR:
                o = RxJavaPlugins.onAssembly(new FlowableOnBackpressureError<>(o));
            default:
                o = o.onBackpressureBuffer();
        }
        return o;
    }
}
