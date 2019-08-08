package com.sergiopaniegoblanco.webrtcexampleapp.constants;

/**
 * Created by sergiopaniegoblanco on 19/02/2018.
 */

public final class JSONConstants {

    public static final String VALUE = "value";
    public static final String PARAMS = "params";
    public static final String METHOD = "method";
    public static final String ID = "id";
    public static final String RESULT = "result";
    public static final String ICE_CANDIDATE = "iceCandidate";
    public static final String PARTICIPANT_JOINED = "participantJoined";
    public static final String PARTICIPANT_PUBLISHED = "participantPublished";
    public static final String PARTICIPANT_LEFT = "participantLeft";
    public static final String SESSION_ID = "sessionId";
    public static final String SDP_ANSWER = "sdpAnswer";
    public static final String METADATA = "metadata";

    // Methods
    public static final String JOIN_ROOM_METHOD = "joinRoom";
    public static final String PUBLISH_VIDEO_METHOD = "publishVideo";
    public static final String PING_METHOD = "ping";
    public static final String ON_ICE_CANDIDATE_METHOD = "onIceCandidate";
    public static final String RECEIVE_VIDEO_METHOD = "receiveVideoFrom";
    public static final String LEAVE_ROOM_METHOD = "leaveRoom";

    private JSONConstants() {}
}
