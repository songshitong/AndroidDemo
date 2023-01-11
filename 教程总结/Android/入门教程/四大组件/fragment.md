
fragment传参
```
      val fragment = Fragment()
      val bundle = Bundle()
      bundle.putString(DATA_TYPE, type)
      bundle.putSerializable(KEY_INTENT_STORE_NAME, dept)
      fragment.arguments = bundle
 
//参数获取
arguments?.let {
      it.getString(DATA_TYPE),
      it.getSerializable(KEY_INTENT_STORE_NAME) as Dept
    }      
```


fragment的view查找是从容器开始的
```
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_ahhome, container, false);
    TextView tvAudioRecordStart = view.findViewById(R.id.home_audio_record_start);
    return view;
  }
```

https://blog.csdn.net/fly_with_24/article/details/107075698
onActivityCreated废弃的替代方案
```
override fun onAttach(context: Context) {
    super.onAttach(context)
    requireActivity().lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            // 想做啥做点啥
            owner.lifecycle.removeObserver(this)
        }
    })
}
```