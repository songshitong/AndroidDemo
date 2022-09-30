
开始扫描
```
09:45:19.576 Blueto...dapter  D  startLeScan(): [6e400001-b5a3-f393-e0a9-e50e24dcca9e]
09:45:19.577 Blueto...dapter  D  isLeEnabled(): ON
09:45:19.577 BluetoothLeImp   D  isRoutingAllowedForScanStub
09:45:19.579 Blueto...canner  D  onScannerRegistered() - status=0 scannerId=9 mScannerId=0
```

连接设备的日志
```
09:40:57.586 Blueto...dapter  D  stopLeScan()
09:40:57.586 Blueto...dapter  D  isLeEnabled(): ON
09:40:57.592 BtGatt...ervice  E  onScanResult appName: com.autohome.aibadge, scannerId: 9
09:40:57.595 BluetoothGatt    D  connect() - device: DC:45:94:CB:DA:95, auto: false, eattSupport: false
09:40:57.595 BluetoothGatt    D  registerApp()
09:40:57.596 BluetoothGatt    D  registerApp() - UUID=bc4c4d82-ab61-4158-98a0-b822cf4ce145
09:40:57.605 BluetoothGatt    D  onClientRegistered() - status=0 clientIf=9
09:40:57.666 BluetoothGatt    D  onClientConnectionState() - status=0 clientIf=9 device=DC:45:94:CB:DA:95
09:40:57.667 BluetoothGatt    D  discoverServices() - device: DC:45:94:CB:DA:95
09:40:57.669 BluetoothGatt    D  onConfigureMTU() - Device=DC:45:94:CB:DA:95 mtu=185 status=0
09:40:58.315 BluetoothGatt    D  onConnectionUpdated() - Device=DC:45:94:CB:DA:95 interval=6 latency=0 timeout=500 status=0
09:40:58.390 BluetoothGatt    D  onSearchComplete() = Device=DC:45:94:CB:DA:95 Status=0
09:40:58.391 BluetoothGatt    D  setCharacteristicNotification() - uuid: 6e400003-b5a3-f393-e0a9-e50e24dcca9e enable: true
```