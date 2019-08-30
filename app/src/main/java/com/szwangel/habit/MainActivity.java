package com.szwangel.habit;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.AlarmClock;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.bumptech.glide.Glide;
import com.githang.statusbar.StatusBarCompat;
import com.szwangel.habit.fragment.ChatFragment;
import com.szwangel.habit.fragment.ReviewFragment;
import com.szwangel.habit.service.RetrofitService;
import com.szwangel.habit.utils.FileUtils;
import com.xiasuhuei321.loadingdialog.view.LoadingDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MainActivity extends FragmentActivity implements OnClickListener {
    Context mContext;

    private static final int REQUEST_GALLERY_IMAGE = 10;// 图库选取图片标识请求码
    private static final int REQUEST_CAMERA_IMAGE = 11;// 拍照标识请求码
    private static final int REQUEST_CROP_IMAGE = 12;// 图片裁剪标识请求码

    // 三个导航按钮
    Button buttonOne;
    Button buttonTwo;
    Button buttonThree;

    // 三个Fragment（页面）
    ChatFragment chatFragment;
    ReviewFragment reviewFragment;
    ChatFragment thirdFragment;

    // 页面以及按钮集合
    List<Fragment> fragmentList;
    List<Button> buttonList;

    // 当前选中的项
    int currentTab = -1;

    // 页面容器
    ViewPager mViewPager;
    FragmentManager mFragmentManager;

    // 底部弹窗
    PopupWindow popupWindow;

    View plusMenu;
    ImageView profile;

    SharedPreferences sharedPreferences;
    RetrofitService retrofitService;

    int uid;
    String mCurrentPhotoPath;
    String mLastProfilePath;
    Uri uriTempFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        //设置状态栏的颜色
        StatusBarCompat.setStatusBarColor(this, getResources().getColor(R.color.colorPrimary));
        mContext = MainActivity.this;

        // 未登录时跳转
        sharedPreferences = getSharedPreferences("user_data", MODE_PRIVATE);
        uid = sharedPreferences.getInt("uid", -1);
        if (uid == -1) {
            navigateTo(LoginActivity.class);
            finish();
        } else {
            // 首次打开设置三个默认闹钟
//            if (sharedPreferences.getString("alertTime", "").equals("")) {
//                SharedPreferences.Editor editor = sharedPreferences.edit();
//                editor.putString("alertTime", "7:50");
//                editor.apply();
//
//                createAlarm("【习惯APP】每天总结", 22, 30, -1);
//                createAlarm("【习惯APP】早上复习", 7, 50, -1);
//                createAlarm("【习惯APP】每周总结", 18, 50, 5);
//
//                new AlertDialog.Builder(mContext)
//                        .setTitle("已为您设置三个默认闹钟")
//                        .setMessage("每天总结  每天22:30\n" +
//                                "早上复习  每天07:50\n" +
//                                "每周总结  周六18:50\n\n" +
//                                "可在应用中点击左下角更改闹钟时间\n" +
//                                "如您不需要闹钟，可去系统闹铃里手动关闭")
//                        .setPositiveButton("我知道了", null)
//                        .setCancelable(false)
//                        .show();
//            }

            initView();
            initListener();
            initPermission();

            mLastProfilePath = sharedPreferences.getString("mLastProfilePath", "");
            File file = new File(mLastProfilePath);
            if (file.exists()) {
                Bitmap bitmap; //从本地取图片
                try {
                    bitmap = BitmapFactory.decodeStream(new FileInputStream(file));
                    if (bitmap != null) {
                        profile.setImageBitmap(bitmap); //设置Bitmap为头像
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

            MyApplication application = (MyApplication) getApplication();
            String domain = application.getDomain();
            retrofitService = new Retrofit.Builder()
                    .baseUrl(domain)
                    .build()
                    .create(RetrofitService.class);

            updateProfile();
            changeView(0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//        initPermission();// 针对6.0以上版本做权限适配，流氓式提醒，体验很差
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_one:
                changeView(0);
                break;
            case R.id.btn_two:
                changeView(1);
                break;
            case R.id.btn_three:
                changeView(2);
                break;
            case R.id.user:
                showPopupWindow();
                break;
            case R.id.btn_pop_album:
                if (checkPermission()) {
                    Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(i, REQUEST_GALLERY_IMAGE);
                }
                popupWindow.dismiss();
                break;
            case R.id.btn_pop_camera:
                takePhoto();
                popupWindow.dismiss();
                break;
            case R.id.btn_pop_cancel:
                popupWindow.dismiss();
                break;
            case R.id.bubblemenu:
                showBubbleMenu();
                break;
            case R.id.menuSearch:
                navigateTo(SearchActivity.class);
                popupWindow.dismiss();
                break;
            case R.id.menuKeyword:
                navigateTo(KeywordActivity.class);
                popupWindow.dismiss();
                break;
            case R.id.menuHereAndNow:
                navigateTo(HereAndNowActivity.class);
                popupWindow.dismiss();
                break;
            case R.id.timeButton:
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_GALLERY_IMAGE && null != data) {
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                mCurrentPhotoPath = cursor.getString(columnIndex);
                cropPhoto();
                cursor.close();
            } else if (requestCode == REQUEST_CAMERA_IMAGE) {
                cropPhoto();
            } else if (requestCode == REQUEST_CROP_IMAGE) {
                // MIUI无法通过return-data返回bitmap，需要特殊处理
//                if ( null != data) {
//                mCurrentPhotoPath = saveBitmap((Bitmap) data.getExtras().get("data"));
//                }

                // 已经输出成文件，无需再次保存
//                try {
//                    mCurrentPhotoPath = saveBitmap(BitmapFactory.decodeStream(getContentResolver().openInputStream(uriTempFile)));
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                }
                mCurrentPhotoPath = uri2path(uriTempFile);
                uploadProfile(mCurrentPhotoPath);

                //TODO 失效
                // 更新聊天窗口的头像
                FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
                fragmentTransaction.detach(chatFragment);
                fragmentTransaction.attach(chatFragment).commit();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 666) {
            for (int permission : grantResults) {
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    Toast toast = Toast.makeText(mContext, "请同意应用正常运行所需权限", Toast.LENGTH_SHORT);
                    toast.setText("请同意应用正常运行所需权限");
                    toast.show();
                }
            }

            // 用户勾选不再提示
            if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
                    || !shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
                    || !shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                try {
                    Intent intent = new Intent();
                    intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);

                    Toast toast = Toast.makeText(mContext, "您禁止了某些权限并勾选了不再提醒，未获得权限前某些功能可能无法正常运行", Toast.LENGTH_LONG);
                    toast.setText("您禁止了某些权限并勾选了不再提醒，未获得权限前某些功能可能无法正常运行");
                    toast.show();
                } catch (Exception ignored) {
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //    @CallSuper
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View view = getCurrentFocus();
            if (isShouldHideKeyBord(view, event)) {
                hideSoftInput(view.getWindowToken());
            }
        }
        return super.dispatchTouchEvent(event);
    }

    // intent的Extra传递失败，此处只能拿到-1
//    @Override
//    protected void onNewIntent(Intent intent) {
//        setIntent(intent);
//        int tab = intent.getIntExtra("tab", -1);
//        Log.e("tab",tab+"set");
//        super.onNewIntent(intent);
//    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        // 闹钟跳转
//        Intent intent = getIntent();
//        int tab = intent.getIntExtra("tab", -1);
//        Log.e("tab",tab+"get");
//        if (tab != -1) {
//            changeView(tab);
//
//            new AlertDialog.Builder(mContext)
//                    .setTitle("闹钟时间到")
//                    .setMessage("定时总结，养成习惯")
//                    .setPositiveButton("好的", null)
//                    .setCancelable(false)
//                    .show();
//
//            // 重复设定闹钟
//            Intent newAlarm = new Intent(mContext, RingReceiver.class);
//            newAlarm.setAction("com.kowah.habit.Ring");
//            newAlarm.putExtra("tab", tab);
//            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, tab, newAlarm, PendingIntent.FLAG_UPDATE_CURRENT);
//            AlarmManager alarmManager = (AlarmManager) mContext.getApplicationContext().getSystemService(Service.ALARM_SERVICE);
//            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent);
//        }
//    }

    private void initView() {
        buttonOne = findViewById(R.id.btn_one);
        buttonTwo = findViewById(R.id.btn_two);
        buttonThree = findViewById(R.id.btn_three);
        profile = findViewById(R.id.user);
        plusMenu = findViewById(R.id.bubblemenu);

        buttonList = new ArrayList<>();
        buttonList.add(buttonOne);
        buttonList.add(buttonTwo);
        buttonList.add(buttonThree);

        Bundle bundle = new Bundle();
        bundle.putInt("tab", 0);
        chatFragment = new ChatFragment();
        chatFragment.setArguments(bundle);
        reviewFragment = new ReviewFragment();
        thirdFragment = new ChatFragment();

        fragmentList = new ArrayList<>();
        fragmentList.add(chatFragment);
        fragmentList.add(reviewFragment);
        fragmentList.add(thirdFragment);

        mFragmentManager = getSupportFragmentManager();
        mViewPager = findViewById(R.id.viewpager);
//        mViewPager.setOffscreenPageLimit(2);
        mViewPager.setAdapter(new MyFragmentStatePagerAdapter(mFragmentManager));
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }

            @Override
            public void onPageSelected(int i) {
                // 更新按钮颜色
                buttonOne.setTextColor(getColor(R.color.colorText));
                buttonTwo.setTextColor(getColor(R.color.colorText));
                buttonThree.setTextColor(getColor(R.color.colorText));
                buttonList.get(i).setTextColor(getColor(R.color.colorPrimary));
            }
        });
    }

    private void initListener() {
        buttonOne.setOnClickListener(this);
        buttonTwo.setOnClickListener(this);
        buttonThree.setOnClickListener(this);
        profile.setOnClickListener(this);
        plusMenu.setOnClickListener(this);
        plusMenu.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    plusMenu.setBackground(getDrawable(R.color.colorText));
                    plusMenu.setAlpha(0.3F);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    plusMenu.setBackground(getDrawable(R.color.colorPrimary));
                    plusMenu.setAlpha(1);
                }
                return false;
            }
        });
    }

    private void initPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //检查权限
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //请求权限
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 777);
            } else {
//                flag = true;
            }
        } else {
//            flag = true;
        }
    }

    // 自定义ViewPager适配器
    class MyFragmentStatePagerAdapter extends FragmentStatePagerAdapter {

        MyFragmentStatePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        // 每次更新完成ViewPager的内容后，调用该接口，此处复写主要是为了让导航按钮上层的覆盖层能够动态的移动
        @Override
        public void finishUpdate(ViewGroup container) {
            super.finishUpdate(container);// 这句话要放在最前面，否则会报错
            // 获取当前的视图是位于ViewGroup的第几个位置，用来更新对应的覆盖层所在的位置
            int currentItem = mViewPager.getCurrentItem();
            if (currentItem == currentTab) {
                return;
            }

            currentTab = mViewPager.getCurrentItem();
        }
    }

    // 手动设置ViewPager要显示的视图
    private void changeView(int tab) {
        mViewPager.setCurrentItem(tab, true);
    }

    // 页面跳转
    private void navigateTo(Class activity) {
        Intent intent = new Intent(mContext, activity);
        startActivity(intent);
    }

    // 更新头像
    private void updateProfile() {
        Call<ResponseBody> responseBodyCall = retrofitService.profile(uid);

        responseBodyCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                String fileName = response.raw().headers().get("Content-Disposition");
                if (fileName != null && fileName.contains("fileName=")) {
                    fileName = "profile_" + fileName.substring(fileName.indexOf("=") + 1);

                    File pic = new File(FileUtils.dirPath + File.separator + fileName);
                    if (!pic.exists()) {
                        //建立一个文件
                        final File file = FileUtils.createFile(mContext, fileName);

                        //下载文件后更新UI，放在UI线程
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                FileUtils.writeFile2Disk(response, file);

                                if (file.exists()) {
                                    Glide.with(mContext)
                                            .load(file)
                                            .into(profile);
                                    mLastProfilePath = file.getAbsolutePath();

                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("mLastProfilePath", mLastProfilePath);
                                    editor.apply();
                                }
                            }
                        });
                    } else {
                        Glide.with(mContext)
                                .load(pic)
                                .into(profile);
                        mLastProfilePath = pic.getAbsolutePath();

                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("mLastProfilePath", mLastProfilePath);
                        editor.apply();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    // 上传头像
    private void uploadProfile(String picPath) {
        final LoadingDialog loadingDialog = new LoadingDialog(this);
        loadingDialog.setLoadingText("上传中,请稍等...")
                .setSuccessText("上传成功")
                .setFailedText("上传头像失败，请稍后再试！")
                .setInterceptBack(true)
                .setRepeatCount(0)
                .show();

        // 防止重复上传
        if (picPath.equals(mLastProfilePath)) {
            loadingDialog.setFailedText("请勿重复上传！");
            loadingDialog.loadFailed();
            return;
        }

        File file = new File(picPath);
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part pic = MultipartBody.Part.createFormData("pic", file.getName(), requestFile);
        Call<ResponseBody> call = retrofitService.uploadProfile(uid, pic);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                String json;
                try {
                    json = response.body().string();
                    JSONObject jsonObject = JSONObject.parseObject(json);
                    if (jsonObject.getInteger("retcode").equals(0)) {
                        loadingDialog.loadSuccess();
                        updateProfile();
                    } else {
                        loadingDialog.loadFailed();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    loadingDialog.loadFailed();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                loadingDialog.loadFailed();
                Toast toast = Toast.makeText(mContext, "网络异常，请稍后重试", Toast.LENGTH_SHORT);
                toast.setText("网络异常，请稍后重试");
                toast.show();
            }
        });
    }

    // 弹窗选择相册或拍照
    private void showPopupWindow() {
        View popupView = View.inflate(mContext, R.layout.popupwindow_camera_need, null);
        Button btn_album = popupView.findViewById(R.id.btn_pop_album);
        Button btn_camera = popupView.findViewById(R.id.btn_pop_camera);
        Button btn_cancel = popupView.findViewById(R.id.btn_pop_cancel);

        btn_album.setOnClickListener(this);
        btn_camera.setOnClickListener(this);
        btn_cancel.setOnClickListener(this);

        //获取屏幕宽高
        int weight = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels / 3;

        popupWindow = new PopupWindow(popupView, weight, height);
        popupWindow.setAnimationStyle(R.style.Animation_Design_BottomSheetDialog);
        popupWindow.setFocusable(true);
        //点击外部popupWindow消失
        popupWindow.setOutsideTouchable(true);
        //popupWindow消失屏幕变为不透明
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.alpha = 1.0F;
                getWindow().setAttributes(lp);
            }
        });

        //popupWindow出现屏幕变为半透明
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 0.2F;
        getWindow().setAttributes(lp);
        popupWindow.showAtLocation(popupView, Gravity.BOTTOM, 0, 10);
    }

    // 调用摄像头
    private void takePhoto() {
        if (checkPermission()) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Ensure that there's a camera activity to handle the intent
            if (takePictureIntent.resolveActivity(mContext.getPackageManager()) != null) {
                // Create the File where the photo should go
                File photo = FileUtils.createImageFile();
                // Continue only if the File was successfully created
                if (photo != null) {
                    // api24后需要使用FileProvider
                    mCurrentPhotoPath = photo.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(mContext, "com.szwangel.fileprovider", photo));
                }
            }

            startActivityForResult(takePictureIntent, MainActivity.REQUEST_CAMERA_IMAGE);//跳转界面传回拍照所得数据
        }
    }

    // 图片裁剪
    private void cropPhoto() {
        // 调用系统中自带的图片剪裁
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.setDataAndType(FileProvider.getUriForFile(mContext, "com.szwangel.fileprovider", new File(mCurrentPhotoPath)), "image/*");
        // 下面这个crop=true是设置在开启的Intent中设置显示的VIEW可裁剪
        intent.putExtra("crop", "true");
        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // outputX outputY 是裁剪图片宽高，return-data限制最大160
        intent.putExtra("outputX", 160);
        intent.putExtra("outputY", 160);
        // MIUI无法通过return-data返回bitmap，需要特殊处理
//        intent.putExtra("return-data", true);

        // 裁剪后的图片Uri路径，uriTempFile为Uri类变量
        uriTempFile = Uri.parse("file://" + "/" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/" + "temp.jpg");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uriTempFile);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());

        startActivityForResult(intent, REQUEST_CROP_IMAGE);
    }

    // bitmap存储为jpg
    public static String saveBitmap(Bitmap bitmap) {
        File file = FileUtils.createImageFile();
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            // 设置PNG的话，透明区域不会变成黑色
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);

            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file.getAbsolutePath();
    }

    //判定当前是否需要隐藏
    protected boolean isShouldHideKeyBord(View v, MotionEvent event) {
        if (v instanceof EditText) {
            int[] l = {0, 0};
            v.getLocationInWindow(l);
            int left = l[0], top = l[1], bottom = top + v.getHeight(), right = left + v.getWidth();
            return !(event.getX() > left && event.getX() < right && event.getY() > top && event.getY() < bottom);
            //return !(ev.getY() > top && ev.getY() < bottom);
        }
        return false;
    }

    // 隐藏软键盘
    private void hideSoftInput(IBinder token) {
        if (token != null) {
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    // URI转路径
    private String uri2path(Uri contentURI) {
        String result;
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(contentURI, null, null, null, null);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (cursor == null) {
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    // 创建闹钟
    private void createAlarm(String message, int hour, int minutes, int selected) {
        ArrayList<Integer> weekDays = new ArrayList<>();
        weekDays.add(Calendar.MONDAY);
        weekDays.add(Calendar.TUESDAY);
        weekDays.add(Calendar.WEDNESDAY);
        weekDays.add(Calendar.THURSDAY);
        weekDays.add(Calendar.FRIDAY);
        weekDays.add(Calendar.SATURDAY);
        weekDays.add(Calendar.SUNDAY);

//        String packageName = getActivity().getApplication().getPackageName();
//        Uri ringtoneUri = Uri.parse("android.resource://" + packageName + "/" + resId);

        //action为AlarmClock.ACTION_SET_ALARM
        Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM)
                //闹钟的小时
                .putExtra(AlarmClock.EXTRA_HOUR, hour)
                //闹钟的分钟
                .putExtra(AlarmClock.EXTRA_MINUTES, minutes)
                //响铃时提示的信息
                .putExtra(AlarmClock.EXTRA_MESSAGE, message)
                //用于指定该闹铃触发时是否振动
                .putExtra(AlarmClock.EXTRA_VIBRATE, false)
                //一个 content: URI，用于指定闹铃使用的铃声，也可指定 VALUE_RINGTONE_SILENT 以不使用铃声。
                //如需使用默认铃声，则无需指定此 extra。
//                .putExtra(AlarmClock.EXTRA_RINGTONE, ringtoneUri)
                //一个 ArrayList，其中包括应重复触发该闹铃的每个周日。
                // 每一天都必须使用 Calendar 类中的某个整型值（如 MONDAY）进行声明。
                //对于一次性闹铃，无需指定此 extra
                .putExtra(AlarmClock.EXTRA_DAYS, weekDays)
                //如果为true，则调用startActivity()不会进入手机的闹钟设置界面
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true);

        // 选定某一天
        if (selected != -1) {
            ArrayList<Integer> oneDay = new ArrayList<>();
            oneDay.add(weekDays.get(selected));
            intent.putExtra(AlarmClock.EXTRA_DAYS, oneDay);
        }

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    // 确认必要权限
    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
            }, 666);

            return false;
        }
        return true;
    }

    // 弹出右上角菜单
    private void showBubbleMenu() {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout linearLayout = new LinearLayout(mContext);
        View popupView = inflater.inflate(R.layout.popupwindow_bubble_menu, linearLayout);
        final View search = popupView.findViewById(R.id.menuSearch);
        final View keyword = popupView.findViewById(R.id.menuKeyword);
        final View hereAndNow = popupView.findViewById(R.id.menuHereAndNow);

        search.setOnClickListener(this);
        keyword.setOnClickListener(this);
        hereAndNow.setOnClickListener(this);
        search.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // 避免圆角被覆盖
                    Drawable drawable = search.getBackground();
                    if (drawable instanceof GradientDrawable) {
                        ((GradientDrawable) drawable).setColor(getResources().getColor(R.color.black));
                        search.setAlpha(0.3F);
                    }
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    Drawable drawable = search.getBackground();
                    if (drawable instanceof GradientDrawable) {
                        ((GradientDrawable) drawable).setColor(getResources().getColor(R.color.bubbleBack));
                        search.setAlpha(1);
                    }
                }
                return false;
            }
        });
        keyword.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    keyword.setBackground(getDrawable(R.color.black));
                    keyword.setAlpha(0.3F);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    keyword.setBackground(getDrawable(R.color.bubbleBack));
                    keyword.setAlpha(1);
                }
                return false;
            }
        });
        hereAndNow.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // 避免圆角被覆盖
                    Drawable drawable = hereAndNow.getBackground();
                    if (drawable instanceof GradientDrawable) {
                        ((GradientDrawable) drawable).setColor(getResources().getColor(R.color.black));
                        hereAndNow.setAlpha(0.3F);
                    }
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    Drawable drawable = hereAndNow.getBackground();
                    if (drawable instanceof GradientDrawable) {
                        ((GradientDrawable) drawable).setColor(getResources().getColor(R.color.bubbleBack));
                        hereAndNow.setAlpha(1);
                    }
                }
                return false;
            }
        });

        popupWindow = new PopupWindow(popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true);
        popupWindow.setOutsideTouchable(true);

        popupWindow.showAsDropDown(plusMenu, 0, 0);
    }

}