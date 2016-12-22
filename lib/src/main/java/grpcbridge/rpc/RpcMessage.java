package grpcbridge.rpc;

import static java.lang.String.format;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import grpcbridge.Exceptions.ConfigurationException;
import grpcbridge.route.Variable;
import io.grpc.Metadata;

/**
 * gRPC message (request or response) abstraction. Each message is the
 * request or response protobuf and the headers or trailers metadata.
 */
public class RpcMessage {
    private Message body;
    private final Metadata metadata;

    /**
     * @param body request or response protobuf message
     * @param metadata headers or trailers metadata
     */
    public RpcMessage(Message body, Metadata metadata) {
        this.body = body;
        this.metadata = metadata;
    }

    /**
     * @return request or response protobuf message
     */
    public Message getBody() {
        return body;
    }

    /**
     * @return headers or trailers metadata
     */
    public Metadata getMetadata() {
        return metadata;
    }

    /**
     * Applies the specified variable to the underlying protobuf message.
     *
     * @param var variable to set
     */
    public void setVar(Variable var) {
        Message.Builder start = body.toBuilder();

        Message.Builder current = start;

        for (String segment : var.getFieldPath()) {
            Descriptors.FieldDescriptor field = current.getDescriptorForType().findFieldByName(segment);
            if (field == null) {
                throw new ConfigurationException("Invalid variable path: " + var + ", looking for: " + segment);
            }
            current = current.getFieldBuilder(field);
        }

        Descriptors.FieldDescriptor field = current.getDescriptorForType().findFieldByName(var.getFieldName());
        if (field == null) {
            throw new ConfigurationException("Invalid variable path: " + var);
        }

        if (field.isRepeated()) {
            current.addRepeatedField(field, var.valueAs(field));
        } else {
            current.setField(field, var.valueAs(field));
        }
        this.body = start.build();
    }

    @Override
    public String toString() {
        return format("{%s} %s", body, metadata);
    }
}
