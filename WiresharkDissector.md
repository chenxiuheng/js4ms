<font color='red'><b>Draft Text - This page has not yet been formatted.</b></font>

To decode AMT messages in [Wireshark|http://www.wireshark.org/]:

Verify you are running a version that supports Lua scripting (check  "Help->About Wireshark").

Locate the {color:#800080}{**}init.lua{**}{color} file in your Wireshark installation directory:
\- Windows: {color:#800080}{**}C:\Program Files\Wireshark{**}{color}.
\- Mac: {color:#800080}**/Applications/Wireshark.app/Contents/Resources/share/wireshark{**}{color}.

Download or copy **[amt.lua|^amt.lua]** into the same directory as the {color:#800080}{**}init.lua{**}{color} file.

Edit the {color:#800080}{**}init.lua{**}{color} file to enable Lua scripting and load the {color:#800080}{**}amt.lua{**}{color} script:

{code}
- Comment out the following line to enable Lua support.
- disable\_lua = true; do return end;
{code}
You may also need to change the following depending on your environment:
{code}
- If set and we are running with special privileges this setting-- tells whether scripts other than this one are to be run.
run\_user\_scripts\_when\_superuser =true;
{code}
Add the following line at the bottom of the {color:#800080}{**}init.lua{**}{color} file:
{code}
dofile("amt.lua")
{code}
Start Wireshark and capture an AMT message exchange.

Wireshark will automatically dissect the AMT messages as they are captured. The AMT discovery, advertisement and request messages appear as such in the capture. The AMT query, update, and data messages will not be labeled as AMT messages - the capture log will list the IP packets that are encapsulated within these messages (IGMP/MLD or multicast data). The AMT message header will appear in the packet decomposition pane when inspecting these encapsulated packets.

---

[amt.lua|^amt.lua] AMT dissector script for Wireshark