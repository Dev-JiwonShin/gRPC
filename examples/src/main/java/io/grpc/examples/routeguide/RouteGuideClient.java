// main에서 주석 해제하며 실행.

package io.grpc.examples.routeguide;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.routeguide.RouteGuideGrpc.RouteGuideBlockingStub;
import io.grpc.examples.routeguide.RouteGuideGrpc.RouteGuideStub;
import io.grpc.stub.StreamObserver;
import org.graalvm.compiler.hotspot.replacements.Log;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RouteGuideClient {
    private static final Logger logger = Logger.getLogger(RouteGuideClient.class.getName());
    
    private final RouteGuideBlockingStub blockingStub;
    private final RouteGuideStub asyncStub;
    
    private Random random = new Random();
    private TestHelper testHelper;
    static int cnt = 1;
    
    /**
     Construct client for accessing RouteGuide server using the existing channel.
     */
    public RouteGuideClient (Channel channel) {
        blockingStub = RouteGuideGrpc.newBlockingStub(channel);
        asyncStub = RouteGuideGrpc.newStub(channel);
    }

    /**
     Blocking server-streaming example.
     */
    public void listFeatures (int lowLat, int lowLon, int hiLat, int hiLon) {
        info("*** server - streaming ***");
        Rectangle request = Rectangle.newBuilder().setLo(Point.newBuilder().setLatitude(lowLat).setLongitude(lowLon).build()).setHi(Point.newBuilder().setLatitude(hiLat).setLongitude(hiLon).build()).build();
        Iterator<Feature> features;
        try {
            features = blockingStub.listFeatures(request);
            for (int i = 1; features.hasNext(); i++) {
                Feature feature = features.next();
                info("[server to client] message : " + i);
                if ( testHelper != null ) {
                    testHelper.onMessage(feature);
                }
            }
        } catch (StatusRuntimeException e) {
            warning("RPC failed: {0}", e.getStatus());
            if ( testHelper != null ) {
                testHelper.onRpcError(e);
            }
        }
    }
    
    /**
     Async client-streaming example.
     */
    public void recordRoute (List<Feature> features, int numPoints) throws InterruptedException {
        info("*** client - streaming ***");
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<RouteSummary> responseObserver = new StreamObserver<RouteSummary>() {
            @Override
            public void onNext (RouteSummary summary) {
                info("[server to client] : \"{0}\" ", summary.getFeatureCount());
                
                if ( testHelper != null ) {
                    testHelper.onMessage(summary);
                }
            }
            
            @Override
            public void onError (Throwable t) {
                warning("RecordRoute Failed: {0}", Status.fromThrowable(t));
                if ( testHelper != null ) {
                    testHelper.onRpcError(t);
                }
                finishLatch.countDown();
            }
            
            @Override
            public void onCompleted () {
                // info("Finished RecordRoute");
                finishLatch.countDown();
            }
        };
        
        StreamObserver<Point> requestObserver = asyncStub.recordRoute(responseObserver);
        try {
            // Send numPoints points randomly selected from the features list.
            for (int i = 0; i < numPoints; ++i) {
                int index = random.nextInt(features.size());
                Point point = features.get(index).getLocation();
                info("[client to server] message : \"{0}\" ", i + 1);
                requestObserver.onNext(point);
                // Sleep for a bit before sending the next one.
                Thread.sleep(random.nextInt(1000) + 500);
                if ( finishLatch.getCount() == 0 ) {
                    return;
                }
            }
        } catch (RuntimeException e) {
            // Cancel RPC
            requestObserver.onError(e);
            throw e;
        }
        // Mark the end of requests
        requestObserver.onCompleted();
        
        // Receiving happens asynchronously
        if ( !finishLatch.await(1, TimeUnit.MINUTES) ) {
            warning("recordRoute can not finish within 1 minutes");
        }
    }
    
    /**
     Bi-directional example
     */
    public CountDownLatch routeChat () {
        info("*** Bi-directional example");
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<RouteNote> requestObserver = asyncStub.routeChat(new StreamObserver<RouteNote>() {
            @Override
            public void onNext (RouteNote note) {
                info("[server to client] message : \"{0}\" ", cnt);
                cnt++;
            }
            
            @Override
            public void onError (Throwable t) {
                warning("RouteChat Failed: {0}", Status.fromThrowable(t));
                if ( testHelper != null ) {
                    testHelper.onRpcError(t);
                }
                finishLatch.countDown();
            }
            
            @Override
            public void onCompleted () {
                info("[server to client] message : \"{0}\" ", cnt);
                info("Finished RouteChat");
                finishLatch.countDown();
            }
        });
        
        try {
            RouteNote[] requests = {newNote("First message", 0, 0), newNote("Second message", 0, 10_000_000), newNote("Third message", 10_000_000, 0), newNote("Fourth message", 10_000_000, 10_000_000)};
            
            int i = 1;
            for (RouteNote request: requests) {
                info("[client to server] message : \"{0}\" ", i);
                i++;
                requestObserver.onNext(request);
            }
            info("[client to server] message : \"{0}\" ", i);
            
        } catch (RuntimeException e) {
            // Cancel RPC
            requestObserver.onError(e);
            throw e;
        }
        // Mark the end of requests
        requestObserver.onCompleted();
        
        // return the latch while receiving happens asynchronously
        return finishLatch;
    }
    
    /**
     Issues several different requests and then exits.
     */
    public static void main (String[] args) throws InterruptedException {
        String target = "localhost:8980";
        if ( args.length > 0 ) {
            if ( "--help".equals(args[0]) ) {
                System.err.println("Usage: [target]");
                System.err.println("");
                System.err.println("  target  The server to connect to. Defaults to " + target);
                System.exit(1);
            }
            target = args[0];
        }
        
        List<Feature> features;
        try {
            features = RouteGuideUtil.parseFeatures(RouteGuideUtil.getDefaultFeaturesFile());
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
        
        ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        try {
            RouteGuideClient client = new RouteGuideClient(channel);
            
            
            // *** server-streaming ***
            client.listFeatures(400000000, -750000000, 420000000, -730000000);

            // *** client-streaming ***
            // client.recordRoute(features, 5);
            
            // *** bi-directional ***
            // CountDownLatch finishLatch = client.routeChat();
            // if ( !finishLatch.await(1, TimeUnit.MINUTES) ) {
            //     client.warning("routeChat can not finish within 1 minutes");
            // }
            
            
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
    
    private void info (String msg, Object... params) {
        logger.log(Level.INFO, msg, params);
    }
    
    private void warning (String msg, Object... params) {
        logger.log(Level.WARNING, msg, params);
    }
    
    private RouteNote newNote (String message, int lat, int lon) {
        return RouteNote.newBuilder().setMessage(message).setLocation(Point.newBuilder().setLatitude(lat).setLongitude(lon).build()).build();
    }
    
    /**
     Only used for helping unit test.
     */
    @VisibleForTesting
    interface TestHelper {
        /**
         Used for verify/inspect message received from server.
         */
        void onMessage (Message message);
        
        /**
         Used for verify/inspect error received from server.
         */
        void onRpcError (Throwable exception);
    }
    
}
