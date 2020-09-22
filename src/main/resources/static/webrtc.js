$(document).ready(function(){

    var url = 'https://' + window.location.host + '/marcopolo';
    var sock = new SockJS(url);

    var stomp = Stomp.over(sock);
    var payload = JSON.stringify({ 'message': 'Marco!' });

    var CALLEE_ARRIVED = JSON.stringify({ 'message': '1', 'type':"callee_arrived" });
    var NEW_ICE_CANDIDATE = JSON.stringify({ 'message': '2', 'type':"new_ice_candidate" });
    var NEW_DESCRIPTION = JSON.stringify({ 'message': '3', 'type':"new_description" });

    var mId = null;

    var localVideo = document.getElementById("local_video");
    var remoteVideo = document.getElementById("remote_video");

    var localStream, localPeerConnection, remotePeerConnection;

    function log(txt) {
        console.log(txt);
    }

    function login() {
        var data = {"user":"user"};
        var postJson = JSON.stringify(data);
        $.ajax({
            type: "post",
            url: "/loginUser",
            dataType : "json",
            contentType: "application/json",
            data: postJson,
            success: function (d) {
                console.log("I'm " + d.sid);
                mId = d.sid;
            }
        });
    }

    function log_error(e) {
         console.log("log_error " + e);
    }

    // handle signals as a caller
    function caller_signal_handler(signal) {

        if (signal.type === "callee_arrived") {
            log("caller_signal_handler:" + $.localPeerConnection);
            $.localPeerConnection.createOffer(
                new_description_created,
                log_error
            );
        } else {

        }
    }

    // handler to process new descriptions
    function new_description_created(description) {
        log("new_description_created: " + description);
        localPeerConnection.setLocalDescription(
            description,
            function () {
                stomp.send("/app/signal/room1", {},
                    JSON.stringify({
                        call_token:"call_token",
                        type:"new_description",
                        message: encodeURIComponent(JSON.stringify(description))
                    }));
                //signaling_server.send(
                //    JSON.stringify({
                //        call_token:call_token,
                //        type:"new_description",
                //        sdp:description
                //    })
                //);
            },
            log_error
        );
    }

    // handle signals as a callee
    function callee_signal_handler(signal) {
        if (signal.type === "new_description") {
            localPeerConnection.setRemoteDescription(
                new RTCSessionDescription($.parseJSON(decodeURIComponent(signal.message))),
                function () {
                    if (localPeerConnection.remoteDescription.type == "offer") {
                        localPeerConnection.createAnswer(new_description_created, log_error);
                    }
                },
                log_error
            );
        }
    }

    function call_signal_handler(signal) {
        if (signal.type === "new_ice_candidate") {
            // Add candidate to the remote PeerConnection
            localPeerConnection.addIceCandidate(new RTCIceCandidate($.parseJSON(decodeURIComponent(signal.message))));
            log("Local ICE candidate: " + $.parseJSON(decodeURIComponent(signal.message)));
        }
    }

    function notifyRoom1(data) {
        var signal = $.parseJSON(data.body);
        // console.log("Received msg:" + signal);
        try {
            // console.log("Receive Msg :" + signal.from + " me:" + mId);
            // Handle Msg from others
            if (signal.from != mId) {
                if (signal.type === "callee_arrived") {
                    console.log("Received callee_arrived msg:" + signal.message);

                    caller_signal_handler(signal);

                } else if (signal.type === "new_ice_candidate") {
                    console.log("Received new_ice_candidate msg:" + signal.message);

                    call_signal_handler(signal);

                } else if (signal.type === "new_description") {
                    console.log("Received new_description msg:" + signal.message);

                    callee_signal_handler(signal);

                } else {
                    // extend with your own signal types here
                }
            }
        } catch(e) {
            console.log(e);
        }
    }

    stomp.connect({}, function(frame) {
        //stomp.subscribe("/user/queue/notifications", notifications);
        stomp.subscribe("/topic/room1", notifyRoom1);
        login();
        console.log("You are now connected.");
    }, function(error){
        console.log("You are now disconnected." + error);
    });

    $("#accept").click(function() {
        stomp.send("/app/signal/room1", {}, CALLEE_ARRIVED);
    });

    $("#start").click(function() {
        start();
    });

    $("#call").click(function() {
        call();
    });

    // Callback in case of success of the getUserMedia() call
    function successCallback(stream) {
        log("Received local stream");
        // Associate the local video element with the retrieved stream
        if (window.URL) {
            try {
    			// Chrome case: URL.createObjectURL() converts a MediaStream to a blob URL
    			localVideo.src = window.URL.createObjectURL(stream);
    		} catch(e) {
    			localVideo.srcObject = stream;
    		}
        } else {
            localVideo.src = stream;
        }
        localStream = stream;
    }

    // Handler to be called as soon as the remote stream becomes available
    function gotRemoteStream(event) {
        log("Received remote stream");
        // Associate the remote video element with the retrieved stream
        if (window.URL) {
            try {
                // Chrome case: URL.createObjectURL() converts a MediaStream to a blob URL
                remoteVideo.src = window.URL.createObjectURL(event.stream);
            } catch(e) {
                remoteVideo.srcObject = event.stream;
            }
        } else {
            // Firefox
            remoteVideo.src = event.stream;
        }
    }

    function gotLocalIceCandidate(event) {
        if (event.candidate) {
            stomp.send("/app/signal/room1", {}, JSON.stringify({ 'message': encodeURIComponent(JSON.stringify(event.candidate)), 'type':"new_ice_candidate" }));
        }
    }

    // Function associated with clicking on the Start button
    // This is the event triggering all other actions
    function start() {
        // Get ready to deal with different browser vendors...
        navigator.getUserMedia = navigator.getUserMedia ||
                                 navigator.webkitGetUserMedia ||
                                 navigator.mozGetUserMedia;
        if (navigator.getUserMedia) {
            navigator.getUserMedia(
                {
                    audio: true,
                    video: true
                },
                successCallback,
                function(error) {
                    log("navigator.getUserMedia error: ", error);
                }
            );
        } else {
            log("navigator.getUserMedia not supported");
        }
    }

    function call() {
        // ...just use them with Chrome
        if (navigator.webkitGetUserMedia) {
            // Log info about video and audio device in use
            if (localStream.getVideoTracks().length > 0) {
                log('Using video device: ' + localStream.getVideoTracks()[0].label);
            }
            if (localStream.getAudioTracks().length > 0) {
                log('Using audio device: ' + localStream.getAudioTracks()[0].label);
            }
        }
        // Chrome
        if (navigator.webkitGetUserMedia) {
            RTCPeerConnection = webkitRTCPeerConnection;
            // Firefox
        } else if (navigator.mozGetUserMedia) {
            RTCPeerConnection = mozRTCPeerConnection;
            RTCSessionDescription = mozRTCSessionDescription;
            RTCIceCandidate = mozRTCIceCandidate;
        }
        log("RTCPeerConnection object: " + RTCPeerConnection);

        var servers = {
            "iceServers": [
                { "url": "stun://123.57.85.31:3478" },
            ]
        };
        // Create the local PeerConnection object
        localPeerConnection = new RTCPeerConnection(servers);
        log("Created local peer connection object localPeerConnection");
        // Add a handler associated with ICE protocol events
        localPeerConnection.onicecandidate = gotLocalIceCandidate;
        localPeerConnection.onaddstream = gotRemoteStream;
        // Add the local stream (as returned by getUserMedia())
        // to the local PeerConnection.
        try {
        localPeerConnection.addStream(localStream);
        } catch(e) {
            log("call " + e);
        }

        $.localPeerConnection = localPeerConnection;

        log("$.localPeerConnection " + $.localPeerConnection);
    }
});
