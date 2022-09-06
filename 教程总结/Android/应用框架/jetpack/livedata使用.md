
liveData.observe不调用
liveData绑定在lifecycleOwner，当其不在激活状态，不回掉通知
场景 存在三个页面1,2,3  3通知1,2关闭
可以使用liveData.observeForever(Observer<? super T> observer)，脱离生命周期，但是要手动移除


lievedata值保存在viewModel中，如果activity销毁了，然后重进activity，viewmodel没有被gc，值仍然存在，影响新页面的值
```
 loginVm.jumpLogin.observe(this, aBoolean -> {
      Logger.d(TAG + "jumpLogin" + aBoolean.toString());
      if (aBoolean) {
        loginVm.jumpLogin.setValue(false);
        Intent intent = new Intent(AHGetSmsActivity.this, AHLoginActivity.class);
        intent.putExtra(AHLoginActivity.INTENT_PHONE, loginVm.inputPhone.get());
        startActivity(intent);
      }
    });
```