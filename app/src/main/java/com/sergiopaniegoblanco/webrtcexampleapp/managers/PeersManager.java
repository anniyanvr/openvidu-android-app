package com.sergiopaniegoblanco.webrtcexampleapp.managers;

import android.util.Log;
import android.widget.LinearLayout;

import com.neovisionaries.ws.client.WebSocket;
import com.sergiopaniegoblanco.webrtcexampleapp.VideoConferenceActivity;
import com.sergiopaniegoblanco.webrtcexampleapp.RemoteParticipant;
import com.sergiopaniegoblanco.webrtcexampleapp.listeners.CustomWebSocketListener;
import com.sergiopaniegoblanco.webrtcexampleapp.observers.CustomPeerConnectionObserver;
import com.sergiopaniegoblanco.webrtcexampleapp.observers.CustomSdpObserver;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SoftwareVideoDecoderFactory;
import org.webrtc.SoftwareVideoEncoderFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sergiopaniegoblanco on 18/02/2018.
 */

public class PeersManager {

    private final String TAG = "PeersManager";
    private final EglBase rootEglBase = EglBase.create();
    private PeerConnection localPeer;
    private PeerConnectionFactory peerConnectionFactory;
    private CustomWebSocketListener webSocketAdapter;
    private WebSocket webSocket;
    private LinearLayout views_container;
    private AudioTrack localAudioTrack;
    private VideoTrack localVideoTrack;
    private SurfaceViewRenderer localVideoView;
    private VideoCapturer videoCapturerAndroid;
    private VideoConferenceActivity activity;
    private SurfaceTextureHelper surfaceTextureHelper;


    public PeersManager(VideoConferenceActivity activity, LinearLayout views_container, SurfaceViewRenderer localVideoView) {
        this.views_container = views_container;
        this.localVideoView = localVideoView;
        this.activity = activity;
    }

    public PeerConnection getLocalPeer() {
        return localPeer;
    }

    public AudioTrack getLocalAudioTrack() {
        return localAudioTrack;
    }

    public VideoTrack getLocalVideoTrack() {
        return localVideoTrack;
    }

    public PeerConnectionFactory getPeerConnectionFactory() {
        return peerConnectionFactory;
    }

    public void setWebSocket(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    public CustomWebSocketListener getWebSocketAdapter() {
        return webSocketAdapter;
    }

    public void setWebSocketAdapter(CustomWebSocketListener webSocketAdapter) {
        this.webSocketAdapter = webSocketAdapter;
    }

    public WebSocket getWebSocket() {
        return webSocket;
    }

    public void start() {

        final VideoEncoderFactory encoderFactory;
        final VideoDecoderFactory decoderFactory;

        PeerConnectionFactory.InitializationOptions.Builder optionsBuilder = PeerConnectionFactory.InitializationOptions.builder(activity);
        optionsBuilder.setEnableInternalTracer(true);
        PeerConnectionFactory.InitializationOptions opt = optionsBuilder.createInitializationOptions();

        PeerConnectionFactory.initialize(opt);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();


        /*encoderFactory = new DefaultVideoEncoderFactory(
                rootEglBase.getEglBaseContext(), true ,true);
        decoderFactory = new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext());*/


        encoderFactory = new SoftwareVideoEncoderFactory();
        decoderFactory = new SoftwareVideoDecoderFactory();

        peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .setOptions(options)
                .createPeerConnectionFactory();

        createVideoCapturer();

        MediaConstraints constraints = new MediaConstraints();

        AudioSource audioSource = peerConnectionFactory.createAudioSource(constraints);
        localAudioTrack = peerConnectionFactory.createAudioTrack("101", audioSource);

        localVideoTrack.addSink(localVideoView);

        createLocalPeerConnection();
    }

    private void createVideoCapturer() {
        videoCapturerAndroid = createCameraCapturer(new Camera1Enumerator(false));
        surfaceTextureHelper =
                SurfaceTextureHelper.create("CaptureThread", rootEglBase.getEglBaseContext());
        VideoSource videoSource = peerConnectionFactory.createVideoSource(videoCapturerAndroid.isScreencast());
        videoCapturerAndroid.initialize(surfaceTextureHelper, activity.getApplicationContext(), videoSource.getCapturerObserver());
        videoCapturerAndroid.startCapture(1000, 1000, 30);
        localVideoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);
        localVideoTrack.setEnabled(true);
    }

