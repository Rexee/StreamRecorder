StreamRecorder
===
[![Latest release][badge-release]][Releases]  
**Web interface for Livestreamer [Livestreamer][Livestreamer]**
---

![main][main]

## Description
This is web interface for the [Livestreamer][Livestreamer].  
It allow to track streams in background and when some of tracked stream is go online - start recording it to local file.

**You can start StreamRecorder on your NAS and when you come back home from work, watch recorded streams!**


## Requirements
* [Livestreamer][Livestreamer]
* [Java 8 JRE][Java]


## Set Up
*Windows*  
Just unzip release file in local folder and run "streamrecorder.cmd". In console you will see something like  
**"Server started at address: http://192.168.1.100/streamrecorder"**.  
Open this address in internet browser and you will be in StreamRecorder!  
First of all you should set up StreamRecorder. Fill fields *Livestreamer.exe path* - path to Livestreamer.exe, *output video folder* - path, where video will be recording, and *rescan time* - interval in seconds after which StreamRecorder will be check - if stream online and try to record.

*Other OS*  
Didnt'n tested :).

  [Livestreamer]: https://github.com/chrippa/livestreamer "Livestreamer"
  [Main]: https://cloud.githubusercontent.com/assets/8672252/8272966/87fbe0cc-1861-11e5-8a7c-e5e0f52ec0c3.jpg
  [Releases]: https://github.com/Rexee/StreamRecorder/releases "Releases"
  [Java]: https://java.com/download "Java"
  [badge-release]: https://img.shields.io/badge/Release-1.0-green.svg "Latest release"