package com.example;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import rxbus.ecaray.com.rxbuslib.rxbus.RxBusReact;
import rxbus.ecaray.com.rxbuslib.rxbus.RxBusScheduler;

@AutoService(Processor.class)
public class DIEventProcess extends AbstractProcessor {

    private final static String COMPOSITE_DISPOSABLE_FIELD = "compositeDisposable";
    private final static String SOURCE_PROXY_FIELD = "sourceInstance";
    private final static String SUFFIX = "$$BindEvent";

    private Map<TypeElement, EventInfo> map = new HashMap<>();
    Set<String> erasedTargetNames = new LinkedHashSet<>();
    private Elements elementUtils;
    private ClassName defaultEventClazz;
    private ClassName rxbusHelper;
    private ClassName consumer;
    private ClassName disposable;
    private ClassName predicate;
    private Types typeUtils;
    private Filer filer;
    private Messager messager;
    private ClassName consumermpositeDisposable;


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(RxBusReact.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        initConstantObj();
        getData(roundEnvironment);
        handleData();
        return false;
    }


    private void getData(RoundEnvironment roundEnvironment) {
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(RxBusReact.class);
        for (Element element : elements) {
            RxBusReact annotation = element.getAnnotation(RxBusReact.class);
            if (annotation == null) {
                continue;
            }
            Element enclosingElement = element.getEnclosingElement();
            if (!enclosingElement.getKind().isClass()) {
                continue;
            }
            TypeElement typeElement = (TypeElement) enclosingElement;
            erasedTargetNames.add(typeElement.toString());
//            LinkedList<MethodData> methodDatas = map.get(typeElement);
            EventInfo eventInfo = map.get(typeElement);
            if (eventInfo == null) {
                eventInfo = new EventInfo();
                eventInfo.methodDatas = new LinkedList<>();
                map.put(typeElement, eventInfo);
            }
            MethodData methodData = new MethodData();
            methodData.methodName = element.getSimpleName().toString();

//            methodData.clazzName = typeUtils.TypeElement.annotation.clazz();//TypeName.get(element.asType());
//            methodData.clazzName =  TypeName.get(annotation.clazz()).toString();// annotation.clazz().toString();//TypeName.get(String.class);//typeUtils.TypeElement.annotation.clazz();//TypeName.get(element.asType());


            methodData.tag = annotation.tag();
            methodData.observeOn = annotation.observeOn();
            methodData.subscribeOn = annotation.subscribeOn();
            methodData.strategy = annotation.strategy();
            methodData = extractMethodParameterInfo((ExecutableElement) element, methodData);
            eventInfo.addData(methodData);
        }

        for (Map.Entry<TypeElement, EventInfo> entry : map.entrySet()) {
            String parentClassFqcn = findParentFqcn(entry.getKey(), erasedTargetNames);
            if (parentClassFqcn != null) {
                entry.getValue().parentClazzName = (parentClassFqcn + SUFFIX);
            }
        }
    }

