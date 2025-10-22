package org.francd;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;

public class PojoClassTypeResolver implements TypeResolver {

    @Override
    public GraphQLObjectType getType(TypeResolutionEnvironment env) {

        var concreteObject = env.getObject();
        return env.getSchema().getObjectType(concreteObject.getClass().getSimpleName());
    }
}
