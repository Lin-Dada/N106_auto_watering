package com.example.lindada.myapplication;

import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

//作者：林鹏
//时间：2018/07/19
//N106自动浇水安卓控制APP
//使用前请连接任何一个校内外的WIFI热点
//请配合使用文档进行使用

public class MainActivity extends AppCompatActivity {

    private Button conn_btn,start_btn,stop_btn,clear_btn,status_btn,checktime_btn,
            checkalarm_btn,checklong_btn, settime_btn,setalarm_btn,setlong_btn,setip_btn;
    private Socket socket=null;
    private Handler handler;
    private TextView textview;
    private BufferedReader br;
    private OutputStream os;
    private boolean have_conn=false,have_rec=false;
    private int hour,minute,time=2;
    private String ip;
    private ProgressDialog loadingdialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        conn_btn = (Button) findViewById(R.id.conn_btn);
        start_btn = (Button) findViewById(R.id.start_btn);
        stop_btn = (Button) findViewById(R.id.stop_btn);
        clear_btn = (Button) findViewById(R.id.clear_btn);
        status_btn = (Button) findViewById(R.id.status_btn);
        checktime_btn = (Button) findViewById(R.id.checktime_btn);
        checkalarm_btn = (Button) findViewById(R.id.checkalarm_btn);
        checklong_btn = (Button) findViewById(R.id.checklong_btn);
        settime_btn = (Button) findViewById(R.id.settime_btn);
        setalarm_btn = (Button) findViewById(R.id.setalarm_btn);
        setlong_btn = (Button) findViewById(R.id.setlong_btn);
        setip_btn = (Button) findViewById(R.id.setip_btn);
        textview=(TextView)findViewById(R.id.textview);

