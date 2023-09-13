




//使用scanner
scanMode相关  https://juejin.cn/post/7046328465108402207
4.1.1 SCAN_MODE_LOW_POWER
这个是Android默认的扫描模式，耗电量最小。如果扫描不再前台，则强制执行此模式。
在这种模式下， Android会扫喵0.5s,暂停4.5s.
4.1.2 SCAN_MODE_BALANCED
平衡模式， 平衡扫描频率和耗电量的关系。
在这种模式下，Android会扫描2s, 暂停3s。 这是一种妥协模式。
4.1.3 SCAN_MODE_LOW_LATENCY
连续不断的扫描， 建议应用在前台时使用。但会消耗比较多的电量。 扫描结果也会比较快一些。
4.1.4 SCAN_MODE_OPPORTUNISTIC
这种模式下， 只会监听其他APP的扫描结果回调。它无法发现你想发现的设备。


```
ScanSettings settings = new ScanSettings.Builder().setCallbackType(
                        ScanSettings.CALLBACK_TYPE_ALL_MATCHES)  //匹配所有
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) //高频率，低延迟适用于前台app
                        .build();
List<ScanFilter> filters = new ArrayList<ScanFilter>();
if (serviceUuids != null && serviceUuids.length > 0) {
    ScanFilter filter =
            new ScanFilter.Builder().setServiceUuid(new ParcelUuid(serviceUuids[0])) //final UUID[] serviceUuids 过滤设备的service uuid
                    .build();
    //filter.setDeviceName(name)过滤设备名称  .setDeviceAddress(address)                
    filters.add(filter);
}
scanner.startScan(filters, settings, 
new ScanCallback() {
      @Override public void onScanResult(int callbackType, ScanResult result) {
        super.onScanResult(callbackType, result);
        BluetoothDevice device = result.getDevice();
        if (null != device) {
              //由于可能扫描到重复的蓝牙设备，通过Set过滤掉重复的设备。 
               //部分设备可能过一段时间改变mac  提示这个设备仍然可以被连接
              //https://stackoverflow.com/questions/36296769/scanning-using-bluetoothlescanner-calls-onscanresult-multiple-times-for-the-same
        }
      }

      @Override public void onBatchScanResults(List<ScanResult> results) {
        super.onBatchScanResults(results);
      }

      @Override public void onScanFailed(int errorCode) {
        super.onScanFailed(errorCode);
        switch (errorCode){
          case SCAN_FAILED_ALREADY_STARTED:
            Logger.e(TAG+" 扫描失败===: 已经开始 ");
          break;
          case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED: //需要关闭重新打开蓝牙bluetoothAdapter.disable()  bluetoothAdapter.enable()
            Logger.e(TAG+" 扫描失败===: app 注册失败 ");
            break;
          case SCAN_FAILED_FEATURE_UNSUPPORTED:
            Logger.e(TAG+" 扫描失败===: 电量扫描不支持 ");
            break;
          case SCAN_FAILED_INTERNAL_ERROR:
            Logger.e(TAG+" 扫描失败===: 内部异常 ");
            break;
          default:
            Logger.e(TAG+" 扫描失败===: 未知错误");
            break;
        }
      }
    }
);
```