https://cloud.tencent.com/developer/article/1457066
参考: android源码目录里的system/core/init/readme.txt.


Android系统里以*.rc为扩展名为系统初始化脚本，脚本里就是使用安卓初始化语言.
语句都是一行为一条语句，没有符号分隔. 语句里的每个词用空格隔开.

如: service ueventd /sbin/ueventd
备注语句以符号”#”作为注释.
语言基本上分为4个块: Actions Commands Services 和 Options.
每个Actions和Services关键词开始的语句作为一个新的区块.
Commands和Options就是属于就近的Actions或Services块. 

Actions的形式:
1on <trigger>
2   <command>
3   <command>
4   <command>


Services的形式:
1service <name> <pathname> [ <argument> ]*
2   <option>
3   <option>



每个rc文件里又可以包含其它rc文件:
1import /init.environ.rc
2import /init.usb.rc
3import /init.${ro.hardware}.rc
4import /init.usb.configfs.rc
5import /init.${ro.zygote}.rc
6import /init.trace.rc


Actions里常用的trigger有:
1on early-init
2    <command>
3
4on init
5    <command>
6
7on late-init
8    <command>
9
10on post-fs
11   <command>
12
13on boot
14   <command>
15
16on property:sys.init_log_level=*  //在设置属性值时触发
17   <command>
18
19on charger
20   <command>
21
22on property:sys.powerctl=*
23   <command>


常用的Commands有:
//参考system/core/init/keywords.h
1bootchart_init
2   Start bootcharting if configured (see below).
3   This is included in the default init.rc.
4
5chmod <octal-mode> <path>
6   Change file access permissions.
7
8chown <owner> <group> <path>
9   Change file owner and group.
10
11class_start <serviceclass>
12   Start all services of the specified class if they are
13   not already running.
14
15class_stop <serviceclass>
16   Stop and disable all services of the specified class if they are
17   currently running.
18
19class_reset <serviceclass>
20   Stop all services of the specified class if they are
21   currently running, without disabling them. They can be restarted
22   later using class_start.
23
24copy <src> <dst>
25   Copies a file. Similar to write, but useful for binary/large
26   amounts of data.
27
28domainname <name>
29   Set the domain name.
30
31enable <servicename>
32   Turns a disabled service into an enabled one as if the service did not
33   specify disabled.
34   If the service is supposed to be running, it will be started now.
35   Typically used when the bootloader sets a variable that indicates a specific
36   service should be started when needed. E.g.
37     on property:ro.boot.myfancyhardware=1
38        enable my_fancy_service_for_my_fancy_hardware
39
40exec [ <seclabel> [ <user> [ <group> ]* ] ] -- <command> [ <argument> ]*
41   Fork and execute command with the given arguments. The command starts
42   after "--" so that an optional security context, user, and supplementary
43   groups can be provided. No other commands will be run until this one
44   finishes. <seclabel> can be a - to denote default.
45
46export <name> <value>
47   Set the environment variable <name> equal to <value> in the
48   global environment (which will be inherited by all processes
49   started after this command is executed)
50
51hostname <name>
52   Set the host name.
53
54ifup <interface>
55   Bring the network interface <interface> online.
56
57import <filename>
58   Parse an init config file, extending the current configuration.
59
60insmod <path>
61   Install the module at <path>
62
63load_all_props
64   Loads properties from /system, /vendor, et cetera.
65   This is included in the default init.rc.
66
67load_persist_props
68   Loads persistent properties when /data has been decrypted.
69   This is included in the default init.rc.
70
71loglevel <level>
72   Sets the kernel log level to level. Properties are expanded within <level>.
73
74mkdir <path> [mode] [owner] [group]
75   Create a directory at <path>, optionally with the given mode, owner, and
76   group. If not provided, the directory is created with permissions 755 and
77   owned by the root user and root group. If provided, the mode, owner and group
78   will be updated if the directory exists already.
79
80mount_all <fstab>
81   Calls fs_mgr_mount_all on the given fs_mgr-format fstab.
82
83mount <type> <device> <dir> [ <flag> ]* [<options>]
84   Attempt to mount the named device at the directory <dir>
85   <device> may be of the form mtd@name to specify a mtd block
86   device by name.
87   <flag>s include "ro", "rw", "remount", "noatime", ...
88   <options> include "barrier=1", "noauto_da_alloc", "discard", ... as
89   a comma separated string, eg: barrier=1,noauto_da_alloc
90
91powerctl
92   Internal implementation detail used to respond to changes to the
93   "sys.powerctl" system property, used to implement rebooting.
94
95restart <service>
96   Like stop, but doesn't disable the service.
97
98restorecon <path> [ <path> ]*
99   Restore the file named by <path> to the security context specified
100   in the file_contexts configuration.
101   Not required for directories created by the init.rc as these are
102   automatically labeled correctly by init.
103
104restorecon_recursive <path> [ <path> ]*
105   Recursively restore the directory tree named by <path> to the
106   security contexts specified in the file_contexts configuration.
107
108rm <path>
109   Calls unlink(2) on the given path. You might want to
110   use "exec -- rm ..." instead (provided the system partition is
111   already mounted).
112
113rmdir <path>
114   Calls rmdir(2) on the given path.
115
116setprop <name> <value>
117   Set system property <name> to <value>. Properties are expanded
118   within <value>.
119
120setrlimit <resource> <cur> <max>
121   Set the rlimit for a resource.
122
123start <service>
124   Start a service running if it is not already running.
125
126stop <service>
127   Stop a service from running if it is currently running.
128
129swapon_all <fstab>
130   Calls fs_mgr_swapon_all on the given fstab file.
131
132symlink <target> <path>
133   Create a symbolic link at <path> with the value <target>
134
135sysclktz <mins_west_of_gmt>
136   Set the system clock base (0 if system clock ticks in GMT)
137
138trigger <event>
139   Trigger an event.  Used to queue an action from another
140   action.
141
142verity_load_state
143   Internal implementation detail used to load dm-verity state.
144
145verity_update_state <mount_point>
146   Internal implementation detail used to update dm-verity state and
147   set the partition.<mount_point>.verified properties used by adb remount
148   because fs_mgr can't set them directly itself.
149
150wait <path> [ <timeout> ]
151   Poll for the existence of the given file and return when found,
152   or the timeout has been reached. If timeout is not specified it
153   currently defaults to five seconds.
154
155write <path> <content>
156   Open the file at <path> and write a string to it with write(2).
157   If the file does not exist, it will be created. If it does exist,
158   it will be truncated. Properties are expanded within <content>.



