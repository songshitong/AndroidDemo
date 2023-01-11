https://developer.android.com/guide/topics/connectivity/bluetooth-le?hl=zh-cn  中文版可能落后
https://developer.android.com/guide/topics/connectivity/bluetooth/find-ble-devices  新的版本

Android 4.3 （API 18 ）引入了低功耗蓝牙，应用可以查询周围设备、查询设备的服务、传输信息

关键术语：
1.通用属性配置文件 (GATT) — GATT 配置文件是一种通用规范，内容针对在 BLE 链路上发送和接收称为“属性”的简短数据片段。
   目前所有低功耗应用配置文件均以 GATT 为基础
蓝牙特别兴趣小组 (Bluetooth SIG) 为低功耗设备定义诸多配置文件。配置文件是描述设备如何在特定应用中工作的规范。
一台设备可以实现多个配置文件。例如，一台设备可能包含心率监测仪和电池电量检测器
2.属性协议 (ATT) — 属性协议 (ATT) 是 GATT 的构建基础，二者的关系也被称为 GATT/ATT。ATT 经过优化，可在 BLE 设备上运行。
为此，该协议尽可能少地使用字节。每个属性均由通用唯一标识符 (UUID) 进行唯一标识，后者是用于对信息进行唯一标识的字符串 ID 的 128 位标准化格式。
由 ATT 传输的属性采用特征和服务格式
3. 特征 — 特征包含一个值和 0 至多个描述特征值的描述符。您可将特征理解为类型，后者与类类似。
4. 描述符 — 描述符是描述特征值的已定义属性。例如，描述符可指定人类可读的描述、特征值的可接受范围或特定于特征值的度量单位。
5. Service — 服务是一系列特征。例如，您可能拥有名为“心率监测器”的服务，其中包括“心率测量”等特征。
 您可以在 bluetooth.org 上找到基于 GATT 的现有配置文件和服务的列表


https://developer.android.com/guide/topics/connectivity/bluetooth-le?hl=zh-cn
//只能安装在支持ble的设备
<uses-feature android:name="android.hardware.bluetooth_le" android:required="true"/>

关键代码
1.检查是否支持BLE
```
if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
    Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
    finish();
}
```
2.获取 BluetoothAdapter 可用于检测蓝牙属性是否支持
BluetoothAdapter 代表设备自身的蓝牙适配器（蓝牙无线装置）。整个系统有一个蓝牙适配器，并且您的应用可使用此对象与之进行交互
```
private BluetoothAdapter bluetoothAdapter;
final BluetoothManager bluetoothManager =
        (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
bluetoothAdapter = bluetoothManager.getAdapter();//BluetoothAdapter.getDefaultAdapter();
isDiscovering() 是否扫描设备中
getDiscoverableTimeout() 超时时间
getMaxConnectedAudioDevices() 最大连接音频设备
```
BluetoothAdapter的关键方法
```
public BluetoothLeScanner getBluetoothLeScanner()
 public BluetoothDevice getRemoteDevice(String address)

//扫描设备 可能被废弃了
public boolean startLeScan(BluetoothAdapter.LeScanCallback callback)  //内部调用Scanner的方法
public void stopLeScan(BluetoothAdapter.LeScanCallback callback) 

```


