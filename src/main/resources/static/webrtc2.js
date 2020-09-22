$(document).ready(function(){

    var url = 'https://' + window.location.host + '/marcopolo';
    var sock = new SockJS(url);
    var stomp = Stomp.over(sock);

    var mId = null;
    var localVideo = document.getElementById("local_video");
    var remoteVideo = document.getElementById("remote_video");
    var localStream, localPeerConnection, dataChannel;

    function log1(arg1, arg2) {
        console.log(arg1, arg2);
        $("#log").html($("#log").html() + "</br>" + arg1 + "</br>" + arg2)
    }

    function log(...a) {
        console.log(a,...a);

        for(var i=0; i<a.length; i++) {
            $("#log").html($("#log").html() + "</br>" + a[i])
        }
    }

    function log_error(e) {
         console.log("log_error " + e);
         $("#log").html($("#log").html() + "</br>" + " Error:" + e)
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
                log("I'm " + d.sid);
                mId = d.sid;
            }
        });
    }

    // handle signals as a caller
    function caller_signal_handler(signal) {

        if (signal.type === "callee_arrived") {
            log("caller_signal_handler:" + localPeerConnection);
            localPeerConnection.createOffer(
                new_description_created,
                log_error
            );
        } else {

        }
    }

    // handle signals as a callee
    function callee_signal_handler(signal) {
        if (signal.type === "new_description") {
            log("callee_signal_handler:" + localPeerConnection);
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
            },
            log_error
        );
    }

    function call_signal_handler(signal) {
        if (signal.type === "new_ice_candidate") {
            log("call_signal_handler:" + localPeerConnection);
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
            log_error(e);
        }
    }

    stomp.connect({}, function(frame) {
        stomp.subscribe("/topic/room1", notifyRoom1);
        login();
        console.log("You are now connected.");
    }, function(error){
        log_error("You are now disconnected." + error);
    });

    // Callback in case of success of the getUserMedia() call
    function gotLocalStream(stream) {
        log("Received local stream");

        // Associate the local video element with the retrieved stream
        if (window.URL) {
            try {
                // Chrome case: URL.createObjectURL() converts a MediaStream to a blob URL
                localVideo.src = window.URL.createObjectURL(stream);
            } catch(e) {
                localVideo.srcObject = stream;
                //log_error("You are now disconnected." + error);
            }
        } else {
            localVideo.src = stream;
        }

        localStream = stream;

        log("After Received local stream, call RtcPeerConnection");

        call();
    }

    // Function associated with clicking on the Start button
    // This is the event triggering all other actions
    function start() {
        log('Getting user media (video) ...');
        navigator.mediaDevices.getUserMedia({
            audio: false,
            video: true
        })
        .then(gotLocalStream)
        .catch(function(e) {
            log_error('getUserMedia() error: ' + e);
            call();
        });
    }

    function call() {

        var servers = {
            "iceServers": [
                { "urls": "stun://123.57.85.31:3478" },
                //{ "urls": "stun://stun.stunprotocol.org:3478"},
                //{ "urls": "stun://stun.voipstunt.com"}
            ]
        };
        try {
            var RTCPeerConnection = window.RTCPeerConnection || window.mozRTCPeerConnection || window.webkitRTCPeerConnection;
            log("Created local peer connection object localPeerConnection" + RTCPeerConnection);
            // Create the local PeerConnection object
            localPeerConnection =  new RTCPeerConnection(servers);
            log("Created local peer connection object localPeerConnection");
            // Add a handler associated with ICE protocol events
            localPeerConnection.onicecandidate = gotLocalIceCandidate;
            localPeerConnection.ondatachannel = gotDataChannel;
            localPeerConnection.onaddstream = function(event){
                log("gotRemoteStream is received");
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
            };

            dataChannel = localPeerConnection.createDataChannel("txt_msg");
            dataChannel.onopen = function() {
                log('local Datachannel is open and ready to be used.');

                dataChannel.send("dc onopen");
            };

            dataChannel.onerror = function (error) {
                log("local Datachannel error:", error);
            };

            dataChannel.onmessage = function (event) {
                log("local Datachannel message:", event.data);
            };

            dataChannel.onclose = function () {
                log("local Datachannel has been closed.");
            };

            // Add the local stream (as returned by getUserMedia())
            // to the local PeerConnection.

            localPeerConnection.addStream(localStream);
        } catch(e) {
            log_error(" call " + e);
        }
    }

    function sendTxtMag(txt) {
        //var dataChannelOptions = {
        //    reliable: false,
        //    maxRetransmitTime: 3000
        //};

        log('local Datachannel: ' + dataChannel);
        dataChannel.send(txt);
    }

    // Handler to be called as soon as the remote stream becomes available
    function gotRemoteStream(event) {
        log("gotRemoteStream is received");
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

    function gotDataChannel(event) {
        log('gotDataChannel is received!');

        event.channel.onopen = function() {
            log('got Datachannel is open and ready to be used.');
        };

        event.channel.onerror = function (error) {
            log("got Datachannel error:", error);
        };

        event.channel.onmessage = function (event) {
            log("got Datachannel message:", event.data);
        };

        event.channel.onclose = function () {
            log("got Datachannel has been closed.");
        };
    }

    $("#accept").click(function() {
        stomp.send("/app/signal/room1", {}, JSON.stringify({ 'message': '', 'type':"callee_arrived" }));
    });

    $("#start").click(function() {
        start();
    });

    $("#txt_msg_btn").click(function() {
        var txt_msg = $("#txt_msg").val();
        log("txt_msg:" + txt_msg);
        sendTxtMag(txt_msg);
    });
});