        SharedPreferences read = getSharedPreferences("confing.xml", MODE_PRIVATE);  //获取设置中保存的ip值
        String ip1 = read.getString("ip", "");
        if(ip1 == ""|| ip1==null)ip="172.31.100.234";   //如果没被设设置过就取默认值
        else ip=ip1;
        Toast.makeText(MainActivity.this, "当前路由器ip："+ip, Toast.LENGTH_SHORT).show();  //显示当前连接的路由器ip值
        loadingdialog = new ProgressDialog(MainActivity.this);  //初始化“加载中”弹出框
        loadingdialog.setMessage("正在加载中");
        loadingdialog.setCancelable(false);
        HandlerInit();   //初始化后台Handler事件
        ButtonsSetListener();  //设置按钮事件


    }
    public void loadingshow()   //显示“加载中”
    {
        Message msg = handler.obtainMessage(5);
        handler.sendMessage(msg);
    }

    public void loadinghide()   //取消显示“加载中”
    {
        Message msg = handler.obtainMessage(4);
        handler.sendMessage(msg);
    }
    public void HandlerInit() {
        handler = new Handler() {
            public void handleMessage(Message msg)
            {
                super.handleMessage(msg);  //连接成功事件
                if(msg.what == 0) {
                    System.out.println("连接成功");
                    conn_btn.setText("断开连接");
                    textview.setText("设备状态：已连接\n（有消息返回才说明指令发送成功，否则请重试）");
                }
                else if(msg.what == 1)   //收到数据事件
                {
                    System.out.println("收到数据");

                    String str = msg.obj.toString();
                    str=stringDeal(str);  //对收到的数据进行处理
                    if(str!=null){
                        textview.setText(textview.getText().toString()+"\n\r"+str); //若处理后的数据不为null，则让其显示在消息框中
                        have_rec=true;  //标记为true，以便跳出循环重发
                    }

                }
                else if(msg.what == 2)//断开连接事件
                {
                    System.out.println("连接断开");
                    conn_btn.setText("连接设备");
                    textview.setText("设备状态：未连接\n（有消息返回才说明指令发送成功，否则请重试）");
                }
                else if(msg.what == 3)//显示消息专用
                {
                    String str = msg.obj.toString();
                    Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
                }
                else if(msg.what == 4)//隐藏“加载中”
                {
                    loadingdialog.hide();
                }
                else if(msg.what == 5)//显示“加载中”
                {
                    have_rec=false; //标记为false，以便进入循环重发
                    loadingdialog.show();
                }


            }
        };
    }

    public String stringDeal(String str){  //收到的数据处理函数
        if(str.contains("The Time is "))str=str.replaceAll("The Time is","设备当前时间是：");
        else if(str.contains("The Alarm was set as"))str=str.replaceAll("The Alarm was set as","下一次浇水时间是：");
        else if(str.contains("The WaterTime was set as"))str=str.replaceAll("The WaterTime was set as","当前浇水时长是（单位：分钟）：");
        else if(str.contains("The Time was Reset as"))str=str.replaceAll("The Time was Reset as ","时间已更新：" );
        //以上是中英文替换，便于显示

        else if(str.contains("欢迎连接")||str.contains("Wifi Status")||str.contains("~~~~~")||str.contains("查询定时浇水时间"))str=null;
        else if(str.contains("查询浇水时长")||str.contains("查询系统时间")||str.contains("手动浇水")||str.contains("手动停止浇水"))str=null;
        else if(str.contains("查询系统状态")||str.contains("修改WIFI连接")||str.contains("查询WIFI")||str.contains("慎用"))str=null;
        else if(str.contains("设置定时浇水时间")||str.contains("设置浇水时长"))str=null;
        //以上是第一次连接成功的欢迎信息，不需要显示

        return str;
    }

    public void ButtonsSetListener() {
        conn_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (have_conn == false) //如果还没连接浇水系统
                {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("提示");
                builder.setMessage("连接前请确保已连接上相应的wifi");
                builder.setPositiveButton("我已连接，继续", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        loadingdialog.show();

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    socket = new Socket();
                                    int port = 8080; //路由器的端口请设置为8080
                                    SocketAddress addr = new InetSocketAddress(ip, port);
                                    socket.connect(addr, 3000);//超时时间 3000ms
                                    have_conn = true; //若成功更改标记
                                    Message msg = handler.obtainMessage(0);//Handler弹出连接成功的Toast提醒
                                    handler.sendMessage(msg);

                                    br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "gb2312")); //输入流
                                    new Thread(new Runnable() {
                                        public void run() {
                                            while (!socket.isClosed()) {//开一个线程进行接收消息监听
                                                try {
                                                    String line = br.readLine();
                                                    line = URLDecoder.decode(line, "UTF-8");
                                                    System.out.println(line + "：1");
                                                    Message msg = handler.obtainMessage(1);//对收到的结果进行文本显示
                                                    msg.obj = line;
                                                    handler.sendMessage(msg);

                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }

                                            }
                                            have_conn = false;//如果跳出循环说明socket已关闭
                                           // System.out.println("socket已关闭：1");
                                        }
                                    }).start();


                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Message msg = handler.obtainMessage(3);
                                    msg.obj="连接失败！请检查网络是否正确";
                                    handler.sendMessage(msg);
                                }finally {
                                   loadinghide(); //不管是否连接成功，隐藏“加载中”
                                }

                            }
                        }).start();

                    }
                });

                builder.setNegativeButton("返回", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                //    显示出该对话框
                builder.show();
            }
            else if(have_conn==true)
                {
                    if(socket.isClosed()!=true){
                        try {
                            os = socket.getOutputStream();
                            os.close();
                            socket.close();
                            Toast.makeText(MainActivity.this, "连接已关闭", Toast.LENGTH_SHORT).show();
                            Message msg = handler.obtainMessage(2);//Handler弹出Toast提醒
                            handler.sendMessage(msg);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }
        });

        status_btn.setOnClickListener(new View.OnClickListener() {//查询系统当前状态
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            if (socket != null) {
                                if (!socket.isClosed()) {
                                    loadingshow();
                                    int i = 0;
                                    for(i=0;i<20;i++) {//在没收到返回数据之前，自动重发，循环20次
                                        if(have_rec==false) {
                                            String order;
                                            order = "status"; //要发送的指令
                                            os = socket.getOutputStream();
                                            os.write(order.getBytes("UTF-8"));
                                        }else {
                                            have_rec=false; //如果收到了数据，把标记改成false以便下一次发送
                                            break;  //退出循环即可
                                        }
                                        Thread.sleep(300);  //发送间隔为300ms（若日后测试发现该值不合理，过大或过小，可更改）
                                    }
                                    loadinghide();  //跳出循环后，代表循环重发已结束，隐藏“加载中”
                                    if(i == 20){//若是循环了20次，代表没有发送成功，是因为次数过多而跳出的循环
                                        Message msg = handler.obtainMessage(3);  //提示连接超时
                                        msg.obj = "连接超时，请重试！";
                                        handler.sendMessage(msg);
                                    }

                                }
                                else {
                                    Message msg = handler.obtainMessage(3);
                                    msg.obj = "错误！请先连接设备！";
                                    handler.sendMessage(msg);
                                }
                            }
                            else {
                                Message msg = handler.obtainMessage(3);
                                msg.obj = "错误！请先连接设备！";
                                handler.sendMessage(msg);
                            }
                        } catch (IOException e)
                        {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "错误！请先连接设备！", Toast.LENGTH_SHORT).show();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        //以下按钮事件大同小异，不再注释
        //请自行阅读理解

        start_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                if (socket != null) {
                                    if(!socket.isClosed()){
                                        loadingshow();
                                        int i = 0;
                                        for(i=0;i<20;i++) {
                                            if(have_rec==false) {
                                                    String order;
                                                order = "startwater";
                                                os = socket.getOutputStream();
                                                os.write(order.getBytes("UTF-8"));
                                            }else {
                                                have_rec=false;
                                                break;
                                            }
                                            Thread.sleep(300);
                                        }
                                        loadinghide();
                                        if(i == 20){
                                            Message msg = handler.obtainMessage(3);
                                            msg.obj = "连接超时，请重试！";
                                            handler.sendMessage(msg);
                                        }
                                    }
                                    else {
                                        Message msg = handler.obtainMessage(3);
                                        msg.obj = "错误！请先连接设备！";
                                        handler.sendMessage(msg);
                                    }
                                }
                                else {
                                    Message msg = handler.obtainMessage(3);
                                    msg.obj = "错误！请先连接设备！";
                                    handler.sendMessage(msg);
                                }

                            } catch (IOException e)
                            {
                                e.printStackTrace();
                                Toast.makeText(MainActivity.this, "错误！请先连接设备！", Toast.LENGTH_SHORT).show();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
        });

        stop_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            if(socket != null ) {
                                if (!socket.isClosed()) {
                                    loadingshow();
                                    int i = 0;
                                    for(i=0;i<20;i++) {
                                        if(have_rec==false) {
                                            String order;
                                            order = "stopwater";
                                            os = socket.getOutputStream();
                                            os.write(order.getBytes("UTF-8"));
                                        }else {
                                            have_rec=false;
                                            break;
                                        }
                                        Thread.sleep(300);
                                    }
                                    loadinghide();
                                    if(i == 20){
                                        Message msg = handler.obtainMessage(3);
                                        msg.obj = "连接超时，请重试！";
                                        handler.sendMessage(msg);
                                    }
                                }
                                else {
                                    Message msg = handler.obtainMessage(3);
                                    msg.obj = "错误！请先连接设备！";
                                    handler.sendMessage(msg);
                                }
                            }
                            else {
                                Message msg = handler.obtainMessage(3);
                                msg.obj = "错误！请先连接设备！";
                                handler.sendMessage(msg);
                            }

                        } catch (IOException e)
                        {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "错误！请先连接设备！", Toast.LENGTH_SHORT).show();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        clear_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str;
                if(have_conn == true)str="设备状态：已连接\n（有消息返回才说明指令发送成功，否则请重试）";
                else str="设备状态：未连接\n（有消息返回才说明指令发送成功，否则请重试）";
                textview.setText(str);
                Toast.makeText(MainActivity.this, "屏幕已清除！", Toast.LENGTH_SHORT).show();
            }
        });

        checktime_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            if(socket != null ) {
                                if (!socket.isClosed()) {
                                    loadingshow();
                                    int i = 0;
                                    for(i=0;i<20;i++) {
                                        if(have_rec==false) {
                                            String order;
                                            order = "checktime";
                                            os = socket.getOutputStream();
                                            os.write(order.getBytes("UTF-8"));
                                        }else {
                                            have_rec=false;
                                            break;
                                        }
                                        Thread.sleep(300);
                                    }
                                    loadinghide();
                                    if(i == 20){
                                        Message msg = handler.obtainMessage(3);
                                        msg.obj = "连接超时，请重试！";
                                        handler.sendMessage(msg);
                                    }
                                }
                                else {
                                    Message msg = handler.obtainMessage(3);
                                    msg.obj = "错误！请先连接设备！";
                                    handler.sendMessage(msg);
                                }
                            }
                            else {
                                Message msg = handler.obtainMessage(3);
                                msg.obj = "错误！请先连接设备！";
                                handler.sendMessage(msg);
                            }

                        } catch (IOException e)
                        {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "错误！请先连接设备！", Toast.LENGTH_SHORT).show();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        checkalarm_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            if(socket != null ) {
                                if (!socket.isClosed()) {
                                    loadingshow();
                                    int i = 0;
                                    for(i=0;i<20;i++){
                                        if(have_rec==false) {
                                            String order;
                                            order = "checkalarm";
                                            os = socket.getOutputStream();
                                            os.write(order.getBytes("UTF-8"));
                                        }else {
                                            have_rec=false;
                                            break;
                                        }
                                        Thread.sleep(300);
                                    }
                                    loadinghide();
                                    if(i == 20){
                                        Message msg = handler.obtainMessage(3);
                                        msg.obj = "连接超时，请重试！";
                                        handler.sendMessage(msg);
                                    }

                                }
                                else {
                                    Message msg = handler.obtainMessage(3);
                                    msg.obj = "错误！请先连接设备！";
                                    handler.sendMessage(msg);
                                }
                            }
                            else {
                                Message msg = handler.obtainMessage(3);
                                msg.obj = "错误！请先连接设备！";
                                handler.sendMessage(msg);
                            }

                        } catch (IOException e)
                        {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "错误！请先连接设备！", Toast.LENGTH_SHORT).show();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        checklong_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            if(socket != null ) {
                                if (!socket.isClosed()) {
                                    loadingshow();
                                    int i = 0;
                                    for(i=0;i<20;i++) {
                                        if(have_rec==false) {
                                            String order;
                                            order = "checkwatertime";
                                            os = socket.getOutputStream();
                                            os.write(order.getBytes("UTF-8"));
                                        }else {
                                            have_rec=false;
                                            break;
                                        }
                                        Thread.sleep(300);
                                    }
                                    loadinghide();
                                    if(i == 20){
                                        Message msg = handler.obtainMessage(3);
                                        msg.obj = "连接超时，请重试！";
                                        handler.sendMessage(msg);
                                    }
                                }
                                else {
                                    Message msg = handler.obtainMessage(3);
                                    msg.obj = "错误！请先连接设备！";
                                    handler.sendMessage(msg);
                                }
                            }
                            else {
                                Message msg = handler.obtainMessage(3);
                                msg.obj = "错误！请先连接设备！";
                                handler.sendMessage(msg);
                            }

                        } catch (IOException e)
                        {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "错误！请先连接设备！", Toast.LENGTH_SHORT).show();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        settime_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            Date day=new Date();
                            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            String str=df.format(day).replace("-","").replace(" ","").replace(":","");
                            str="settime"+str;
                            System.out.println(str);

                            if(socket != null ){
                                if(!socket.isClosed()) {
                                    loadingshow();
                                    int i = 0;
                                    for(i=0;i<20;i++) {
                                        if(have_rec==false) {
                                            String order;
                                            order = str;
                                            os = socket.getOutputStream();
                                            os.write(order.getBytes("UTF-8"));
                                        }else {
                                            have_rec=false;
                                            break;
                                        }
                                        Thread.sleep(300);
                                    }
                                    loadinghide();
                                    if(i == 20){
                                        Message msg = handler.obtainMessage(3);
                                        msg.obj = "连接超时，请重试！";
                                        handler.sendMessage(msg);
                                    }
                                }
                                else {
                                    Message msg = handler.obtainMessage(3);
                                    msg.obj = "错误！请先连接设备！";
                                    handler.sendMessage(msg);
                                }
                            }
                            else {
                                Message msg = handler.obtainMessage(3);
                                msg.obj = "错误！请先连接设备！";
                                handler.sendMessage(msg);
                            }

                        } catch (IOException e)
                        {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "错误！请先连接设备！", Toast.LENGTH_SHORT).show();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        setalarm_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog dialog = new TimePickerDialog(MainActivity.this, android.app.AlertDialog.THEME_HOLO_LIGHT,new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, final int minutes) {
                        hour=hourOfDay;
                        minute=minutes;

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try{
                                    if(socket != null ) {
                                        if (!socket.isClosed()) {
                                            String order;
                                            order = "setalarm";

                                            if (hour < 10) order = order + "0" + hour;
                                            else order = order + hour;

                                            if (minute < 10) order = order + "0" + minute + "00";
                                            else order = order + minute + "00";
                                            loadingshow();
                                            int i = 0;
                                            for(i=0;i<20;i++) {
                                                if(have_rec==false) {
                                                    os = socket.getOutputStream();
                                                    os.write(order.getBytes("UTF-8"));
                                                }else {
                                                    have_rec=false;
                                                    break;
                                                }
                                                Thread.sleep(300);
                                            }
                                            loadinghide();
                                            if(i == 20){
                                                Message msg = handler.obtainMessage(3);
                                                msg.obj = "连接超时，请重试！";
                                                handler.sendMessage(msg);
                                            }

                                        }
                                        else {
                                            Message msg = handler.obtainMessage(3);
                                            msg.obj = "错误！请先连接设备！";
                                            handler.sendMessage(msg);
                                        }
                                    }
                                    else {
                                        Message msg = handler.obtainMessage(3);
                                        msg.obj = "错误！请先连接设备！";
                                        handler.sendMessage(msg);
                                    }

                                } catch (IOException e)
                                {
                                    e.printStackTrace();
                                    Toast.makeText(MainActivity.this, "错误！请先连接设备！", Toast.LENGTH_SHORT).show();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }
                },19,0,true);
                dialog.setTitle("设置每天浇水时间");
                dialog.show();
            }
        });

        setlong_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("请选择浇水时长");
                final String[] sex = { "1分钟", "2分钟", "3分钟","4分钟","5分钟" };
                // 设置一个单项选择下拉框
                /**
                 * 第一个参数指定我们要显示的一组下拉单选框的数据集合 第二个参数代表索引，指定默认哪一个单选框被勾选上，1表示默认'女' 会被勾选上
                 * 第三个参数给每一个单选项绑定一个监听器
                 */
                builder.setSingleChoiceItems(sex, 1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Toast.makeText(MainActivity.this, "时长为： " + which+" 分钟", Toast.LENGTH_SHORT).show();
                        time=which+1;
                    }
                });
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try{
                                    String str="setwatertime00";
                                    str=str+time;
                                    if(socket != null ) {
                                        if (!socket.isClosed()) {
                                            loadingshow();
                                            int i = 0;
                                            for(i=0;i<20;i++) {
                                                if(have_rec==false) {
                                                    String order;
                                                    order = str;
                                                    os = socket.getOutputStream();
                                                    os.write(order.getBytes("UTF-8"));
                                                    time = 2;
                                                }else {
                                                    have_rec=false;
                                                    break;
                                                }
                                                Thread.sleep(300);
                                            }
                                            loadinghide();
                                            if(i == 20){
                                                Message msg = handler.obtainMessage(3);
                                                msg.obj = "连接超时，请重试！";
                                                handler.sendMessage(msg);
                                            }
                                        }
                                        else {
                                            Message msg = handler.obtainMessage(3);
                                            msg.obj = "错误！请先连接设备！";
                                            handler.sendMessage(msg);
                                        }
                                    }
                                    else {
                                        Message msg = handler.obtainMessage(3);
                                        msg.obj = "错误！请先连接设备！";
                                        handler.sendMessage(msg);
                                    }

                                } catch (IOException e)
                                {
                                    e.printStackTrace();
                                    Toast.makeText(MainActivity.this, "错误！请先连接设备！", Toast.LENGTH_SHORT).show();
                                    time=2;
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();

                    }
                });
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                builder.show();
            }
        });

        setip_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText et = new EditText(MainActivity.this);

                new AlertDialog.Builder(MainActivity.this).setTitle("输入新的路由器ip地址（如:172.31.100.234）")
                        .setMessage("该功能请在专业人员陪同下使用，路由器转发端口请设置为8080")
                        .setView(et)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String ip1 = et.getText().toString();
                                //步骤1：创建一个SharedPreferences.Editor接口对象，ipadd表示要写入的XML文件名，MODE_WORLD_WRITEABLE写操作
                                SharedPreferences.Editor editor = getSharedPreferences("confing.xml", MODE_PRIVATE).edit();
                                //步骤2：将获取过来的值放入文件
                                editor.putString("ip", ip1);
                                //步骤3：提交
                                editor.commit();
                                //Toast.makeText(getApplicationContext(), "ip更改成功："+ip1, Toast.LENGTH_LONG).show();
                                new AlertDialog.Builder(MainActivity.this).setTitle("重要提示！")
                                        .setMessage("ip成功更改为： "+ip1+"\n请重启后生效！")
                                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                            }
                                        })
                                        .show();
                                }
                        })
                        .setNegativeButton("取消", null)
                        .show();



            }
        });
    }




    }


