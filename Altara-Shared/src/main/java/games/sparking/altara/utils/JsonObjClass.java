package games.sparking.altara.utils;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class JsonObjClass {

    // Constructor that accepts a map and sets fields dynamically
    public JsonObjClass(Map<String, Object> jsonMap) {
        setFieldsFromMap(jsonMap);
    }

    // Method to set fields dynamically using reflection
    private void setFieldsFromMap(Map<String, Object> jsonMap) {
        Class<?> currentClass = this.getClass();
        while (currentClass != null) {
            Field[] fields = currentClass.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                if (jsonMap.containsKey(field.getName())) {
                    try {
                        field.set(this, jsonMap.get(field.getName()));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            currentClass = currentClass.getSuperclass();
        }
    }

    // Refactored toJson method using Gson
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        Class<?> currentClass = this.getClass();

        while (currentClass != null) {
            Field[] fields = currentClass.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    Object value = field.get(this);
                    if (value != null) {
                        jsonObject.addProperty(field.getName(), value.toString());
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        return jsonObject;
    }
}