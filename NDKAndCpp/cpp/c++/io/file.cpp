//
// Created by ISS Mac on 2019-06-28.
//
#include <stdio.h>
#include <dirent.h>
#include <sys/types.h>

// c语言的文件读写
// 头文件 stdio.h
// 函数原型 FILE* fopen(const char *path,const char *mode)   返回一个后续操作的File,使用fclose关闭
// path 操作的文件路径
// mode 模式    r,w,a,r+,w+,b
//"r"	打开一个用于读取的文件。该文件必须存在。
//"w"	创建一个用于写入的空文件。如果文件名称与已存在的文件相同，则会删除已有文件的内容，文件被视为一个新的空文件。  只能写
//"a"	追加到一个文件。写操作向文件末尾追加数据。如果文件不存在，则创建文件。
//"r+"	打开一个用于更新的文件，可读取也可写入。该文件必须存在。
//"w+"	创建一个用于读写的空文件。
//"a+"	打开一个用于读取和追加的文件。
//b 以binary的形式打开文件

//写文件
//size_t fwrite(const void *ptr, size_t size, size_t nmemb, FILE *stream)
//ptr -- 这是指向要被写入的元素数组的指针。
//size -- 这是要被写入的每个元素的大小，以字节为单位。
//nmemb -- 这是元素的个数，每个元素的大小为 size 字节。
//stream -- 这是指向 FILE 对象的指针，该 FILE 对象指定了一个输出流
//https://baike.baidu.com/item/fwrite/10942398
//写入到文件的哪里？ 这个与文件的打开模式有关，如果是w+，则是从file pointer指向的地址开始写，替换掉之后的内容，文件的长度可以不变，
//stream的位置移动count个数；如果是a+，则从文件的末尾开始添加，文件长度加大。
//fseek对此函数有作用，但是fwrite函数写到用户空间缓冲区，并未同步到文件中，所以修改后要将内存与文件同步可以用fflush（FILE *fp）函数同步。 [3]

//ftell
//ftell()函数返回指定流的当前文件指针的位置。在文件末尾移动文件指针后，我们可以使用ftell()函数获取文件的总大小
//long int ftell(FILE *stream)

//fseek
//fseek()函数用于将文件指针设置为指定的偏移量。它用于将数据写入所需位置的文件
//int fseek(FILE *stream, long int offset, int whence) //SEEK_SET，SEEK_CUR和SEEK_END
//SEEK_SET 设置当前的文件指针   SEEK_END移动到末尾fseek(fd,0,SEEK_END)
//fseek (ptr, -sizeof student1, SEEK_CUR);  从当前位置移动多少距离




//c++ 的文件读写 todo 使用例子
//<iostream>和<fstream>
// ofstream   输出文件流，创建文件并向文件写入信息
// ifstream   输入文件流，从文件读取信息
// fstream    文件流，同时有ofstream和ifstream的功能
int main(){

  return 0;
}

//https://stackoverflow.com/questions/612097/how-can-i-get-the-list-of-files-in-a-directory-using-c-or-c
static void list_dir(const char *path) {
  struct dirent *entry;
  DIR *dir = opendir(path);
  if (dir == nullptr) {
    return;
  }

  while ((entry = readdir(dir)) != nullptr) {
    printf("%s\n",entry->d_name);
  }

  closedir(dir);
}
