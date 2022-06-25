


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