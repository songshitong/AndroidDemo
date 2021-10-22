zygote  ˈzaɪɡəʊt


http://liuwangshu.cn/framework/booting/1-init.html   android7.0
1.init简介
init进程是Android系统中用户空间的第一个进程，作为第一个进程，它被赋予了很多极其重要的工作职责，比如创建zygote(孵化器)和属性服务等。
init进程是由多个源文件共同组成的，这些文件位于源码目录system/core/init。本文将基于Android7.0源码来分析Init进程

2.引入init进程
说到init进程，首先要提到Android系统启动流程的前几步：
1.启动电源以及系统启动
当电源按下时引导芯片代码开始从预定义的地方（固化在ROM）开始执行。加载引导程序Bootloader到RAM，然后执行。
2.引导程序Bootloader
 引导程序是在Android操作系统开始运行前的一个小程序，它的主要作用是把系统OS拉起来并运行。
3.linux内核启动
内核启动时，设置缓存、被保护存储器、计划列表，加载驱动。当内核完成系统设置，它首先在系统文件中寻找”init”文件，然后启动root进程或者系统的第一个进程。
4.init进程启动

init入口函数  main函数
/system/core/init/init.cpp
```int main(int argc, char** argv) {
    if (!strcmp(basename(argv[0]), "ueventd")) {
        return ueventd_main(argc, argv);
    }
    if (!strcmp(basename(argv[0]), "watchdogd")) {
        return watchdogd_main(argc, argv);
    }
    umask(0);
    add_environment("PATH", _PATH_DEFPATH);
    bool is_first_stage = (argc == 1) || (strcmp(argv[1], "--second-stage") != 0);
    //创建文件并挂载
    if (is_first_stage) {
        mount("tmpfs", "/dev", "tmpfs", MS_NOSUID, "mode=0755");
        mkdir("/dev/pts", 0755);
        mkdir("/dev/socket", 0755);
        mount("devpts", "/dev/pts", "devpts", 0, NULL);
        #define MAKE_STR(x) __STRING(x)
        mount("proc", "/proc", "proc", 0, "hidepid=2,gid=" MAKE_STR(AID_READPROC));
        mount("sysfs", "/sys", "sysfs", 0, NULL);
    }
    open_devnull_stdio();
    klog_init();
    klog_set_level(KLOG_NOTICE_LEVEL);
    NOTICE("init %s started!\n", is_first_stage ? "first stage" : "second stage");
    if (!is_first_stage) {
        // Indicate that booting is in progress to background fw loaders, etc.
        close(open("/dev/.booting", O_WRONLY | O_CREAT | O_CLOEXEC, 0000));
        //初始化属性相关资源
        property_init();//1
        process_kernel_dt();
        process_kernel_cmdline();
        export_kernel_boot_props();
    }
 ...
    //启动属性服务
    start_property_service();//2
    const BuiltinFunctionMap function_map;
    Action::set_function_map(&function_map);
    Parser& parser = Parser::GetInstance();
    parser.AddSectionParser("service",std::make_unique<ServiceParser>());
    parser.AddSectionParser("on", std::make_unique<ActionParser>());
    parser.AddSectionParser("import", std::make_unique<ImportParser>());
    //解析init.rc配置文件
    parser.ParseConfig("/init.rc");//3
   ...   
       while (true) {
        if (!waiting_for_exec) {
            am.ExecuteOneCommand();
            restart_processes();
        }
        int timeout = -1;
        if (process_needs_restart) {
            timeout = (process_needs_restart - gettime()) * 1000;
            if (timeout < 0)
                timeout = 0;
        }
        if (am.HasMoreCommands()) {
            timeout = 0;
        }
        bootchart_sample(&timeout);
        epoll_event ev;
        int nr = TEMP_FAILURE_RETRY(epoll_wait(epoll_fd, &ev, 1, timeout));
        if (nr == -1) {
            ERROR("epoll_wait failed: %s\n", strerror(errno));
        } else if (nr == 1) {
            ((void (*)()) ev.data.ptr)();
        }
    }
    return 0;
}
```

解析init.rc
system/core/rootdir/init.rc
```import /init.${ro.zygote}.rc
12 
13 on early-init
14     # Set init and its forked children's oom_adj.
15     write /proc/1/oom_score_adj -1000
```

