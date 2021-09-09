//
// Created by ISS Mac on 2019-06-28.
//

#include "SingleInstance.h"

SingleInstance* SingleInstance::instance =0;


SingleInstance* SingleInstance::getInstance(){
   if(!instance){
     instance = new SingleInstance();
   }
   return instance;
}