package dev.turtywurty.veldtlauncher.minecraft.mapping;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Mappings(Map<String, ClassMapping> classMappings) {
    private static final Pattern STACKTRACE_CLASS_PATTERN = Pattern.compile(
            "(\\bat\\s+)([A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*)(\\.)([A-Za-z_$][\\w$]*)(\\([^)]*\\))"
    );

    public Mappings {
        Objects.requireNonNull(classMappings, "classMappings cannot be null");
        classMappings = Map.copyOf(classMappings);
    }

    public static Mappings empty() {
        return new Mappings(Map.of());
    }

    public ClassMapping getClassMapping(String obfuscatedName) {
        return classMappings.get(obfuscatedName);
    }

    public boolean hasClassMapping(String obfuscatedName) {
        return classMappings.containsKey(obfuscatedName);
    }

    public boolean isEmpty() {
        return classMappings.isEmpty();
    }

    public String mapClassName(String obfuscatedName) {
        if (obfuscatedName == null || obfuscatedName.isBlank())
            return obfuscatedName;

        ClassMapping mapping = this.classMappings.get(obfuscatedName);
        return mapping == null ? obfuscatedName : mapping.deobfuscatedName();
    }

    public String mapLoggerName(String loggerName) {
        return mapClassName(loggerName);
    }

    public String remapStackTraceClasses(String text) {
        if (text == null || text.isBlank() || this.classMappings.isEmpty())
            return text;

        Matcher matcher = STACKTRACE_CLASS_PATTERN.matcher(text);
        StringBuilder builder = new StringBuilder(text.length());
        while (matcher.find()) {
            String prefix = matcher.group(1);
            String className = matcher.group(2);
            String methodSeparator = matcher.group(3);
            String methodName = matcher.group(4);
            String suffix = matcher.group(5);
            String remappedClassName = remapStackTraceClassName(className);
            String remappedMethodName = remapStackTraceMethodName(className, methodName);
            matcher.appendReplacement(builder, Matcher.quoteReplacement(
                    prefix + remappedClassName + methodSeparator + remappedMethodName + suffix
            ));
        }

        matcher.appendTail(builder);
        return builder.toString();
    }

    private String remapStackTraceClassName(String className) {
        String remappedClassName = mapClassName(className);
        if (!Objects.equals(remappedClassName, className))
            return remappedClassName;

        int innerClassSeparator = className.indexOf('$');
        if (innerClassSeparator < 0)
            return className;

        String outerClassName = className.substring(0, innerClassSeparator);
        String remappedOuterClassName = mapClassName(outerClassName);
        if (Objects.equals(remappedOuterClassName, outerClassName))
            return className;

        return remappedOuterClassName + className.substring(innerClassSeparator);
    }

    private String remapStackTraceMethodName(String className, String methodName) {
        if (methodName == null || methodName.isBlank())
            return methodName;

        ClassMapping classMapping = this.classMappings.get(className);
        if (classMapping == null) {
            int innerClassSeparator = className.indexOf('$');
            if (innerClassSeparator >= 0) {
                classMapping = this.classMappings.get(className.substring(0, innerClassSeparator));
            }
        }

        if (classMapping == null)
            return methodName;

        MethodMapping methodMapping = classMapping.methodMappings().get(methodName);
        return methodMapping == null ? methodName : methodMapping.deobfuscatedName();
    }
}