Android 7.0中对init.rc文件进行了拆分，每个服务一个rc文件。我们要分析的zygote服务的启动脚本则在init.zygoteXX.rc中定义，
这里拿64位处理器为例
system/core/rootdir/init.zygote64.rc
````
service zygote /system/bin/app_process64 -Xzygote /system/bin --zygote --start-system-server
2     class main
3     socket zygote stream 660 root system
4     onrestart write /sys/android_power/request_state wake
5     onrestart write /sys/power/state on
6     onrestart restart audioserver
7     onrestart restart cameraserver
8     onrestart restart media
9     onrestart restart netd
10     writepid /dev/cpuset/foreground/tasks
````
其中service用于通知init进程创建名zygote的进程，这个zygote进程执行程序的路径为/system/bin/app_process64，
后面的则是要传给app_process64的参数。class main指的是zygote的class name为main



5.解析service
```
Parser& parser = Parser::GetInstance();
691      parser.AddSectionParser("service",std::make_unique<ServiceParser>());
692      parser.AddSectionParser("on", std::make_unique<ActionParser>());
693      parser.AddSectionParser("import", std::make_unique<ImportParser>());
694      parser.ParseConfig("/init.rc");
```
ServiceParser解析rc中的service    ActionParser解析on    ImportParser解析import


解析service，会用到两个函数，一个是ParseSection，它会解析service的rc文件，比如上文讲到的init.zygote64.rc，
ParseSection函数主要用来搭建service的架子。另一个是ParseLineSection，用于解析子项。代码如下所示。
system/core/init/service.cpp
```
bool ServiceParser::ParseSection(const std::vector<std::string>& args,
                                 std::string* err) {
    if (args.size() < 3) {
        *err = "services must have a name and a program";
        return false;
    }
    const std::string& name = args[1];
    if (!IsValidName(name)) {
        *err = StringPrintf("invalid service name '%s'", name.c_str());
        return false;
    }
    std::vector<std::string> str_args(args.begin() + 2, args.end());
    service_ = std::make_unique<Service>(name, "default", str_args);//1
    return true;
}

bool ServiceParser::ParseLineSection(const std::vector<std::string>& args,
                                     const std::string& filename, int line,
                                     std::string* err) const {
    return service_ ? service_->HandleLine(args, err) : false;
}
```
注释1处，根据参数，构造出一个service对象，它的classname为”default”。当解析完毕时会调用EndSection：
```
void ServiceParser::EndSection() {
    if (service_) {
        ServiceManager::GetInstance().AddService(std::move(service_));
    }
}
```

接着查看AddService做了什么：
```
void ServiceManager::AddService(std::unique_ptr<Service> service) {
    Service* old_service = FindServiceByName(service->name());
    if (old_service) {
        ERROR("ignored duplicate definition of service '%s'",
              service->name().c_str());
        return;
    }
    services_.emplace_back(std::move(service));//1
}
```
注释1处的代码将service对象加入到services链表中。上面的解析过程总体来讲就是根据参数创建出service对象，然后根据选项域的内容填充service对象，
最后将service对象加入到vector类型的services链表中
std::vector<std::unique_ptr<Service>> services_; 在头文件中/system/core/init/service.h



init启动zygote
讲完了解析service，接下来该讲init是如何启动service，在这里我们主要讲解启动zygote这个service。在zygote的启动脚本中
我们得知zygote的class name为main。在init.rc有如下配置代码：
system/core/rootdir/init.rc
```
on nonencrypted    
    # A/B update verifier that marks a successful boot.  
    exec - root -- /system/bin/update_verifier nonencrypted  
    class_start main         
    class_start late_start 
...    
```
其中class_start是一个COMMAND，对应的函数为do_class_start。我们知道main指的就是zygote，因此class_start main用来启动zygote。
do_class_start函数在builtins.cpp中定义
system/core/init/builtins.cpp
```
static int do_class_start(const std::vector<std::string>& args) {
ServiceManager::GetInstance().
ForEachServiceInClass(args[1], [] (Service* s) { s->StartIfNotDisabled(); });
return 0;
}
```

来查看StartIfNotDisabled做了什么：
system/core/init/service.cpp
```
bool Service::StartIfNotDisabled() {
    if (!(flags_ & SVC_DISABLED)) {
        return Start();
    } else {
        flags_ |= SVC_DISABLED_START;
    }
    return true;
}
```

