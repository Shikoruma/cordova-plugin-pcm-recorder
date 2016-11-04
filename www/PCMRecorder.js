function PCMRecorder() {
}

PCMRecorder.prototype.record = function (successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, "PCMRecorder", "record", []);
};

PCMRecorder.prototype.stop = function (successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, "PCMRecorder", "stop", []);
};

PCMRecorder.prototype.playback = function (filepath,successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, "PCMRecorder", "playback", [filepath]);
};

PCMRecorder.install = function () {
  if (!window.plugins) {
    window.plugins = {};
  }
  window.plugins.pcmRecorder = new PCMRecorder();
  return window.plugins.pcmRecorder;
};

cordova.addConstructor(PCMRecorder.install);
