package grpcbridge.common;

import grpcbridge.test.proto.SnakeTestServiceGrpc;
import grpcbridge.test.proto.TestSnakeCase;
import io.grpc.stub.StreamObserver;

/**
 * Test Implementation for TestSnakeService
 */
public class TestSnakeService extends SnakeTestServiceGrpc.SnakeTestServiceImplBase {

    @Override
    public void snake(TestSnakeCase.SnakeRequest request, StreamObserver<TestSnakeCase.SnakeResponse> responseObserver) {
        responseObserver.onNext(TestSnakeCase.SnakeResponse.newBuilder().setStatusCode("OK").build
                ());
        responseObserver.onCompleted();
    }
}