2.启用蓝牙
```
if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    context.startActivity(enableBtIntent);
    //startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    //onActivityResult后进行Ble设备查找scanLeDevice
     //打开蓝牙后自动链接设备 用户可能点击打开，但是只有权限页面打开有回调，点击确定取消没有回调
     //有的手机例如小米，在权限页面打开后返回ResultCode为0(result cancel)
              AHTimeUtil.retryTask(() -> {
                if(bluetooth4Adapter.isEnabled()&& !isDeviceConnect){
                  startLeScan();
                }
              },10,0,3000);
    
}
```
3.查找 BLE 设备
扫描非常耗电，因此您应遵循以下准则：
找到所需设备后，立即停止扫描。
绝对不进行循环扫描，并设置扫描时间限制。之前可用的设备可能已超出范围，继续扫描会耗尽电池电量。
```
public class DeviceScanActivity extends ListActivity {

    private BluetoothAdapter bluetoothAdapter;
    private boolean mScanning;
    private Handler handler;
    private static final long SCAN_PERIOD = 10000;
    ...
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothAdapter.stopLeScan(leScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            bluetoothAdapter.startLeScan(leScanCallback);
        } else {
            mScanning = false;
            bluetoothAdapter.stopLeScan(leScanCallback);
        }
    }
...
}

private LeDeviceListAdapter leDeviceListAdapter;
private BluetoothAdapter.LeScanCallback leScanCallback =
        new BluetoothAdapter.LeScanCallback() {
    @Override
    public void onLeScan(final BluetoothDevice device, int rssi,
            byte[] scanRecord) {
        runOnUiThread(new Runnable() {
           @Override
           public void run() {
               leDeviceListAdapter.addDevice(device);
               leDeviceListAdapter.notifyDataSetChanged();
           }
       });
   }
};
```
BlueBoothDevice代表蓝牙设备  蓝牙设备的相关信息可能为空,可能受距离影响，离得远了就拿不到设备，连接不成功了
 例如getName()返回null 可以备用mac地址
```
public String getName()
public BluetoothGatt connectGatt(Context context, boolean autoConnect, BluetoothGattCallback callback)
```
4.连接到 GATT 服务器
与 BLE 设备交互的第一步便是连接到 GATT 服务器。更具体地说，是连接到设备上的 GATT 服务器。
```
bluetoothGatt = device.connectGatt(this, false, gattCallback);
```
这将连接到由 BLE 设备托管的 GATT 服务器，并返回 BluetoothGatt 实例，然后您可使用该实例执行 GATT 客户端操作
BluetoothGattCallback会有状态变化的监听onConnectionStateChange，onServicesDiscovered服务发现，onCharacteristicRead。。。
```
public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
        
        }else{
          //服务失败
        }
}
```
有些设备，在 onServicesDiscovered 回调中，返回的 status 是 129，133时，在关闭，重新打开蓝牙，无法解决问题时，建议更换设备，
这些问题大多是设备底层gatt 服务异常，重新连接，进行discoverServices();
```
// 出现129，133时。关闭蓝牙
mBluetoothAdapter.disable();
// 关闭蓝牙后，延时1s，重新开启蓝牙
mBluetoothAdapter.enable();
//重新连接
```


