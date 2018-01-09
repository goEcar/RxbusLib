package rxbus.ecaray.com.rxbuslib.rxbus;


import android.annotation.SuppressLint;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.operators.flowable.FlowableOnBackpressureError;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;


/**
 * Created by Jiashu on 2015/11/3.
 * RxBus 基于 RxJava 设计的用于组件间通讯的事件总线。test
 */
public class RxBus {

    private static RxBus sBus;

    private FlowableProcessor<RxBusEvent> mSubject;
//    private ReplayProcessor<RxBusEvent> mStickySubject;

    @SuppressLint("NewApi")
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

    /**
     * 注册
     */
    public void register(Object target) {
        String clsName = target.getClass().getName();
        EventBinder<Object> eventBinder = eventBinders.get(clsName);

        try {
            if (eventBinder == null) {
                Class<?> eventBindingClass = Class.forName(clsName + "$$BindEvent");
                eventBinder = (EventBinder) eventBindingClass.newInstance();
                eventBinders.put(clsName,eventBinder);
            }
            eventBinder.register(target);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 撤销注册 RxBus
     */
    @SuppressLint("NewApi")
    public void unregister(Object object) {
        if(eventBinders!=null && object!=null){
            String clsName = object.getClass().getName();
            EventBinder<Object> objectEventBinder = eventBinders.remove(clsName);
            if(objectEventBinder!=null){
                objectEventBinder.unRegister();
            }
        }

    }


    /**
     * 发送一个事件，并标记该事件为 tag。只有指定为 tag 的地方才能响应该 事件。
     *
     * @param event
     * @param tag
     */
    public void post(Object event, String tag) {
        if (mSubject != null) {
            mSubject.onNext(new RxBusEvent(event, tag));
        }
    }

    /**
     * 返回 事件发布者。
     * 熟悉 RxJava 的开发者可以通过本方法获取到 事件发布者，自定义事件响应策略。
     *
     * @return
     */
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

    public Disposable register(Consumer<RxBusEvent> consumer, final Class clazz, final String tag, String subscribeOn, String observeOn, String strategy) {
        if (consumer != null) {
            Scheduler observeScheduler = getScheduler(observeOn);
            //获取订阅线程
            Scheduler subscribeScheduler = getScheduler(subscribeOn);

            Flowable<RxBusEvent> rxBusEventFlowable = getObservable()
                    .subscribeOn(subscribeScheduler)
                    .filter(new Predicate<RxBusEvent>() {
                        @Override
                        public boolean test(RxBusEvent rxBusEvent) throws Exception {
                            return clazz.equals(rxBusEvent.getObj().getClass()) &&
                                    tag.equals(rxBusEvent.getTag());
                        }
                    })
                    .observeOn(observeScheduler);
            return handleStrategy(rxBusEventFlowable,strategy).subscribe(consumer);
        }
        return null;
    }

    public Flowable<RxBusEvent> handleStrategy(Flowable<RxBusEvent> o, String strategy){
        switch (strategy) {
            case RxBusStaregy.DROP:
                o = o.onBackpressureDrop();
            case RxBusStaregy.LATEST:
                o = o.onBackpressureLatest();
            case RxBusStaregy.MISSING:
                o = o;
            case RxBusStaregy.ERROR:
                o = RxJavaPlugins.onAssembly(new FlowableOnBackpressureError<>(o));
            default:
                o = o.onBackpressureBuffer();
        }
        return o;
    }

    private static HashMap<String, Scheduler> sSchedulersMapper;
    static {
        sSchedulersMapper = new HashMap<>();
        sSchedulersMapper.put(RxBusScheduler.NEW_THREAD, Schedulers.newThread());
        sSchedulersMapper.put(RxBusScheduler.COMPUTATION, Schedulers.computation());
        sSchedulersMapper.put(RxBusScheduler.IO, Schedulers.io());
        sSchedulersMapper.put(RxBusScheduler.TRAMPOLINE, Schedulers.trampoline());
        sSchedulersMapper.put(RxBusScheduler.MAIN_THREAD, AndroidSchedulers.mainThread());
    }

    public static Scheduler getScheduler(String key) {//(@Theme String key) {//注解限制类
        return sSchedulersMapper.get(key);
    }


}
