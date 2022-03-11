手写binder  不借助AndroidStudio

https://www.jianshu.com/p/2bff72c78ef2
定义一个接口
```
public interface IPersonManager extends IInterface {
      List<Person> getPersonList() throws RemoteException;
     void addPerson(Person person) throws RemoteException;
}

```

```
public abstract class PersonManagerImpl extends Binder implements IPersonManager {

//唯一标识用于注册该BInder，用包名+接口名定义
private static final String DESCRIPTOR = "com.binder.aidl.IPersonManager";
//getList方法唯一标识
static final int TRANSACTION_getList = (IBinder.FIRST_CALL_TRANSACTION + 0);
//add方法唯一标识
static final int TRANSACTION_add = (IBinder.FIRST_CALL_TRANSACTION + 1);

public PersonManagerImpl() {
//注册该binder
this.attachInterface(this, DESCRIPTOR);
}


public static IPersonManager asInterface(IBinder obj) {
if ((obj == null)) {
return null;
}
IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
//查询当前进程
if (((iin != null) && (iin instanceof PersonManagerImpl))) {
return (PersonManagerImpl) iin;//当前进程返回IBookManager
}
return new Proxy(obj);//非当前进程返回Proxy
}


@Override
protected boolean onTransact(int code, @NonNull Parcel data, @Nullable Parcel reply,
int flags) throws RemoteException {

    switch (code) {
        case INTERFACE_TRANSACTION: {
            reply.writeString(DESCRIPTOR);
            return true;
        }
        case TRANSACTION_getList: {
            data.enforceInterface(DESCRIPTOR);
            List<Person> _result = this.getPersonList();
            reply.writeNoException();
            reply.writeTypedList(_result);
            return true;
        }
        case TRANSACTION_add: {
            data.enforceInterface(DESCRIPTOR);
            Person person;
            if ((0 != data.readInt())) {
                person = Person.CREATOR.createFromParcel(data);
            } else {
                person = null;
            }
            this.addPerson(person);
            reply.writeNoException();
            return true;
        }
    }


    return super.onTransact(code, data, reply, flags);
}

@Override
public IBinder asBinder() {
return this;
}


public static class Proxy implements IPersonManager {
    private IBinder mRemote;
    public String getInterfaceDescriptor() {
        return DESCRIPTOR;
    }
    public Proxy(IBinder obj) {
        this.mRemote = obj;
    }
    @Override
    public List<Person> getPersonList() throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        List<Person> result;
        try {
            data.writeInterfaceToken(DESCRIPTOR);
            mRemote.transact(TRANSACTION_getList, data, reply, 0);
            reply.readException();
            result = reply.createTypedArrayList(Person.CREATOR);
        } finally {
            reply.recycle();
            data.recycle();
        }
        return result;
    }

    @Override
    public void addPerson(Person person) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(DESCRIPTOR);
            if ((person != null)) {
                data.writeInt(1);
                person.writeToParcel(data, 0);
            } else {
                data.writeInt(0);
            }
            mRemote.transact(TRANSACTION_add, data, reply, 0);
            reply.readException();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    @Override
    public IBinder asBinder() {
        return null;
    }
}
```


server服务端
```
public class RemoteService extends Service {

    public final PersonManagerImpl mBinder = new PersonManagerImpl() {

    @Override
    public List<Person> getPersonList() throws RemoteException {
        return mPersonsList;
    }

    @Override
    public void addPerson(Person person) {
        mPersonsList.add(person);
    }
    };


//适合用于进程间传输的列表类
private CopyOnWriteArrayList<Person> mPersonsList = new CopyOnWriteArrayList<>();


@Override
public void onCreate() {
    super.onCreate();
    mPersonsList.add(new Person("小明"));
    mPersonsList.add(new Person("小李"));
    mPersonsList.add(new Person("小华"));
    Log.d(RemoteService.class.getSimpleName(), "RemoteService 启动了");
}

@Nullable
@Override
public IBinder onBind(Intent intent) {
    return mBinder;
}
```


客户端启动服务 bindService
```
 private ServiceConnection mConnection = new ServiceConnection() {
    //onServiceConnected与onServiceDisconnected都是在主线程中的，所以如果里面如果涉及到服务端的耗时操作那么需要在子线程中进行
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        //获取到IPersonManager对象
        mPersonManager = PersonManagerImpl.asInterface(service);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mPersonManager = null;

    }
}; 
```


```
public class Person implements Parcelable {
    public String name = "张三";


    public Person() {
    }

    public Person(String name) {
        this.name = name;
    }

    public Person(Parcel in) {
        this.name = in.readString();
    }

    public static final Creator<Person> CREATOR = new Creator<Person>() {
        @Override
        public Person createFromParcel(Parcel in) {
            return new Person(in);
        }

        @Override
        public Person[] newArray(int size) {
            return new Person[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
    }
}
```