start方法
```bool Service::Start() {
    flags_ &= (~(SVC_DISABLED|SVC_RESTARTING|SVC_RESET|SVC_RESTART|SVC_DISABLED_START));
    time_started_ = 0;
    if (flags_ & SVC_RUNNING) {//如果Service已经运行，则不启动
        return false;
    }
    bool needs_console = (flags_ & SVC_CONSOLE);
    if (needs_console && !have_console) {
        ERROR("service '%s' requires console\n", name_.c_str());
        flags_ |= SVC_DISABLED;
        return false;
    }
  //判断需要启动的Service的对应的执行文件是否存在，不存在则不启动该Service
    struct stat sb;
    if (stat(args_[0].c_str(), &sb) == -1) {
        ERROR("cannot find '%s' (%s), disabling '%s'\n",
              args_[0].c_str(), strerror(errno), name_.c_str());
        flags_ |= SVC_DISABLED;
        return false;
    }

...
    pid_t pid = fork();//1.fork函数创建子进程
    if (pid == 0) {//运行在子进程中
        umask(077);
        for (const auto& ei : envvars_) {
            add_environment(ei.name.c_str(), ei.value.c_str());
        }
        for (const auto& si : sockets_) {
            int socket_type = ((si.type == "stream" ? SOCK_STREAM :
                                (si.type == "dgram" ? SOCK_DGRAM :
                                 SOCK_SEQPACKET)));
            const char* socketcon =
                !si.socketcon.empty() ? si.socketcon.c_str() : scon.c_str();

            int s = create_socket(si.name.c_str(), socket_type, si.perm,
                                  si.uid, si.gid, socketcon);
            if (s >= 0) {
                PublishSocket(si.name, s);
            }
        }
...
        //2.通过execve执行程序
        if (execve(args_[0].c_str(), (char**) &strs[0], (char**) ENV) < 0) {
            ERROR("cannot execve('%s'): %s\n", args_[0].c_str(), strerror(errno));
        }

        _exit(127);
    }
...
    return true;
}
```
通过注释1和2的代码，我们得知在Start方法中调用fork函数来创建子进程，并在子进程中调用execve执行system/bin/app_process，
这样就会进入frameworks/base/cmds/app_process/app_main.cpp的main函数  
//todo    Android定义了execve可以执行的方法？？
frameworks/base/cmds/app_process/app_main.cpp
```
int main(int argc, char* const argv[])
{
    ...省略对命令的处理  zygote为bool类型
    if (zygote) {
        runtime.start("com.android.internal.os.ZygoteInit", args, zygote);//1
    } else if (className) {
        runtime.start("com.android.internal.os.RuntimeInit", args, zygote);
    } else {
        fprintf(stderr, "Error: no class name or --zygote supplied.\n");
        app_usage();
        LOG_ALWAYS_FATAL("app_process: no class name or --zygote supplied.");
        return 10;
    }
}
```
从注释1处的代码可以得知调用runtime(AppRuntime)的start来启动zygote



属性服务
Windows平台上有一个注册表管理器，注册表的内容采用键值对的形式来记录用户、软件的一些使用信息。即使系统或者软件重启，
它还是能够根据之前在注册表中的记录，进行相应的初始化工作。Android也提供了一个类似的机制，叫做属性服务。
在本文的开始，我们提到在init.cpp代码中和属性服务相关的代码有：
system/core/init/init.cpp
```
property_init();
start_property_service();
```
这两句代码用来初始化属性服务配置并启动属性服务


属性服务初始化与启动
property_init函数具体实现的代码如下所示。
system/core/init/property_service.cpp
```
void property_init() {
    if (__system_property_area_init()) {
        ERROR("Failed to initialize property area\n");
        exit(1);
    }
}
```

__system_property_area_init函数用来初始化属性内存区域。接下来查看start_property_service函数的具体代码：
```
void start_property_service() {
    property_set_fd = create_socket(PROP_SERVICE_NAME, SOCK_STREAM | SOCK_CLOEXEC | SOCK_NONBLOCK,
                                    0666, 0, 0, NULL);//1
    if (property_set_fd == -1) {
        ERROR("start_property_service socket creation failed: %s\n", strerror(errno));
        exit(1);
    }
    listen(property_set_fd, 8);//2
    register_epoll_handler(property_set_fd, handle_property_set_fd);//3
}
```
注释1处用来创建非阻塞的socket。
注释2处调用listen函数对property_set_fd进行监听，这样创建的socket就成为了server，也就是属性服务；
  listen函数的第二个参数设置8意味着属性服务最多可以同时为8个试图设置属性的用户提供服务。
