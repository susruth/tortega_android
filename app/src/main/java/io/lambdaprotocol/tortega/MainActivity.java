package io.lambdaprotocol.tortega;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.lambdaprotocol.rpc.RelayGrpc;
import io.lambdaprotocol.rpc.Request;
import io.lambdaprotocol.rpc.Response;
import io.lambdaprotocol.rpc.Signature;
import io.lambdaprotocol.rpc.UnsignedResponse;

public class MainActivity extends AppCompatActivity {

    final ManagedChannel channel = ManagedChannelBuilder.forAddress("10.1.1.153", 3000) // office
    //final ManagedChannel channel = ManagedChannelBuilder.forAddress("100.113.84.6", 3000) // home
            .usePlaintext(true)
            .build();
    final RelayGrpc.RelayStub async = RelayGrpc.newStub(channel);
    private Account mainAccount = new Account();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("SYNC", "initiating notifications");
        Signature signature = Signature.newBuilder().setSignatory("Wallet").setHash("Hash").build();
        async.notifications(signature, new StreamObserver<Request>() {
            @Override
            public void onNext(Request req) {
                Log.d("ASYNC", "Request received " + req);
                RelayGrpc.RelayBlockingStub sync = RelayGrpc.newBlockingStub(channel);
                Signature signature = Signature.newBuilder().setSignatory("Wallet").build();
                Response res = Response.newBuilder().setResponse(handleRequest(req)).setSignature(signature).build();
                sync.respond(res);
                Log.d("SYNC", "Response sent " + res);
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onCompleted() {
                Log.d("ASYNC", "completed");
            }
        });
    }

    private UnsignedResponse handleRequest(Request req) {
        try{
            String txHash = mainAccount.send(req.getRequest().getPayload());
            return UnsignedResponse.newBuilder().setRequest(req).setPayload(txHash).build();
        }catch (Exception e){
            return UnsignedResponse.newBuilder().setRequest(req).setPayload("Transaction Failed").build();
        }
    }
}
