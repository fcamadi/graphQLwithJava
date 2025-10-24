package org.francd.model;

import graphql.language.*;

import java.util.Collection;

public class TypeHelper {
    public static ObjectTypeDefinition provinceDefinitionOf(Class<Province> provinceClass) {

        return ObjectTypeDefinition.newObjectTypeDefinition()
                .name("Province")
                .description(new Description("Added via code in GraphQLRuntime",null,false))
                .fieldDefinition(FieldDefinition.newFieldDefinition()
                        .name("name")
                        .type(new NonNullType(new TypeName("String")))
                        .build())
                .fieldDefinition(FieldDefinition.newFieldDefinition()
                        .name("population")
                        .type(new NonNullType(new TypeName("Int")))
                        .build())
                .fieldDefinition(FieldDefinition.newFieldDefinition()
                        .name("capital")
                        .type(new NonNullType(new TypeName("City")))
                        .build())
                .fieldDefinition(FieldDefinition.newFieldDefinition()
                        .name("area")
                        .type(new NonNullType(new TypeName("Int")))
                        .build())
                .implementz(new TypeName("Place"))
                .build();
    }

    /*
     * Using introspection
     */
    public static ObjectTypeDefinition objectDefinitionOf(Class<?> clazz) {

        ObjectTypeDefinition.Builder definitionBuilder = ObjectTypeDefinition.newObjectTypeDefinition()
                .name(clazz.getSimpleName())
                .description(new Description("Added via code",null,false));

        for (var field : clazz.getDeclaredFields()) {
            var typeAnnotation = field.getAnnotation(GraphqlType.class);
            Type<?> gqlType = typeAnnotation != null ?
                    new TypeName(typeAnnotation.value()) : fromJavaClassToGraphQLType(field.getType());

            // make the field non‑null if the Java field is required
            if (isRequired(field)) {
                gqlType = new NonNullType(gqlType);
            }

            definitionBuilder.fieldDefinition(
                    FieldDefinition.newFieldDefinition()
                            .name(field.getName())
                            .type(gqlType)
                            .build());
        }
        for (var interfaz : clazz.getInterfaces()) {
            definitionBuilder.implementz(new TypeName(interfaz.getSimpleName()));
        }

        return definitionBuilder.build();
    }

    private static boolean isRequired(java.lang.reflect.Field field) {
        //primitive types are always required
        if (field.getType().isPrimitive()) {
            return true;
        }
        //look for a @NonNull (or any other marker -dependencies needed in that case)
        return field.isAnnotationPresent(org.springframework.lang.NonNull.class)
                // || field.isAnnotationPresent(javax.validation.constraints.NotNull.class)
                // || field.isAnnotationPresent(org.jetbrains.annotations.NotNull.class)
                ;
    }

    private static Type<?> fromJavaClassToGraphQLType(Class<?> javaType) {
        if (Collection.class.isAssignableFrom(javaType)) {
            return new ListType((new TypeName(javaType.getSimpleName())));
        } else if (Integer.class.isAssignableFrom(javaType) || int.class.isAssignableFrom(javaType) ||
                   Long.class.isAssignableFrom(javaType) || long.class.isAssignableFrom(javaType)) {
            return new TypeName("Int");
        } else if (Double.class.isAssignableFrom(javaType) || double.class.isAssignableFrom(javaType)) {
            return new TypeName("Float");
        } else {
            return new TypeName(javaType.getSimpleName());
        }
    }

}