    private void handleData() {
        for (Map.Entry<TypeElement, EventInfo> entry : map.entrySet()) {
            TypeElement key = entry.getKey();
            EventInfo eventInfo = entry.getValue();
            TypeName typeName = TypeName.get(key.asType());

            TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(key.getSimpleName() + SUFFIX);
            typeBuilder.addTypeVariable(TypeVariableName.get("T", typeName));
            String packageName = getPackageName(key);
            if (eventInfo.parentClazzName != null) {
                TypeSpec parent = TypeSpec.classBuilder(eventInfo.parentClazzName)
                        .addModifiers(Modifier.PUBLIC)
                        .addTypeVariable(TypeVariableName.get("T"))
//                        .addSuperinterface(ParameterizedTypeName.get(ClassName.get("rxbus.ecaray.com.rxbuslib.rxbus","EventBinder"),typeName))
                        .build();
//                clazzContent.append(" extends ").append(eventInfo.parentClazzName).append("<T>");
                typeBuilder.superclass(ParameterizedTypeName.get(ClassName.bestGuess(parent.name), TypeVariableName.get("T")));
//                typeBuilder.superclass(ClassName.bestGuess(parent.));
            } else {
                FieldSpec instanceSpec = generateProxySourceInstanceCode(typeName);
                FieldSpec compositeSpec = generateCompositeDisposableField();
                typeBuilder.addField(instanceSpec);
                typeBuilder.addField(compositeSpec);
                //---<implements ViewBinder<MainActivity>>
//                clazzContent.append(" implements BindEvent<T>");select
//                typeBuilder.addSuperinterface(ParameterizedTypeName.get(ClassName.get("rxbus.ecaray.com.rxbuslib.rxbus","EventBinder"),typeName));
                typeBuilder.addSuperinterface(ParameterizedTypeName.get(ClassName.get("rxbus.ecaray.com.rxbuslib.rxbus", "EventBinder"), TypeVariableName.get("T")));
            }

            MethodSpec registerBuilder = generateRegisterMethodCode(entry);
            MethodSpec unRegisterBuilder = generateUnRegisterMethodCode(entry);


            typeBuilder.addModifiers(Modifier.PUBLIC)
                    .addMethod(registerBuilder)
                    .addMethod(unRegisterBuilder);

            ////                    .addTypeVariable(TypeVariableName.get("T",typeName)) //----<T extends MainActivity>
//                .addSuperinterface(ParameterizedTypeName.get(ClassName.get("rxbus.ecaray.com.rxbuslib.rxbus","EventBinder"),typeName))//---<implements ViewBinder<MainActivity>>
            //----<T extends MainActivity>


            try {
                JavaFile.builder(packageName, typeBuilder.build()).build().writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

//    private void generateClassInfo(){
//        TypeSpec aptApi = TypeSpec.classBuilder(key.getSimpleName() + SUFFIX)
//                .addModifiers(Modifier.PUBLIC)
//                .addMethod(registerBuilder)
//                .addMethod(unRegisterBuilder)
//                .addField(compositeSpec)
//                .addField(instanceSpec)
////                    .addTypeVariable(TypeVariableName.get("T",typeName)) //----<T extends MainActivity>
//                .addSuperinterface(ParameterizedTypeName.get(ClassName.get("rxbus.ecaray.com.rxbuslib.rxbus","EventBinder"),typeName))//---<implements ViewBinder<MainActivity>>
//                .build();
//    }

    /**
     *
     */
    private MethodData extractMethodParameterInfo(ExecutableElement methodElement, MethodData methodData) {
        List<? extends VariableElement> methodParams = methodElement.getParameters();
        if (methodParams != null) {
            if (methodParams.size() == 1) {
                VariableElement variableElement = methodParams.get(0);
                methodData.parameterClassSimpleName = getParameterClassName(variableElement);
                methodData.parameterClassFullName = getParameterFullName(variableElement);

                return methodData;
            } else if (methodParams.size() > 1) {
                error("EventSubscribe annotation method's parameters size can't be more than one");
            } else if (methodParams.size() == 0) {
                error("EventSubscribe annotation method's parameters size can't be zero");
            }
        } else {
            error("EventSubscribe annotation method's parameters size can't be zero");
        }

        return methodData;
    }

    private FieldSpec generateCompositeDisposableField() {

        FieldSpec fieldSpec = FieldSpec
                .builder(consumermpositeDisposable, COMPOSITE_DISPOSABLE_FIELD, Modifier.PROTECTED)
                .build();
        return fieldSpec;
    }

    private FieldSpec generateProxySourceInstanceCode(TypeName typeName) {
        FieldSpec fieldSpec = FieldSpec
//                .builder(typeName, SOURCE_PROXY_FIELD, Modifier.PROTECTED)
                .builder(TypeVariableName.get("T"), SOURCE_PROXY_FIELD, Modifier.PROTECTED)
                .build();
        return fieldSpec;
    }

    private MethodSpec generateRegisterMethodCode(Map.Entry<TypeElement, EventInfo> entry) {
        TypeElement key = entry.getKey();
        EventInfo value = entry.getValue();

        MethodSpec.Builder methodSpecBuilder = MethodSpec
                .methodBuilder("register")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
//                .addParameter(TypeName.get(key.asType()), "target")
                .addParameter(TypeVariableName.get("T"), "target")
                .returns(void.class);

        if (value.parentClazzName != null) {
            methodSpecBuilder.addStatement("super.register(target)");
        } else {
            methodSpecBuilder.addStatement(SOURCE_PROXY_FIELD + "=target");
            methodSpecBuilder.beginControlFlow("if(compositeDisposable==null || compositeDisposable.isDisposed())");
            methodSpecBuilder.addStatement("compositeDisposable = new CompositeDisposable()");
            methodSpecBuilder.endControlFlow();
        }


        for (MethodData methodData : value.methodDatas) {
            String methodName = methodData.methodName;

            String disposableName = methodName + "_disposable";
            ClassName filterClazz = ClassName.bestGuess(methodData.parameterClassFullName);
            String observeOn = generateInvokeRegisterCode(methodData.observeOn);
            String subscribeOn = generateInvokeRegisterCode(methodData.subscribeOn);
            methodSpecBuilder.addStatement(
                    "$T " + disposableName + " = $T.getDefault()" +
//                            ".filter(" +
//                            generateFilterCode(methodData)+
                            ".register(" +
                            generateConsumerCode(SOURCE_PROXY_FIELD, methodName, methodData) +
                            "," + methodData.parameterClassFullName + ".class" + ",$S" + "," + subscribeOn + "," + observeOn + ",$S" + ")"
                    , disposable, rxbusHelper
                    , consumer, defaultEventClazz, defaultEventClazz, filterClazz
                    , methodData.tag, methodData.strategy
            );

            methodSpecBuilder.addStatement("compositeDisposable.add(" + disposableName + ")");
        }
//        methodSpecBuilder.addStatement("return " + COMPOSITE_DISPOSABLE_FIELD);

        return methodSpecBuilder.build();
    }

    private void initConstantObj() {
        if (defaultEventClazz == null) {
            predicate = ClassName.bestGuess("io.reactivex.functions.Predicate");
            defaultEventClazz = ClassName.bestGuess("rxbus.ecaray.com.rxbuslib.rxbus.RxBusEvent");
            rxbusHelper = ClassName.bestGuess("rxbus.ecaray.com.rxbuslib.rxbus.RxBus");
            consumer = ClassName.bestGuess("io.reactivex.functions.Consumer");
            disposable = ClassName.bestGuess("io.reactivex.disposables.Disposable");
            consumermpositeDisposable = ClassName.bestGuess("io.reactivex.disposables.CompositeDisposable");
        }
    }

    private String generateConsumerCode(String sourceInstanceName, String sourceInstanceMethodName, MethodData methodData) {
        return
                "new $T<$T>(){\n" +
                        "@Override\n" +
                        "public void accept($T o) throws Exception {\n" +
                        "if(" + sourceInstanceName + "==null)return;\n" +
//                        sourceInstanceName + "." + sourceInstanceMethodName + "(("+methodData.parameterClassSimpleName+")o.getObj());\n" +
                        sourceInstanceName + "." + sourceInstanceMethodName + "(($T)o.getObj());\n" +
                        "}\n" +
                        "}";
    }

    private String generateFilterCode(MethodData methodData) {
        return
                "new $T<$T>(){\n" +
                        "@Override\n" +
                        "public boolean test($T o) throws Exception {\n" +
                        "return " + methodData.parameterClassSimpleName + ".class == " + "o.getObj().getClass()"
                        + " && $S" + ".equals(o.getTag());" +
                        "}\n" +
                        "}";
    }


//    new Predicate<RxBusEvent>(defaultEventClazz,defaultEventClazz) {
//        @Override
//        public boolean test(RxBusEvent rxBusEvent) throws Exception {
//            return clazz.equals(rxBusEvent.getObj().getClass()) &&
//                    tag.equals(rxBusEvent.getTag());
//        }
//    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_7;
    }

    public String getPackageName(TypeElement key) {
        return elementUtils.getPackageOf(key).getQualifiedName().toString();
    }


    /**
     * judge the java basic type
     *
     * @param element
     * @return
     */
    private boolean isBasicType(Element element) {
        return element.asType().getKind().isPrimitive();
    }

    private String getParameterFullName(VariableElement variableElement) {
        String typeString = "";
        if (isBasicType(variableElement)) {
            switch (variableElement.asType().getKind()) {
                case BOOLEAN:
                    typeString = "java.lang.Boolean";
                    break;
                case BYTE:
                    typeString = "java.lang.Byte";
                    break;
                case SHORT:
                    typeString = "java.lang.Short";
                    break;
                case INT:
                    typeString = "java.lang.Integer";
                    break;
                case LONG:
                    typeString = "java.lang.Long";
                    break;
                case CHAR:
                    typeString = "java.lang.Char";
                    break;
                case FLOAT:
                    typeString = "java.lang.Float";
                    break;
                case DOUBLE:
                    typeString = "java.lang.Double";
                    break;
                default:
                    break;
            }
        } else {
            typeString = variableElement.asType().toString();
        }
        return typeString;
    }

    private String getParameterClassName(VariableElement variableElement) {
        String typeString = "";
        if (isBasicType(variableElement)) {
            switch (variableElement.asType().getKind()) {
                case BOOLEAN:
                    typeString = "Boolean";
                    break;
                case BYTE:
                    typeString = "Byte";
                    break;
                case SHORT:
                    typeString = "Short";
                    break;
                case INT:
                    typeString = "Integer";
                    break;
                case LONG:
                    typeString = "Long";
                    break;
                case CHAR:
                    typeString = "Char";
                    break;
                case FLOAT:
                    typeString = "Float";
                    break;
                case DOUBLE:
                    typeString = "Double";
                    break;
                default:
                    break;
            }
        } else {
            Element element = typeUtils.asElement(variableElement.asType());
            typeString = element.getSimpleName().toString();
        }
        return typeString;
    }

    private String generateInvokeRegisterCode(String scheduler) {
        StringBuilder codeBuilder = new StringBuilder();
        switch (scheduler) {
            case RxBusScheduler.MAIN_THREAD:
                codeBuilder.append("io.reactivex.android.schedulers.AndroidSchedulers.mainThread()");
                break;
            case RxBusScheduler.IO:
                codeBuilder.append("io.reactivex.schedulers.Schedulers.io()");
                break;
            case RxBusScheduler.NEW_THREAD:
                codeBuilder.append("io.reactivex.schedulers.Schedulers.newThread()");
                break;
            case RxBusScheduler.COMPUTATION:
                codeBuilder.append("io.reactivex.schedulers.Schedulers.computation()");
                break;
            case RxBusScheduler.TRAMPOLINE:
                codeBuilder.append("io.reactivex.schedulers.Schedulers.trampoline()");
                break;
            default:
                break;
        }
        return codeBuilder.toString();
    }

    private MethodSpec generateUnRegisterMethodCode(Map.Entry<TypeElement, EventInfo> entry) {
        MethodSpec.Builder methodSpecBuilder = MethodSpec
                .methodBuilder("unRegister")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
//                .addParameter(TypeName.get(entry.getKey().asType()), "target")
                .returns(void.class);
        if (entry.getValue().parentClazzName != null) {
            methodSpecBuilder.addStatement("super.unRegister()");
        }else {
            methodSpecBuilder.addStatement(SOURCE_PROXY_FIELD + "= null");
            methodSpecBuilder.beginControlFlow(
                    "if (compositeDisposable != null && !compositeDisposable.isDisposed())");
            methodSpecBuilder.addStatement("compositeDisposable.dispose()");
            methodSpecBuilder.addStatement("compositeDisposable = null");

            methodSpecBuilder.endControlFlow();
        }
        return methodSpecBuilder.build();
    }

    private void error(Element e, String msg, Object... args) {
        messager.printMessage(
                Diagnostic.Kind.ERROR,
                String.format(msg, args),
                e);
    }

    private void error(String msg, Object... args) {
        messager.printMessage(
                Diagnostic.Kind.ERROR,
                String.format(msg, args));
    }

    /**
     * Finds the parent barbershop type in the supplied set, if any.
     */
    private String findParentFqcn(TypeElement typeElement, Set<String> parents) {
        TypeMirror type;
        while (true) {
            type = typeElement.getSuperclass();
            if (type.getKind() == TypeKind.NONE) {
                return null;
            }
            typeElement = (TypeElement) ((DeclaredType) type).asElement();
            if (parents.contains(typeElement.toString())) {
                String packageName = getPackageName(typeElement);
                return packageName + "." + getClassName(typeElement, packageName);
            }
        }
    }

    private static String getClassName(TypeElement type, String packageName) {
        int packageLen = packageName.length() + 1;
        return type.getQualifiedName().toString().substring(packageLen).replace(".", "$");
    }


}