    private VideoCapturer createCameraCapturer(Camera1Enumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // Trying to find front facing camera
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Log.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Log.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    private void createLocalPeerConnection() {
        //we already have video and audio tracks. Now create peerconnections
        final List<PeerConnection.IceServer> iceServers = new ArrayList<>();

        PeerConnection.IceServer iceServer = PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer();
        iceServers.add(iceServer);

        localPeer = peerConnectionFactory.createPeerConnection(iceServers, new CustomPeerConnectionObserver("localPeerCreation") {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                Map<String, String> iceCandidateParams = new HashMap<>();
                iceCandidateParams.put("sdpMid", iceCandidate.sdpMid);
                iceCandidateParams.put("sdpMLineIndex", Integer.toString(iceCandidate.sdpMLineIndex));
                iceCandidateParams.put("candidate", iceCandidate.sdp);
                if (webSocketAdapter.getUserId() != null) {
                    iceCandidateParams.put("endpointName", webSocketAdapter.getUserId());
                    webSocketAdapter.sendJson(webSocket, "onIceCandidate", iceCandidateParams);
                } else {
                    webSocketAdapter.addIceCandidate(iceCandidateParams);
                }
            }
        });
    }

    public void createLocalOffer(MediaConstraints sdpConstraints) {

        localPeer.createOffer(new CustomSdpObserver("localCreateOffer") {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                localPeer.setLocalDescription(new CustomSdpObserver("localSetLocalDesc"), sessionDescription);
                Map<String, String> localOfferParams = new HashMap<>();
                localOfferParams.put("audioActive", "true");
                localOfferParams.put("videoActive", "true");
                localOfferParams.put("doLoopback", "false");
                localOfferParams.put("frameRate", "30");
                localOfferParams.put("hasAudio", "true");
                localOfferParams.put("hasVideo", "true");
                localOfferParams.put("typeOfVideo", "CAMERA");
                localOfferParams.put("videoDimensions", "{\"width\":320, \"height\":240}");
                localOfferParams.put("sdpOffer", sessionDescription.description);
                if (webSocketAdapter.getId() > 1) {
                    webSocketAdapter.sendJson(webSocket, "publishVideo", localOfferParams);
                } else {
                    webSocketAdapter.setLocalOfferParams(localOfferParams);
                }
            }
        }, sdpConstraints);
    }

    public void createRemotePeerConnection(RemoteParticipant remoteParticipant) {
        final List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        PeerConnection.IceServer iceServer = PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer();
        iceServers.add(iceServer);

        PeerConnection remotePeer = peerConnectionFactory.createPeerConnection(iceServers, new CustomPeerConnectionObserver("remotePeerCreation", remoteParticipant) {

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                Map<String, String> iceCandidateParams = new HashMap<>();
                iceCandidateParams.put("sdpMid", iceCandidate.sdpMid);
                iceCandidateParams.put("sdpMLineIndex", Integer.toString(iceCandidate.sdpMLineIndex));
                iceCandidateParams.put("candidate", iceCandidate.sdp);
                iceCandidateParams.put("endpointName", getRemoteParticipant().getId());
                webSocketAdapter.sendJson(webSocket, "onIceCandidate", iceCandidateParams);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);
                activity.gotRemoteStream(mediaStream, getRemoteParticipant());
            }
        });
        MediaStream mediaStream = peerConnectionFactory.createLocalMediaStream("105");
        mediaStream.addTrack(localAudioTrack);
        mediaStream.addTrack(localVideoTrack);
        remotePeer.addStream(mediaStream);
        remoteParticipant.setPeerConnection(remotePeer);
    }


    public void hangup() {
        if (webSocketAdapter != null && localPeer != null) {
            webSocketAdapter.sendJson(webSocket, "leaveRoom", new HashMap<String, String>());
            webSocket.disconnect();
            localPeer.dispose();
            Map<String, RemoteParticipant> participants = webSocketAdapter.getParticipants();
            for (RemoteParticipant remoteParticipant : participants.values()) {
                remoteParticipant.getPeerConnection().close();
                views_container.removeView(remoteParticipant.getView());

            }
        }
        if (localVideoTrack != null) {
            localVideoTrack.removeSink(localVideoView);
            localVideoView.clearImage();
            videoCapturerAndroid.dispose();
        }

        if (surfaceTextureHelper != null) {
            surfaceTextureHelper.dispose();
            surfaceTextureHelper = null;
        }
        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose();
            peerConnectionFactory = null;
        }
        PeerConnectionFactory.stopInternalTracingCapture();
        PeerConnectionFactory.shutdownInternalTracer();

    }
}