Services里常用的options有
1critical
2  This is a device-critical service. If it exits more than four times in
3  four minutes, the device will reboot into recovery mode.
4
5disabled
6  This service will not automatically start with its class.
7  It must be explicitly started by name.
8
9setenv <name> <value>
10  Set the environment variable <name> to <value> in the launched process.
11
12socket <name> <type> <perm> [ <user> [ <group> [ <seclabel> ] ] ]
13  Create a unix domain socket named /dev/socket/<name> and pass
14  its fd to the launched process.  <type> must be "dgram", "stream" or "seqpacket".
15  User and group default to 0.
16  'seclabel' is the SELinux security context for the socket.
17  It defaults to the service security context, as specified by seclabel or
18  computed based on the service executable file security context.
19
20user <username>
21  Change to username before exec'ing this service.
22  Currently defaults to root.  (??? probably should default to nobody)
23  Currently, if your process requires linux capabilities then you cannot use
24  this command. You must instead request the capabilities in-process while
25  still root, and then drop to your desired uid.
26
27group <groupname> [ <groupname> ]*
28  Change to groupname before exec'ing this service.  Additional
29  groupnames beyond the (required) first one are used to set the
30  supplemental groups of the process (via setgroups()).
31  Currently defaults to root.  (??? probably should default to nobody)
32
33seclabel <seclabel>
34  Change to 'seclabel' before exec'ing this service.
35  Primarily for use by services run from the rootfs, e.g. ueventd, adbd.
36  Services on the system partition can instead use policy-defined transitions
37  based on their file security context.
38  If not specified and no transition is defined in policy, defaults to the init context.
39
40oneshot
41  Do not restart the service when it exits.
42
43class <name>
44  Specify a class name for the service.  All services in a
45  named class may be started or stopped together.  A service
46  is in the class "default" if one is not specified via the
47  class option.
48
49onrestart
50  Execute a Command (see below) when service restarts.
51
52writepid <file...>
53  Write the child's pid to the given files when it forks. Meant for
54  cgroup/cpuset usage.



自定义脚本
1.实现直接使用root用户进入终端, 修改android/out/target/product/tulip-p1/root/init.rc :
//备注使用shell用户进入终端. 会默认使用root用户操作

1service console /system/bin/sh
2     class core
3     console
4     disabled
5 #    user shell
6 #    group shell log
7 #    seclabel u:r:shell:s0

2.实现系统进入后执行自定义的脚本 :
修改android/out/target/product/tulip-p1/root/init.rc , 增加内容:
1service start_mytest /system/bin/mytest.sh    
2    oneshot
3    on property:sys.boot_completed=1
4    start start_mytest

在android/out/target/product/tulip-p1/system/bin目录下，增加mytest.sh, 内容:
1#!/system/bin/sh
2
3echo "mytest ...!" > /dev/ttyS0
4echo "mytest ...!" > /dev/ttyS0
5echo "mytest ...!" > /dev/ttyS0
6echo "mytest ...!" > /dev/ttyS0
7echo "mytest ...!" > /dev/ttyS0
8echo "mytest ...!" > /dev/ttyS0
9echo "mytest ...!" > /dev/ttyS0