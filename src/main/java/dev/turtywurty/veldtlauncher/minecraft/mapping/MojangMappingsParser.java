package dev.turtywurty.veldtlauncher.minecraft.mapping;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MojangMappingsParser implements MappingsParser {
    public static final MojangMappingsParser INSTANCE = new MojangMappingsParser();
    private static final Pattern METHOD_SIGNATURE_PATTERN =
            Pattern.compile("^(?:\\d+:\\d+:)?(.+?)(?::\\d+:\\d+)?$");

    private MojangMappingsParser() {
    }

    @Override
    public Mappings parse(String mappings) throws MappingsParseException {
        String[] lines = mappings.split("\n");
        Map<String, ClassMapping> classMappings = new HashMap<>(lines.length / 10); // Rough estimate, should be enough to avoid resizing

        String currentObfuscatedClass = null;
        String currentDeobfuscatedClass = null;
        Map<String, FieldMapping> currentClassFieldMappings = null;
        Map<String, MethodMapping> currentClassMethodMappings = null;
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#"))
                continue; // Skip empty lines and comments

            boolean memberLine = !rawLine.isEmpty() && Character.isWhitespace(rawLine.charAt(0));
            if (!memberLine && line.endsWith(":")) {
                // Class mapping
                String[] parts = line.substring(0, line.length() - 1).split(" -> ");
                if (parts.length != 2)
                    throw new MappingsParseException("Invalid class mapping: " + line);

                String obfuscatedName = parts[1].trim();
                String deobfuscatedName = parts[0].trim();

                if (currentObfuscatedClass != null) {
                    // Save the previous class mapping
                    classMappings.put(currentObfuscatedClass, new ClassMapping(
                            currentObfuscatedClass,
                            currentDeobfuscatedClass,
                            currentClassMethodMappings,
                            currentClassFieldMappings
                    ));
                }

                currentObfuscatedClass = obfuscatedName;
                currentDeobfuscatedClass = deobfuscatedName;
                currentClassFieldMappings = new HashMap<>();
                currentClassMethodMappings = new HashMap<>();
            } else {
                // Field or method mapping
                if (currentObfuscatedClass == null)
                    throw new MappingsParseException("Field or method mapping without a class: " + line);

                String[] parts = line.split(" -> ");
                if (parts.length != 2)
                    throw new MappingsParseException("Invalid field or method mapping: " + line);

                String originalName = parts[0].trim();
                String obfuscatedName = parts[1].trim();

                if (originalName.contains("(")) {
                    // Method mapping
                    ParsedMethodMapping parsedMethod = parseMethodMapping(originalName, obfuscatedName);
                    if (parsedMethod != null) {
                        currentClassMethodMappings.put(parsedMethod.obfuscatedName(), parsedMethod.methodMapping());
                    }
                } else {
                    // Field mapping
                    String[] split = originalName.split(" ");
                    if (split.length < 2)
                        continue;

                    String fieldType = split[0];
                    String fieldName = split[1];
                    currentClassFieldMappings.put(obfuscatedName, new FieldMapping(obfuscatedName, fieldName, fieldType));
                }
            }
        }

        if (currentObfuscatedClass != null) {
            // Save the last class mapping
            classMappings.put(currentObfuscatedClass, new ClassMapping(
                    currentObfuscatedClass,
                    currentDeobfuscatedClass,
                    currentClassMethodMappings,
                    currentClassFieldMappings
            ));
        }

        return new Mappings(classMappings);
    }

    private ParsedMethodMapping parseMethodMapping(String source, String obfuscatedName) {
        Matcher matcher = METHOD_SIGNATURE_PATTERN.matcher(source);
        if (!matcher.matches())
            return null;

        String signature = matcher.group(1);
        int firstSpace = signature.indexOf(' ');
        if (firstSpace < 0)
            return null;

        String returnType = signature.substring(0, firstSpace).trim();
        String methodPart = signature.substring(firstSpace + 1).trim();
        int openParen = methodPart.indexOf('(');
        int closeParen = methodPart.lastIndexOf(')');
        if (openParen < 0 || closeParen < openParen)
            return null;

        String methodName = methodPart.substring(0, openParen).trim();
        String parameters = methodPart.substring(openParen + 1, closeParen).trim();
        String[] parameterTypes = parameters.isEmpty() ? new String[0] : parameters.split("\\s*,\\s*");
        MethodMapping mapping = new MethodMapping(obfuscatedName, methodName, returnType, parameterTypes, -1, -1);
        return new ParsedMethodMapping(obfuscatedName, mapping);
    }

    private record ParsedMethodMapping(String obfuscatedName, MethodMapping methodMapping) {
    }
}
