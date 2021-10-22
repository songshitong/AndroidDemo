http://gityuan.com/2016/05/15/event-log/

在调试分析Android的过程中，比较常用的地查看EventLog，非常简洁明了地展现当前Activity各种状态，当然不至于此，比如还有window的信息
日志查看 logcat -b events   b代表buffer


查看am 是不是ActionManager
/system/core/init/init.cpp
ActionManager& am = ActionManager::GetInstance();
697  
698      am.QueueEventTrigger("early-init");
699  
700      // Queue an action that waits for coldboot done so we know ueventd has set up all of /dev...
701      am.QueueBuiltinAction(wait_for_coldboot_done_action, "wait_for_coldboot_done");
702      // ... so that we can start queuing up actions that require stuff from /dev.
703      am.QueueBuiltinAction(mix_hwrng_into_linux_rng_action, "mix_hwrng_into_linux_rng");
704      am.QueueBuiltinAction(set_mmap_rnd_bits_action, "set_mmap_rnd_bits");
705      am.QueueBuiltinAction(keychord_init_action, "keychord_init");
706      am.QueueBuiltinAction(console_init_action, "console_init");
707  
708      // Trigger all the boot actions to get us started.
709      am.QueueEventTrigger("init");