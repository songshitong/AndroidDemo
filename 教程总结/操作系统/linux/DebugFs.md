
https://www.kernel.org/doc/html/latest/filesystems/debugfs.html

Debugfs 是内核开发人员向用户空间提供信息的一种简单方法。开发人员可以将他们想要的任何信息放在那里。 debugfs 文件系统也不能作为用户空间的稳定 ABI；
/sys/kernel/debug
默认情况下，只有 root 用户可以访问 debugfs 根目录
创建文件
```
struct dentry *debugfs_create_dir(const char *name, struct dentry *parent);
struct dentry *debugfs_create_file(const char *name, umode_t mode,
                                   struct dentry *parent, void *data,
                                   const struct file_operations *fops);
```
