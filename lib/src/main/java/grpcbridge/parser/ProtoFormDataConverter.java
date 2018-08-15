package grpcbridge.parser;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.net.MediaType;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import grpcbridge.Exceptions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;

public final class ProtoFormDataConverter extends ProtoConverter {

    public static final ProtoFormDataConverter INSTANCE = new ProtoFormDataConverter();
    private static final Gson gson = new Gson();
    private static final Type type = new TypeToken<Map<String, String>>() {}.getType();

    private static final MediaType contentType =
            MediaType.FORM_DATA.withCharset(Charsets.UTF_8);

    private ProtoFormDataConverter() {
    }

    @Override
    public String serialize(Integer index, JsonFormat.Printer printer, @Nonnull Message message) {
        try {
            String json = printer.print(message);
            Map<String, String> msg = new Gson().fromJson(json, type);
            msg = msg.entrySet().parallelStream()
                    .collect(Collectors.toMap(
                            it -> index == null ? it.getKey() : format("{%s}[%s]", it.getKey(), index),
                            it -> {
                                try {
                                    return URLEncoder.encode(
                                            it.getValue(),
                                            contentType.charset().or(Charsets.UTF_8).name()
                                    );
                                } catch (UnsupportedEncodingException e) {
                                    throw new Exceptions.ParsingException(
                                            format("Failed to serialize a message: {%s}", message), e
                                    );
                                }
                            }));
            return Joiner.on('&').withKeyValueSeparator('=').join(msg);
        } catch (InvalidProtocolBufferException e) {
            throw new Exceptions.ParsingException(
                format("Failed to serialize a message: {%s}", message), e
            );
        }
    }

    @Override
    protected MediaType contentType() {
        return contentType;
    }

    @Override
    protected String packMultiple(List<String> serializedItems) {
        return Joiner.on('&').join(serializedItems);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Message> T parse(
            @Nullable String body,
            Charset charset,
            Message.Builder builder
    ) {
        if (Strings.isNullOrEmpty(body)) {
            return (T) builder.build();
        }
        Map<String, String> pairs =
                Splitter.on('&').omitEmptyStrings().withKeyValueSeparator('=').split(body).entrySet()
                        .parallelStream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                it -> {
                                    try {
                                        return URLDecoder.decode(it.getValue(), charset.name());
                                    } catch (UnsupportedEncodingException e) {
                                        throw new Exceptions.ParsingException(
                                                format("Failed to serialize a body: {%s}", body), e
                                        );
                                    }
                                }
                        ));
        String json = gson.toJson(pairs);
        return ProtoJsonConverter.INSTANCE.parse(json, charset, builder);
    }
}
