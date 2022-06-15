

todo databinding的源码
dataBindingViewHolder.getmBinding().setVariable(initBRVariableId(getItemViewType(position)), data);
dataBindingViewHolder.getmBinding().executePendingBindings();

将viewmodel和databinding绑定   VariableId一般是BR.xx
binding.setVariable(initVariableId(), viewModel);
例如xml中引入,VariableId是BR.loginVM
```
 <data>
        <variable
            name="loginVM"
            type="com.autohome.aibadgesdk.login.AHLoginViewModel"/>
    </data>
```