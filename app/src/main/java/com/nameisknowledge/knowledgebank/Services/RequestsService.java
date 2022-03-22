package com.nameisknowledge.knowledgebank.Services;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.nameisknowledge.knowledgebank.Activities.DuoModeActivity;
import com.nameisknowledge.knowledgebank.BroadCastRecivers.NotificationBroadCast;
import com.nameisknowledge.knowledgebank.Constants.FirebaseConstants;
import com.nameisknowledge.knowledgebank.ModelClasses.RequestMD;
import com.nameisknowledge.knowledgebank.R;

import java.util.Objects;

public class RequestsService extends Service {
    private static FragmentActivity mActivity;
    private int id = 0;
    public RequestsService() {
    }

    public static void startActionFoo(Context context) {
        Intent intent = new Intent(context, RequestsService.class);
        mActivity = (FragmentActivity) context;
        context.startService(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        registerReceiver();
        requestsListener();
        responsesListener();
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    public void showNav(RequestMD requestMD, Context context) {
        String channelId = "ChaId";
        NotificationBroadCast notificationBroadCast = new NotificationBroadCast();
        Intent intent = new Intent(context, notificationBroadCast.getClass()).putExtra("senderID",requestMD.getUid());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId);
        builder.setSmallIcon(R.drawable.ic_launcher_background);

        builder.setContent(getContent(requestMD.getEmail(), context, pendingIntent));
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "myChannel", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }
        id++;
        manager.notify(id, builder.build());
    }

    public RemoteViews getContent(String name, Context context, PendingIntent intent) {
        RemoteViews remoteViews = new RemoteViews(context.getApplicationContext().getPackageName(), R.layout.notification_layout);
        remoteViews.setTextViewText(R.id.title, name);
        remoteViews.setOnClickPendingIntent(R.id.accept, intent);
        return remoteViews;
    }

    private void responsesListener(){
        FirebaseFirestore.getInstance().collection(FirebaseConstants.RESPONSES_COLLECTION)
                .document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                .collection(FirebaseConstants.CONTAINER_COLLECTION)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error == null) {
                    if (value != null) {
                        if (value.getDocumentChanges().size() == 1 && value.getDocumentChanges().get(0).getType() != DocumentChange.Type.REMOVED) {
                            RequestsService.this.startActivity(new Intent(RequestsService.this, DuoModeActivity.class)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                            | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    .putExtra("roomID", value.getDocumentChanges().get(0).getDocument().getString("roomID"))
                                    .putExtra("senderID",value.getDocumentChanges().get(0).getDocument().getString("userID")));
                        }
                    }
                } else {
                    Toast.makeText(RequestsService.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /*
    دالة عبارة عن Listener على كوليكشن الRequests للحصول على اي Request جديدة يتم ارسالها الى المستخدم
     */
    private void requestsListener(){
        FirebaseFirestore.getInstance().collection(FirebaseConstants.REQUESTS_COLLECTION)
                .document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                .collection(FirebaseConstants.CONTAINER_COLLECTION)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error == null) {
                    if (value != null) {
                        // تم وضع هذا الشرط للحصول فقط على الطلبات الجديدة لان السنابشوت الخاص بالفايربيز يقوم بجلب جميع بيانات
                        // الكوليكشن في اول مرة يتم تنفيذ الكود فيها
                        // "لكن يوجد مشكلة"
                        if (value.getDocumentChanges().size() == 1 && value.getDocumentChanges().get(0).getType() != DocumentChange.Type.REMOVED) {
                            // هنا قمنا باستخدام دالة اظهار الاشعار وارسلنا لها اوبجكت الRequest الي وصل + context
                            showNav(value.getDocumentChanges().get(0).getDocument().toObject(RequestMD.class), mActivity.getBaseContext());
                        }
                    }
                } else {
                    Toast.makeText(RequestsService.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    /*
    دالة تقوم بتسجيل البرودكاست في النظام
    وظيفة هذا البرودكاست هي ارسال اشعار للمستخدم عند وصول طلب لعب (Request)
     */
    public void registerReceiver() {
        NotificationBroadCast receiver = new NotificationBroadCast();
        IntentFilter filter = new IntentFilter();
        registerReceiver(receiver, filter);
    }

}