注释3处的代码将property_set_fd放入了epoll句柄中，用epoll来监听property_set_fd：当property_set_fd中有数据到来时，
init进程将用handle_property_set_fd函数进行处理。 在linux新的内核中，epoll用来替换select，epoll最大的好处在于它不会随着监听fd数目的增长而降低效率。
   因为内核中的select实现是采用轮询来处理的，轮询的fd数目越多，自然耗时越多


** 属性服务处理请求**
从上文我们得知，属性服务接收到客户端的请求时，会调用handle_property_set_fd函数进行处理：
system/core/init/property_service.cpp
```
static void handle_property_set_fd()
{  
...
        if(memcmp(msg.name,"ctl.",4) == 0) {
            close(s);
            if (check_control_mac_perms(msg.value, source_ctx, &cr)) {
                handle_control_message((char*) msg.name + 4, (char*) msg.value);
            } else {
                ERROR("sys_prop: Unable to %s service ctl [%s] uid:%d gid:%d pid:%d\n",
                        msg.name + 4, msg.value, cr.uid, cr.gid, cr.pid);
            }
        } else {
            //检查客户端进程权限
            if (check_mac_perms(msg.name, source_ctx, &cr)) {//1
                property_set((char*) msg.name, (char*) msg.value);//2
            } else {
                ERROR("sys_prop: permission denied uid:%d  name:%s\n",
                      cr.uid, msg.name);
            }
            close(s);
        }
        freecon(source_ctx);
        break;
    default:
        close(s);
        break;
    }
}
```
注释1处的代码用来检查客户端进程权限，在注释2处则调用property_set函数对属性进行修改，代码如下所示。
```
int property_set(const char* name, const char* value) {
    int rc = property_set_impl(name, value);
    if (rc == -1) {
        ERROR("property_set(\"%s\", \"%s\") failed\n", name, value);
    }
    return rc;
}
```

property_set函数主要调用了property_set_impl函数：
```
static int property_set_impl(const char* name, const char* value) {
    size_t namelen = strlen(name);
    size_t valuelen = strlen(value);
    if (!is_legal_property_name(name, namelen)) return -1;
    if (valuelen >= PROP_VALUE_MAX) return -1;
    if (strcmp("selinux.reload_policy", name) == 0 && strcmp("1", value) == 0) {
        if (selinux_reload_policy() != 0) {
            ERROR("Failed to reload policy\n");
        }
    } else if (strcmp("selinux.restorecon_recursive", name) == 0 && valuelen > 0) {
        if (restorecon_recursive(value) != 0) {
            ERROR("Failed to restorecon_recursive %s\n", value);
        }
    }
    //从属性存储空间查找该属性
    prop_info* pi = (prop_info*) __system_property_find(name);
    //如果属性存在
    if(pi != 0) {
       //如果属性以"ro."开头，则表示是只读，不能修改，直接返回
        if(!strncmp(name, "ro.", 3)) return -1;
       //更新属性值
        __system_property_update(pi, value, valuelen);
    } else {
       //如果属性不存在则添加该属性
        int rc = __system_property_add(name, namelen, value, valuelen);
        if (rc < 0) {
            return rc;
        }
    }
    /* If name starts with "net." treat as a DNS property. */
    if (strncmp("net.", name, strlen("net.")) == 0)  {
        if (strcmp("net.change", name) == 0) {
            return 0;
        }
      //以net.开头的属性名称更新后，需要将属性名称写入net.change中  
        property_set("net.change", name);
    } else if (persistent_properties_loaded &&
            strncmp("persist.", name, strlen("persist.")) == 0) {
        /*
         * Don't write properties to disk until after we have read all default properties
         * to prevent them from being overwritten by default values.
         */
        write_persistent_property(name, value);
    }
    property_changed(name, value);
    return 0;
}
```
property_set_impl函数主要用来对属性进行修改，并对以ro、net和persist开头的属性进行相应的处理。
到这里，属性服务处理请求的源码就讲到这

8.init进程总结
讲到这，总结起来init进程主要做了三件事：
1.创建一些文件夹并挂载设备
2.初始化和启动属性服务
3.解析init.rc配置文件并启动zygote进程