package com.zhi.fileupload;

import android.app.Activity;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.zhi.service.UploadLogService;
import com.zhi.utils.StreamTool;
import java.io.File;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.RandomAccessFile;
import java.net.Socket;

public class MainActivity extends Activity {
    private EditText filenameText;
    private TextView resultView;
    private ProgressBar uploadBar;
    private UploadLogService service;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            uploadBar.setProgress(msg.getData().getInt("length"));
            float num = (float) uploadBar.getProgress() / (float) uploadBar.getMax();
            int result = (int)(num * 100);
            resultView.setText(result + "%");
            if(uploadBar.getProgress() == uploadBar.getMax()){
                Toast.makeText(MainActivity.this, R.string.success, 1).show();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        service =  new UploadLogService(this);
        filenameText = (EditText)findViewById(R.id.filename);
        resultView = (TextView)findViewById(R.id.result);
        uploadBar = (ProgressBar)findViewById(R.id.uploadbar);
        Button button = (Button)findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String filename = filenameText.getText().toString();
                if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                    File file = new File(Environment.getExternalStorageDirectory(), filename);
                    if(file.exists()){
                        uploadBar.setMax((int) file.length());
                        uploadFile(file);
                    }else{
                        Toast.makeText(MainActivity.this, R.string.notexsit, Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(MainActivity.this, R.string.sdcarderror, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * 这里的文件上传采用socket的自定义协议方式
     * 请求部分：  Content-Length=1234;filename=fanfan.exe;sourceId=123456677\r\n(sourceId可能为null)
     * 响应部分：  sourceId=123456677;position=0\r\n
     *
     * 先向服务器端发起请求，并得到响应的结果，取出 sourceId 和 position的值
     * 再将文件的指针指向需要上传的位置，再向服务器端上传文件。
     *
     *Socket的ip为本机局域网的ip，因为我是用tomcat服务器，Socket服务器端监听的是8080端口
     * @param file
     */
    private void uploadFile(final File file) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    String sourceId = service.getBindId(file);
                    Socket socket = new Socket("192.168.1.3", 8080);
                    OutputStream outStream = socket.getOutputStream();
                    String head = "Content-Length="+ file.length() + ";filename="+ file.getName()
                            + ";sourceId="+(sourceId!=null ? sourceId : "")+"\r\n";
                    outStream.write(head.getBytes());

                    PushbackInputStream inStream = new PushbackInputStream(socket.getInputStream());
                    String response = StreamTool.readLine(inStream);
                    String[] items = response.split(";");
                    String responseSourceId = items[0].substring(items[0].indexOf("=") + 1);
                    String position = items[1].substring(items[1].indexOf("=")+1);
                    if(sourceId==null){//如果是第一次上传文件，在数据库中不存在该文件所绑定的资源id
                        service.save(responseSourceId, file);
                    }
                    RandomAccessFile fileOutStream = new RandomAccessFile(file, "r");
                    fileOutStream.seek(Integer.valueOf(position));
                    byte[] buffer = new byte[1024];
                    int len = -1;
                    int length = Integer.valueOf(position);
                    while( (len = fileOutStream.read(buffer)) != -1){
                        outStream.write(buffer, 0, len);
                        length += len;//累加已经上传的数据长度
                        Message msg = new Message();
                        msg.getData().putInt("length", length);
                        handler.sendMessage(msg);
                    }
                    if(length == file.length()) service.delete(file);
                    fileOutStream.close();
                    outStream.close();
                    inStream.close();
                    socket.close();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                }
            }
        }).start();
    }
}