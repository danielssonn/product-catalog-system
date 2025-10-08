package com.bank.product.party.config;

import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * GraphQL configuration for custom scalar types
 */
@Configuration
public class GraphQLConfig {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        // Create LocalDate scalar based on Date scalar
        GraphQLScalarType localDateScalar = GraphQLScalarType.newScalar()
                .name("LocalDate")
                .description("A LocalDate scalar")
                .coercing(ExtendedScalars.Date.getCoercing())
                .build();

        return wiringBuilder -> wiringBuilder
                .scalar(ExtendedScalars.DateTime)
                .scalar(ExtendedScalars.Json)
                .scalar(localDateScalar);
    }
}
