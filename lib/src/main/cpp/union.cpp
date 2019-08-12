//
// Created by ISS Mac on 2019-06-27.
//
//共用体   ---节约内存
// 共用体是一种特殊的数据类型，允许您在相同的内存位置存储不同的数据类型。可以定义一个带有多个成员的共用体，但是任何时候只能有一个成员带有值
//   共用体提供了一种使用相同的内存位置的有效方式


union Data{
  int i;
  int f;
  char str[20];
} data;

//共用体内存
// 共用体占用的内存应足够存储共用体中最大的成员，  int 占用4字节， char 20字节   共用体内存为20字节

int main(){
  data.i=1;
      printf("i值 %d\n",data.i);
          printf("共用体大小 %d\n",sizeof(data));

      printf("=====修改后===== %d\n");

  data.f = 20;
  printf("i地址 %d\n",&data.i);
    printf("f地址 %d\n",&data.f);  // i 和f的地址是一样的

      printf("i值 %d\n",data.i);
      printf("f值 %d\n",data.f);

    printf("共用体大小 %d\n",sizeof(data));

  return 0;
}