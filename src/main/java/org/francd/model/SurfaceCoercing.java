package org.francd.model;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("NullableProblems")
public class SurfaceCoercing implements Coercing<Surface, String> {

    @Override
    public String serialize(Object dataFetcherResult,  GraphQLContext graphQLContext,  Locale locale) throws CoercingSerializeException {
        Surface surface = convertToSurface(dataFetcherResult);
        return surface.toString();
    }

    private Surface convertToSurface(Object dataFetcherResult) {
        if (dataFetcherResult instanceof Surface surface) {
            return surface;
        }
        if (dataFetcherResult instanceof Double amount) {
            return new Surface(amount, Surface.Unit.KM2);
        }
        if (dataFetcherResult instanceof Integer amount) {
            return new Surface((double)amount, Surface.Unit.KM2);
        }
        if (dataFetcherResult instanceof BigInteger amount) {
            return new Surface(amount.doubleValue(), Surface.Unit.KM2);
        }
        if (dataFetcherResult instanceof BigDecimal amount) {
            return new Surface(amount.doubleValue(), Surface.Unit.KM2);
        }
        if (dataFetcherResult instanceof String surfaceStr) {
            return parseSurface(surfaceStr);
        }
        throw new CoercingSerializeException(
                "Don't know how to coerce from %s to Surface".formatted(dataFetcherResult));
    }

    /*
     * Parses a string of shape XXX.YYY UU
     * whwere XXX and YYY are digits and UU is km2, km², m2 or m²
     */
    private static final Pattern SURFACE_PATTERN = Pattern.compile(
            "(?<AMOUNT>" +
            "-?" +              // optional sign
            "[0-9]*" +          // whole part
            "([.][0-9]+)?" +    // decimal part (optional)
            ")" +
            "\\s*" +            // separation between amount and unit
            "(?<UNIT>(km2|km²|m2|m²))");

    private Surface parseSurface(String surfaceStr) {
        Matcher matcher = SURFACE_PATTERN.matcher(surfaceStr);
        if (matcher.matches()) {
            Double amount = Double.parseDouble(matcher.group("AMOUNT"));
            String unitStr = matcher.group("UNIT");
            return new Surface(amount, Surface.Unit.of(unitStr));
        } else {
            throw new CoercingParseValueException("Unable to parse from %s to Surface".formatted(surfaceStr));
        }
    }

    @Override
    public Surface parseValue(Object input, GraphQLContext graphQLContext, Locale locale) throws CoercingParseValueException {
        return convertToSurface(input);
    }

    @Override
    public Surface parseLiteral(Value<?> input,  CoercedVariables variables,  GraphQLContext graphQLContext,  Locale locale) throws CoercingParseLiteralException {
        if (input instanceof IntValue intValue) {
            return convertToSurface(intValue.getValue());
        }
        if (input instanceof FloatValue floatValue) {
            return convertToSurface(floatValue.getValue());
        }
        if (input instanceof StringValue stringValue) {
            return convertToSurface(stringValue.getValue());
        }
        throw new CoercingParseLiteralException(
                "Don't know how to parse literal from %s to Surface".formatted(input));
    }

    @Override
    public Value<?> valueToLiteral(Object input,  GraphQLContext graphQLContext,  Locale locale) {
        return new StringValue(convertToSurface(input).toString());
    }
}
