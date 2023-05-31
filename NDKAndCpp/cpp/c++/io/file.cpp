//
// Created by ISS Mac on 2019-06-28.
//

// c语言的文件读写
// 头文件 stdio.h
// 函数原型 FILE* fopen(const char *path,const char *mode)
// path 操作的文件路径
// mode 模式    r,w,a,r+,w+

//写文件
//size_t fwrite(const void *ptr, size_t size, size_t nmemb, FILE *stream)
//ptr -- 这是指向要被写入的元素数组的指针。
//size -- 这是要被写入的每个元素的大小，以字节为单位。
//nmemb -- 这是元素的个数，每个元素的大小为 size 字节。
//stream -- 这是指向 FILE 对象的指针，该 FILE 对象指定了一个输出流  一般是文件fd
//https://baike.baidu.com/item/fwrite/10942398
//写入到文件的哪里？ 这个与文件的打开模式有关，如果是w+，则是从file pointer指向的地址开始写，替换掉之后的内容，文件的长度可以不变，
//stream的位置移动count个数；如果是a+，则从文件的末尾开始添加，文件长度加大。
//fseek对此函数有作用，但是fwrite函数写到用户空间缓冲区，并未同步到文件中，所以修改后要将内存与文件同步可以用fflush（FILE *fp）函数同步。 [3]


//c++ 的文件读写
//<iostream>和<fstream>
// ofstream   输出文件流，创建文件并向文件写入信息
// ifstream   输入文件流，从文件读取信息
// fstream    文件流，同时有ofstream和ifstream的功能
int main(){

  return 0;
}

