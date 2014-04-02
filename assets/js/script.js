var sendKeyCode = function(keyCode){
    var callback = function(response){ console.log(response); }

    var xhr = new XMLHttpRequest();
    xhr.open('GET', "/mote?keycode=" + keyCode, true);
    xhr.onreadystatechange = function() {
        if (xhr.readyState != 4) return;
        if (xhr.status == 200) {
            callback(xhr.responseText);
        } else {
            callback(null);
        }
    }
    xhr.send( null );
}

var discoverDevices = function(){
    var callback = function(response){	console.log(response);	}

    var xhr = new XMLHttpRequest();
    xhr.open('GET', "/mote?discoverDevices=true", true);
    xhr.onreadystatechange = function() {
        if (xhr.readyState != 4) return;
        if (xhr.status == 200) {
            callback(xhr.responseText);



        } else {
            callback(null);
        }
    }
    xhr.send( null );
}

var isDiscovering = function(){
    var callback = function(response){	console.log(response);	}

    var xhr = new XMLHttpRequest();
    xhr.open('GET', "/mote?isDiscovering=true", true);
    xhr.onreadystatechange = function() {
        if (xhr.readyState != 4) return;
        if (xhr.status == 200) {
            callback(xhr.responseText);
        } else {
            callback(null);
        }
    }
    xhr.send( null );
}

var getDeviceList = function(){
    var callback = function(response){	console.log(response);	}

    var xhr = new XMLHttpRequest();
    xhr.open('GET', "/mote?getDeviceList=true", true);
    xhr.onreadystatechange = function() {
        if (xhr.readyState != 4) return;
        if (xhr.status == 200) {
            try {
                var tvDevices = JSON.parse(xhr.responseText);
                document.getElementById('ip_address_box').value = tvDevices[0].address;

                callback(xhr.responseText);
            } catch(e){console.log('No devices found.');}


        } else {
            callback(null);
        }
    }
    xhr.send( null );
}

var connectDevice = function(ipAddress){
    var callback = function(response){	console.log(response);	}

    var xhr = new XMLHttpRequest();
    xhr.open('GET', "/mote?connectDevice="+ipAddress, true);
    xhr.onreadystatechange = function() {
        if (xhr.readyState != 4) return;
        if (xhr.status == 200) {
            callback(xhr.responseText);
        } else {
            callback(null);
        }
    }
    xhr.send( null );
}

var isAwaitingPin = function(){
    var callback = function(response){	console.log(response);	}

    var xhr = new XMLHttpRequest();
    xhr.open('GET', "/mote?isAwaitingPin=true", true);
    xhr.onreadystatechange = function() {
        if (xhr.readyState != 4) return;
        if (xhr.status == 200) {
            callback(xhr.responseText);
        } else {
            callback(null);
        }
    }
    xhr.send( null );
}

var cancelPairCode = function(){
    var callback = function(response){	console.log(response);	}

    var xhr = new XMLHttpRequest();
    xhr.open('GET', "/mote?cancelPairCode=true", true);
    xhr.onreadystatechange = function() {
        if (xhr.readyState != 4) return;
        if (xhr.status == 200) {
            callback(xhr.responseText);
        } else {
            callback(null);
        }
    }
    xhr.send( null );
}

var sendPairCode = function(pinCode){
    var callback = function(response){	console.log(response);	}

    var xhr = new XMLHttpRequest();
    xhr.open('GET', "/mote?sendPairCode="+pinCode, true);
    xhr.onreadystatechange = function() {
        if (xhr.readyState != 4) return;
        if (xhr.status == 200) {
            callback(xhr.responseText);
        } else {
            callback(null);
        }
    }
    xhr.send( null );
}

var isConnectingNow = function(){
    var callback = function(response){	console.log(response);	}

    var xhr = new XMLHttpRequest();
    xhr.open('GET', "/mote?isConnectingNow=true", true);
    xhr.onreadystatechange = function() {
        if (xhr.readyState != 4) return;
        if (xhr.status == 200) {
            callback(xhr.responseText);
        } else {
            callback(null);
        }
    }
    xhr.send( null );
}

var connectSuccessOrFail = function(){
    var callback = function(response){	console.log(response);	}

    var xhr = new XMLHttpRequest();
    xhr.open('GET', "/mote?connectSuccessOrFail=true", true);
    xhr.onreadystatechange = function() {
        if (xhr.readyState != 4) return;
        if (xhr.status == 200) {
            callback(xhr.responseText);
        } else {
            callback(null);
        }
    }
    xhr.send( null );
}

var fling = function(url){
    var callback = function(response){ console.log(response); }

    var xhr = new XMLHttpRequest();
    xhr.open('GET', "/mote?fling=" + url.replace("#","%23"), true);
    xhr.onreadystatechange = function() {
        if (xhr.readyState != 4) return;
        if (xhr.status == 200) {
            callback(xhr.responseText);
        } else {
            callback(null);
        }
    }
    xhr.send( null );
}
