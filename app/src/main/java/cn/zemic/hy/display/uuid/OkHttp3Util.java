package cn.zemic.hy.display.uuid;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * http工具类
 *
 * @author copy
 */
public class OkHttp3Util {
    /**
     * 使用volatile关键字防止重排序，因为 new Instance()是一个非原子操作，可能创建一个不完整的实例
     */
    private static volatile OkHttpClient okHttpClient = null;

    private OkHttp3Util() {
    }

    /**
     * 同步get请求（需要在子线程中执行）
     *
     * @param url url
     * @return response
     */
    public static Response doGet(String url) {
        Response response = null;
        //创建OkHttpClient请求对象
        OkHttpClient okHttpClient = getInstance();
        //创建Request
        Request request = new Request.Builder().url(url).build();
        //得到Call对象
        Call call = okHttpClient.newCall(request);
        //执行同步请求
        try {
            response = call.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    public static OkHttpClient getInstance() {
        if (okHttpClient == null) {
            synchronized (OkHttp3Util.class) {
                if (okHttpClient == null) {
                    //okhttp可以缓存数据....指定缓存路径
                    File sdcache = new File(Environment.getExternalStorageDirectory(), "cache");
                    //指定缓存大小
                    int cacheSize = 10 * 1024 * 1024;
                    //构建器
                    okHttpClient = new OkHttpClient.Builder()
                            //连接超时
                            .connectTimeout(5, TimeUnit.SECONDS)
                            //写入超时
                            .writeTimeout(5, TimeUnit.SECONDS)
                            //读取超时
                            .readTimeout(5, TimeUnit.SECONDS)
                            //设置缓存
                            .cache(new Cache(sdcache.getAbsoluteFile(), cacheSize))
                            .build();
                }
            }
        }
        return okHttpClient;
    }

    /**
     * 异步get请求
     *
     * @param url      url
     * @param callback 回调Callback
     */
    public static void doGet(String url, Callback callback) {
        //创建OkHttpClient请求对象
        OkHttpClient okHttpClient = getInstance();
        //创建Request
        Request request = new Request.Builder().url(url).build();
        //得到Call对象
        Call call = okHttpClient.newCall(request);
        //执行异步请求
        call.enqueue(callback);
    }

    /**
     * post请求
     *
     * @param url      url
     * @param params   Map<String, String> params post请求的时候给服务器传的数据
     * @param callback 异步回调
     */
    public static void doPost(String url, Map<String, String> params, Callback callback) {
        //创建OkHttpClient请求对象
        OkHttpClient okHttpClient = getInstance();
        //3.x版本post请求换成FormBody 封装键值对参数
        FormBody.Builder builder = new FormBody.Builder();
        //遍历集合
        for (String key : params.keySet()) {
            builder.add(key, params.get(key));
        }
        //创建Request
        Request request = new Request.Builder().url(url).post(builder.build()).build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(callback);
    }

    /**
     * post请求上传文件....包括图片....流的形式传任意文件...
     *
     * @param url      url
     * @param file     file表示上传的文件
     * @param fileName fileName....文件的名字,,例如aaa.jpg
     * @param params   params ....传递除了file文件 其他的参数放到map集合
     */
    public static void uploadFile(String url, File file, String fileName, Map<String, String> params) {
        //创建OkHttpClient请求对象
        OkHttpClient okHttpClient = getInstance();
        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);

        //参数
        if (params != null) {
            for (String key : params.keySet()) {
                builder.addFormDataPart(key, params.get(key));
            }
        }
        //文件...参数name指的是请求路径中所接受的参数...如果路径接收参数键值是fileeeee,此处应该改变
        builder.addFormDataPart("file", fileName, RequestBody.create(MediaType.parse("application/octet-stream"), file));
        //构建
        MultipartBody multipartBody = builder.build();
        //创建Request
        Request request = new Request.Builder()
                .url(url)
                .post(multipartBody)
                .build();
        //得到Call
        Call call = okHttpClient.newCall(request);
        //执行请求
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //上传成功回调 目前不需要处理
                if (response.isSuccessful()) {
                    String s = response.body().string();
                    Log.e("上传成功", s);
                }
            }
        });
    }

    /**
     * Post请求发送JSON数据....{"name":"zhangsan","pwd":"123456"}
     *
     * @param url        请求Url
     * @param jsonParams 请求的JSON
     * @param callback   请求回调
     */
    public static void doPostJson(String url, String jsonParams, Callback callback) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonParams);
        Request request = new Request.Builder().url(url).post(requestBody).build();
        Call call = getInstance().newCall(request);
        call.enqueue(callback);
    }

    public static void download() {

    }

    /**
     * 下载文件 以流的形式写入的指定文件
     *
     * @param context 上下文
     * @param url     参数er：请求Url
     * @param saveDir 保存文件的文件夹....download
     */
    public static void downloadFile(final Activity context, final String url, final String saveDir) {
        Request request = new Request.Builder().url(url).build();
        Call call = getInstance().newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len;
                FileOutputStream fos = null;
                try {
                    //以字节流的形式拿回响应实体内容
                    is = response.body().byteStream();
                    //apk保存路径
                    final String fileDir = isExistDir(saveDir);
                    //文件
                    File file = new File(fileDir, getNameFromUrl(url));


                    fos = new FileOutputStream(file);
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }
                    fos.flush();
                    context.runOnUiThread(() -> Toast.makeText(context, "下载成功:" + fileDir + "," + getNameFromUrl(url), Toast.LENGTH_SHORT));
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (is != null) {
                        is.close();
                    }
                    if (fos != null) {
                        fos.close();
                    }
                }
            }
        });
    }

    /**
     * 判断下载目录是否存在......并返回绝对路径
     *
     * @param saveDir 保存路径
     * @return savePath
     */
    public static String isExistDir(String saveDir) {
        // 下载位置
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File downloadFile = new File(Environment.getExternalStorageDirectory(), saveDir);
            if (!downloadFile.mkdirs()) {
                try {
                    downloadFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return downloadFile.getAbsolutePath();
        }
        return null;
    }

    /**
     * 从下载连接中解析出文件名
     *
     * @param url 路由地址
     * @return 从下载连接中解析出文件名
     */
    private static String getNameFromUrl(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }

    /**
     * 下载文件 以流的形式把apk写入的指定文件 得到file后进行安装
     *
     * @param context 上下文
     * @param url     参数er：请求Url
     * @param saveDir 保存文件的文件夹....download
     */
    public static void download(final Activity context, final String url, final String saveDir) {
        Request request = new Request.Builder().url(url).build();
        Call call = getInstance().newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                //避免response.body() 使用一次之后 再次调用response.body()为空的情况
                ResponseBody body = response.body();
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len;
                FileOutputStream fos = null;
                try {
                    //以字节流的形式拿回响应实体内容
                    is = body.byteStream();
                    //apk保存路径
                    final String fileDir = isExistDir(saveDir);
                    //文件
                    File file = new File(fileDir, getNameFromUrl(url));
                    //获取文件大小
                    long fileSize = body.contentLength();
                    fos = new FileOutputStream(file);
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }
                    fos.flush();
                    context.runOnUiThread(() ->
                            Toast.makeText(context, "下载成功:" + fileDir + "," + getNameFromUrl(url), Toast.LENGTH_LONG));
                    //apk下载完成后 调用系统的安装方法


                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        intent.setDataAndType(FileProvider.getUriForFile(context, "com.fxs.fileProvider", file), "application/vnd.android.package-archive");
                    } else {
                        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                    }
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    context.startActivity(intent);

                } catch (IOException | NullPointerException e) {
                    e.printStackTrace();
                } finally {
                    if (is != null) {
                        is.close();
                    }
                    if (fos != null) {
                        fos.close();
                    }
                }
            }
        });
    }
}
