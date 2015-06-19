<font color='red'><b>Draft Text - The information on this page is preliminary.</b></font>

VLC provides a flexible platform for media transcoding and streaming. VLC can accept media input from network and file system sources, and can duplicate, transcode, and encapsulate the input media into any number of output files or streams.

We have been using the Darwin Streaming Server (Apple's QTSS) to generate some multicast streams for testing, but are now experimenting with using VLC as it can generate IPv6 streams while DSS cannot.

## Setup ##

The following procedure was used to install and configure VLC for execution on a Ubuntu server (0.0.0.0 / [0::0]).

  * Upgrade server to Ubuntu 11. This upgrade is required to enable installation of new VLC v2.0.

  * Install VLC using apt-get package installer:

```
> apt-get install vlc
```
Add group 'vlc' and user 'vlc'. Separate user and group is required because VLC does not allow execution as root.

Create a shell script <font color='#000080'>/etc/init.d/vlm</font>, that can be used to start VLC as a VLM broadcast daemon at system startup. The telnet and HTTP interfaces are enabled to allow access for configuring broadcast channels:

```
#!/bin/bash
### BEGIN INIT INFO
# Provides:      VLM 
# Required-Start:  $local_fs $remote_fs $network $syslog
# Required-Stop:   $local_fs $remote_fs $network $syslog
# Default-Start:   2 3 4 5
# Default-Stop:    0 1 6
# Short-Description: Stop/Start Video Lan Manager (VLC Streamer)
### END INIT INFO
#
# description: Video Lan Manager (VLC Streamer)
#
# Start VLC 

start() {
    if [ -f /var/vlm/vlm.pid ]
    then
        PID=$(cat /var/vlm/vlm.pid)
        RUNNING=$(ps aux | grep $PID | grep -v grep | wc -l)
        if [ $RUNNING = 1 ]; then
            echo "VLC daemon is already running"
            exit
        else
           rm -f /var/vlm/vlm.pid
        fi
    fi 
    sudo -u vlc /usr/bin/vlc -v --ignore-config --file-logging --logfile /var/log/vlm.log -d -I telnet --extraintf http --http-port 8088
    echo $(ps aux | grep vlc | grep -v grep | head -n 1 | awk '{ print $2 }') > /var/vlm/vlm.pid
    ### Create the lock file ###
    touch /var/lock/vlm
    logger -t vlm "VLC daemon running at pid $(cat /var/vlm/vlm.pid). Use telnet port 4212 or http port 8088""
    echo "VLC daemon running at pid $(cat /var/vlm/vlm.pid). Use telnet port 4212 or http port 8088""
}
# Restart VLC 
stop() {
    kill -HUP $(cat /var/vlm/vlm.pid)
    rm -f /var/vlm/vlm.pid > /dev/null 2>&1
    ### Now, delete the lock file ###
    rm -f /var/lock/vlm
    logger -t vlm "VLC daemon was stopped."
}
### main logic ###
case "$1" in
  start)
    logger -t vlm "Starting VLC daemon..."
    start
    ;;
  stop)
    logger -t vlm "Stopping VLC daemon... "
    stop
    ;;
  status)
    if [ -f /var/vlm/vlm.pid ]
    then
        PID=$(cat /var/vlm/vlm.pid)
        RUNNING=$(ps aux | grep $PID | grep -v grep | wc -l)
        if [ $RUNNING = 1 ]; then
            echo "VLC daemon is running"
        else
            echo "VLC daemon is not running at pid $PID but a pid file was found and removed."
            rm -f /var/vlm/vlm.pid
        fi
    fi
    ;;
  restart|reload|condrestart)
    stop
    start
    ;;
  *)
    echo "Usage: $0 {start|stop|restart|reload|status}"
    exit 1
esac
exit
```
Use <font color='#000080'>'update-rc.d'</font> utility to create links to script at default startup run-levels:

```
> update-rc.d -f vlm defaults
```

## Usage ##

The telnet interface is used to create and control VOD and broadcast programs. It provides a simple command interface for identifying input(s) and required output. The output options are numerous - see [VLC Advanced Streaming](http://wiki.videolan.org/Documentation:Streaming_HowTo/Advanced_Streaming_Using_the_Command_Line).

The following VLC telnet commands create a broadcast that reads media from a single file and broadcasts it as separate IPv4 and IPv6 multicast streams (using arbitrarily-chosen group addresses). The broadcast descriptions are retrieved using the SDP URLs specified in the output command.

```
> new yosemite_1280x720 broadcast enabled loop
> setup yosemite_1280x720 input /usr/local/movies/yosemite_h264_1280x720_30fps_1500kbs_AAC_44khz_1400mtu.mov
> setup yosemite_1280x720 output #gather:duplicate{dst=rtp{dst=232.0.0.1,port=1234,ttl=64,sdp=http://0.0.0.0:8081/yosemite_1280x720_ipv4.sdp},dst=rtp{dst=[ff3e::d9a2:d7ed],port=1234,ttl=64,sdp=http://0.0.0.0:8081/yosemite_1280x720_ipv6.sdp}}
> setup yosemite_1280x720 option sout-keep
> control yosemite_1280x720 play 1
> show
show
    media : ( 1 broadcast - 0 vod )
        yosemite_1280x720
            type : broadcast
            enabled : yes
            loop : yes
            inputs
                1 : /usr/local/movies/yosemite_h264_1280x720_30fps_1500kbs_AAC_44khz_1400mtu.mov
            output : #duplicate{dst=rtp{dst=232.0.0.1,port=1234,ttl=64,sdp=http://0.0.0.0:8081/yosemite_1280x720_ipv4.sdp},dst=rtp{dst[ff3e::d9a2:d7ed],port=1234,ttl=64,sdp=http://0.0.0.0:8081/yosemite_1280x720_ipv6.sdp}}
            options
                sout-keep
            instances
```
<font color='#800000'>The VLC broadcast setup requires the <font color='#000000'><code>gather</code></font> module, <font color='#000000'><code>sout-keep</code></font> option and explicit control input identifier (e.g. <font color='#000000'><code>control &lt;broadcast&gt; play 1</code></font> to instruct VLC to play the looped playlist as a continuous RTP stream. If you leave any of these parameters out, players such as QT and VLC will stop playing the stream when a playlist entry completes, even though they continue to received RTP packets produced by by the next playlist entry or loop iteration.</font> (GB - I suspect this behavior may be caused by a reset of the RTP sequence number or timestamps in the output stream).

The following tcpdump invocation can be used to verify that these streams are being broadcast:

```
sudo tcpdump -n -i eth0 port 1234
```

An RTSP URL that can be used play a multicast stream via the AMT reflector running on port 5554 on localhost will look something like this:

`rtsp://localhost:5554/reflect?sdp_url=http://149.20.1.154:8088/bigbuckbunny_320x180_ipv4.sdp&relay_address=154.17.0.1&source_address=149.20.1.154`