BluetoothGatt关键方法
```
public boolean connect()
bluetoothGatt.discoverServices() //发现服务
bluetoothGatt.getServices//获取服务
public boolean readCharacteristic(BluetoothGattCharacteristic characteristic)//读取描述，触发回调 BluetoothGattCallback.onCharacteristicRead 
public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic)
onCharacteristicWrite中处理写入的结果，写入失败进行处理
```
监听到连接成功需要发现服务和获取服务
```
bluetoothGatt.discoverServices() //发现服务
bluetoothGatt.getServices//获取服务
```
5.读取 BLE 属性
当您的 Android 应用成功连接到 GATT 服务器并发现服务后，应用便可在支持的位置读取和写入属性。
```
 for (BluetoothGattService gattService : gattServices) {
    uuid = gattService.getUuid().toString();
    List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
    uuid = gattCharacteristic.getUuid().toString();  
     for (BluetoothGattCharacteristic characteristic : bluetoothGattService.getCharacteristics()) {
       //符合某种特征的uuid
      if (characteristic.getUuid().toString().equals(readUUID)) {  //读特征
        ...
      } else if (characteristic.getUuid().toString().equals(writeUUID)) {  //写特征
        ...
      }
    }            
 }
```
BluetoothGattService关键代码
```
public UUID getUuid()
public List<BluetoothGattCharacteristic> getCharacteristics()
```
BluetoothGattCharacteristic关键代码
代表蓝牙GATT特性   一般有读特征，写特征
GATT特征是用于构建GATT服务的基本数据元素， BluetoothGattService 。 该特性包含一个值以及附加信息和可选的GATT描述符， BluetoothGattDescriptor 。
```
public static final int PERMISSION_READ = 1;
public static final int PERMISSION_WRITE = 16;
public static final int PROPERTY_READ = 2;
public static final int PROPERTY_WRITE = 8;
public UUID getUuid()
public boolean addDescriptor(BluetoothGattDescriptor descriptor)
public boolean setValue(byte[] value)
public byte[] getValue()
```
BluetoothGattDescriptor描述符有
```
public class BluetoothGattDescriptor implements Parcelable {
    public static final byte[] DISABLE_NOTIFICATION_VALUE = new byte[0];
    public static final byte[] ENABLE_INDICATION_VALUE = new byte[0];
    //开启通知
    public static final byte[] ENABLE_NOTIFICATION_VALUE = new byte[0];
    public static final int PERMISSION_READ = 1;
    public static final int PERMISSION_READ_ENCRYPTED = 2;
    public static final int PERMISSION_READ_ENCRYPTED_MITM = 4;
    public static final int PERMISSION_WRITE = 16;
    public static final int PERMISSION_WRITE_ENCRYPTED = 32;
    public static final int PERMISSION_WRITE_ENCRYPTED_MITM = 64;
    public static final int PERMISSION_WRITE_SIGNED = 128;
    public static final int PERMISSION_WRITE_SIGNED_MITM = 256;
}
```
6.接收 GATT 通知
BLE 应用通常会要求在设备上的特定特征发生变化时收到通知
```
bluetoothGatt.setCharacteristicNotification(characteristic, enabled);
...
BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//有的使用BluetoothGattDescriptor.ENABLE_INDICATION_VALUE这个特征
bluetoothGatt.writeDescriptor(descriptor);
```
为某个特征启用通知后，如果远程设备上的特征发生更改，则会触发 onCharacteristicChanged() 回调：
回调给BluetoothGattCallback
7.关闭客户端应用
https://blog.csdn.net/LoveDou0816/article/details/98508612
一般都是先调用disconnect()方法，此时如果断开成功，会回调onConnectionStateChange()方法，在这个方法中我们再调用close()释放资源。但如果我们使用disconnect()方法后立即调用close()，
会导致无法回调onConnectionStateChange()方法。因此我们需要在合适的地方使用close()释放资源。

当应用完成对 BLE 设备的使用后，其应调用 close()，以便系统可以适当地释放资源
```
private void disconnectDevice(){
   if(null != bluetooth4Adapter){
          bluetooth4Adapter.cancelDiscovery();
        }
   if (bluetoothGatt == null) {
        return;
    }
    bluetoothGatt.disconnect();
}

private void close() {
    if (bluetoothGatt == null) {
        return;
    }
    bluetoothGatt.close();
    bluetoothGatt = null;
}
```
9.给蓝牙设备发送命令
```
 boolean b = writeCharacteristic.setValue(bytes);
 //极端情况存在写入失败 还有就是onCharacteristicWrite的status判断是否写入成功
 boolean result = bluetoothGatt.writeCharacteristic(writeCharacteristic);
```
发送无响应的配置
```
characteristic.setWriteType(BluetoothGattCharacteristic.PROPERTY_WRITE);
characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
characteristic.setWriteType(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE);
```


