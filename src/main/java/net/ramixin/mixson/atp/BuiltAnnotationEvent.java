package net.ramixin.mixson.atp;

public record BuiltAnnotationEvent(String[] resourceIds, String eventName, int priority, boolean failSilently, int ordinal) {

    public static String generateEventName(String methodName) {
        StringBuilder buffer = new StringBuilder(methodName.length());
        for(int i = 0; i < methodName.length(); i++) {
            char c = methodName.charAt(i);
            if(c == '$') buffer.append(':');
            else if(c == '_') buffer.append('/');
            else if(Character.isUpperCase(c)) {
                buffer.append('_');
                buffer.append(Character.toLowerCase(c));
            }
            else buffer.append(c);
        }
        return buffer.toString();
    }

}
