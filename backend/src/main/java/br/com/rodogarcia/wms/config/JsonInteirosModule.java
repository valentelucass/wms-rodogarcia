package br.com.rodogarcia.wms.config;

import org.springframework.stereotype.Component;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.deser.std.DelegatingDeserializer;
import tools.jackson.databind.module.SimpleModule;

/** Conserva valores inteiros Long/Integer sem truncar a parte fracionaria de numeros JSON. */
@Component
public class JsonInteirosModule extends SimpleModule {
    public JsonInteirosModule() {
        super("wms-inteiros-sem-truncamento");
        setDeserializerModifier(
                new ValueDeserializerModifier() {
                    @Override
                    public ValueDeserializer<?> modifyDeserializer(
                            DeserializationConfig config,
                            BeanDescription.Supplier bean,
                            ValueDeserializer<?> original) {
                        Class<?> type = original.handledType();
                        if (type == Long.class || type == long.class)
                            return new InteiroSemTruncamento(original, false);
                        if (type == Integer.class || type == int.class)
                            return new InteiroSemTruncamento(original, true);
                        return original;
                    }
                });
    }

    private static final class InteiroSemTruncamento extends DelegatingDeserializer {
        private final boolean inteiro32bits;

        private InteiroSemTruncamento(ValueDeserializer<?> original, boolean inteiro32bits) {
            super(original);
            this.inteiro32bits = inteiro32bits;
        }

        @Override
        protected ValueDeserializer<?> newDelegatingInstance(ValueDeserializer<?> original) {
            return new InteiroSemTruncamento(original, inteiro32bits);
        }

        @Override
        public Object deserialize(JsonParser parser, DeserializationContext context) {
            if (parser.hasToken(JsonToken.VALUE_NUMBER_FLOAT)) {
                try {
                    if (inteiro32bits)
                        return Integer.valueOf(parser.getDecimalValue().intValueExact());
                    return Long.valueOf(parser.getDecimalValue().longValueExact());
                } catch (ArithmeticException exception) {
                    return context.reportInputMismatch(
                            handledType(), "Valor deve ser um inteiro do dominio declarado.");
                }
            }
            return super.deserialize(parser, context);
        }
    }
}