自动重连
```
 public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        //newState可能是BluetoothGatt.STATE_CONNECTING  
        //status
        switch (newState) {
          case BluetoothProfile.STATE_CONNECTED://连接成功
            gatt.discoverServices();
            break;
          case BluetoothProfile.STATE_DISCONNECTED://断开连接
            refreshDeviceCache();
            //先关闭本次，防止重连后多次回调
            close();
            //开始重连的逻辑
            this.bluetoothAdapter.startLeScan(new UUID[] { SERVICE_UUID }, this.leScanCallback);
            //也可以判断此时的状态status
            if (status == 133) {
          //无法连接
           } else if (status == 62) {
          //成功连接没有发现服务断开
          if (onBleConnectListener != null) {
            gatt.close();
            //62没有发现服务 异常断开
            Log.e(TAG, "连接成功服务未发现断开status:" + status);
          }
        } else if (status == 0) {
          if (onBleConnectListener != null) {
            onBleConnectListener.onDisConnectSuccess(gatt, bluetoothDevice, status); //0正常断开 回调
          }
        } else if (status == 8) {
          //因为距离远或者电池无法供电断开连接
          // 已经成功发现服务
        } else if (status == 34) {
        } else {
          //其它断开连接...
        }
            break;
           case BluetoothProfile.STATE_CONNECTING:
            Logger.i(TAG+" 连接中====");
            break;
          case BluetoothProfile.STATE_DISCONNECTING:
            Logger.i(TAG+" 断开连接中 ");
            break;
          default:
            Logger.i(TAG+" 蓝牙状态变更 未知状态："+status);
            break;
        }
      }

      
/**
   * Clears the internal cache and forces a refresh of the services from the	 * remote device.
   */
  public static boolean refreshDeviceCache(BluetoothGatt mBluetoothGatt) {
    if (mBluetoothGatt != null) {
      try {
        BluetoothGatt localBluetoothGatt = mBluetoothGatt;
        Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
        if (localMethod != null) {
          boolean bool =
              ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
          return bool;
        }
      } catch (Exception localException) {
        Log.i("Config", "An exception occured while refreshing device");
      }
    }
    return false;
  }      
```
重连后多次回调问题
https://stackoverflow.com/questions/33274009/how-to-prevent-bluetoothgattcallback-from-being-executed-multiple-times-at-a-tim
注意localBluetoothGatt的关闭

直接扫描对应的设备
this.bluetooth4Adapter.startLeScan(new UUID[]{SERVICE_UUID}, this.leScanCallback);
扫描完成后链接
```
this.leScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
                bluetooth4Adapter.stopLeScan(BleManager.this.leScanCallback);
                BleManager.this.connect(context, bluetoothDevice);
            }
        };
```

gatt获取对应service和getCharacteristic
BluetoothGattService gattService = gatt.getService(SERVICE_UUID);
if(null != gattService){ //获取不到service  vivox20，频繁重启app可能发送
BleManager.this.readGattCharacteristic = gattService.getCharacteristic(READ_UUID);
BleManager.this.writeGattCharacteristic = gattService.getCharacteristic(WRITE_UUID);
}else{
  // 尝试重连
  此时执行gatt.close，此时不回调onConnectionStateChange
  延迟1S后继续扫描
}



监听蓝牙写入
```
      public void onCharacteristicWrite(BluetoothGatt gatt,
          BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
          if (null != characteristic.getValue()) {
            Logger.i("写入成功 " + XGEncode.bytes2Hex(characteristic.getValue()));
          } else {
            Logger.i("写入成功 但是value 是null");
          }
        } else {
          Logger.e("写入失败");
        }
      }
```

蓝牙设备返回
```
BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
  @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                byte[] data = characteristic.getValue();
                BleManager.this.messageService.handleMessage(data);
            }
        };

```

检查设备是否连接
```
public boolean getConnected() {
    //手动close的标记
    if (null == bluetoothGatt) {
      return false;
    }
    BluetoothDevice device = bluetoothGatt.getDevice();
    if (null == device) {
      return false;
    }
    return BluetoothProfile.STATE_CONNECTED == bluetoothManager.getConnectionState(device,
        BluetoothProfile.GATT);
  }
```

https://github.com/aicareles/Android-BLE/blob/f31981c99048321037e023eb4b619947049cf320/core/src/main/java/cn/com/heaton/blelibrary/ble/BleRequestImpl.java
检查设备是否正忙
```
 public boolean isDeviceBusy(T device) {
        boolean state = false;
        try {
            BluetoothGatt gatt = getBluetoothGatt(device.getBleAddress());
            if (gatt != null){
                Field field = gatt.getClass().getDeclaredField("mDeviceBusy");
                field.setAccessible(true);
                state = (boolean) field.get(gatt);
                BleLog.i(TAG, "isDeviceBusy state:"+state);
                return state;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return state;
    }
```