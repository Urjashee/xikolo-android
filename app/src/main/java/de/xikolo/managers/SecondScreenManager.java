package de.xikolo.managers;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import de.greenrobot.event.EventBus;
import de.xikolo.GlobalApplication;
import de.xikolo.R;
import de.xikolo.controller.SecondScreenActivity;
import de.xikolo.data.entities.Course;
import de.xikolo.data.entities.Item;
import de.xikolo.data.entities.Module;
import de.xikolo.data.entities.VideoItemDetail;
import de.xikolo.data.entities.WebSocketMessage;
import de.xikolo.model.CourseModel;
import de.xikolo.model.ItemModel;
import de.xikolo.model.ModuleModel;
import de.xikolo.model.Result;
import de.xikolo.model.events.Event;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class SecondScreenManager {

    public static final String TAG = SecondScreenManager.class.getSimpleName();

    public static final int NOTIFICATION_ID = "second_screen_notification".hashCode();

    private Course course;

    private Module module;

    private Item<VideoItemDetail> item;

    private CourseModel courseModel;

    private ModuleModel moduleModel;

    private ItemModel itemModel;

    private boolean isRequesting;

    public SecondScreenManager() {
        courseModel = new CourseModel(GlobalApplication.getInstance().getJobManager());
        moduleModel = new ModuleModel(GlobalApplication.getInstance().getJobManager());
        itemModel = new ItemModel(GlobalApplication.getInstance().getJobManager());

        EventBus.getDefault().register(this);

        isRequesting = false;
    }

    public void onEventBackgroundThread(WebSocketManager.WebSocketMessageEvent event) {
        final WebSocketMessage message = event.getWebSocketMessage();

        if (!message.platform().equals("web") || !message.action().startsWith("video_")) {
            // ignore other WebSocket events
            return;
        }

        final Result<Item> itemResult = new Result<Item>() {
            @Override
            protected void onSuccess(Item item, DataSource dataSource) {
                if (item != null && !item.equals(SecondScreenManager.this.item)) {
                    SecondScreenManager.this.item = (Item<VideoItemDetail>) item;

                    Context context = GlobalApplication.getInstance();

                    // show notification
                    NotificationCompat.Builder builder =
                            new NotificationCompat.Builder(context)
                                    .setSmallIcon(R.drawable.ic_notification_second_screen)
                                    .setContentTitle(context.getString(R.string.notification_start_second_screen))
                                    .setContentText(item.title)
                                    .setAutoCancel(true)
                                    .setColor(ContextCompat.getColor(context, R.color.apptheme_main))
                                    .setPriority(Notification.PRIORITY_HIGH)
                                    .setVibrate(new long[0]);

                    Intent intent = new Intent(context, SecondScreenActivity.class);
                    PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                    builder.setContentIntent(contentIntent);

                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.notify(NOTIFICATION_ID, builder.build());

                    // post sticky new video event
                    EventBus.getDefault().postSticky(new SecondScreenNewVideoEvent(course, module, SecondScreenManager.this.item, message));

                    isRequesting = false;
                }
            }

            @Override
            protected void onError(ErrorCode errorCode) {
                isRequesting = false;
            }
        };

        final Result<Module> moduleResult = new Result<Module>() {
            @Override
            protected void onSuccess(Module module, DataSource dataSource) {
                if (module != null && !module.equals(SecondScreenManager.this.module) && module.items != null && module.items.size() > 0) {
                    SecondScreenManager.this.module = module;
                    itemModel.getItemDetail(itemResult, course.id, module.id, message.payload().get("item_id"), Item.TYPE_VIDEO);
                }
            }

            @Override
            protected void onError(ErrorCode errorCode) {
                isRequesting = false;
            }
        };

        Result<Course> courseResult = new Result<Course>() {
            @Override
            protected void onSuccess(Course course, DataSource dataSource) {
                if (course != null && !course.equals(SecondScreenManager.this.course)) {
                    SecondScreenManager.this.course = course;
                    moduleModel.getModuleWithItems(moduleResult, course.id, message.payload().get("section_id"));
                }
            }

            @Override
            protected void onError(ErrorCode errorCode) {
                isRequesting = false;
            }
        };

        synchronized (GlobalApplication.class) {
            if (!isRequesting && (item == null || !item.id.equals(message.payload().get("item_id")))) {
                // new video detected
                isRequesting = true;
                course = null;
                module = null;
                courseModel.getCourse(courseResult, message.payload().get("course_id"));
            } else if (item != null && item.id.equals(message.payload().get("item_id"))) {
                // post video updated event
                EventBus.getDefault().post(new SecondScreenUpdateVideoEvent(course, module, item, message));

                if (message.action().equals("video_close")) {
                    NotificationManager notificationManager = (NotificationManager) GlobalApplication.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancel(NOTIFICATION_ID);

                    EventBus.getDefault().removeStickyEvent(SecondScreenNewVideoEvent.class);

                    course = null;
                    module = null;
                    item = null;
                }
            }
        }
    }

    public static class SecondScreenUpdateVideoEvent extends Event {

        private Course course;

        private Module module;

        private Item<VideoItemDetail> item;

        private WebSocketMessage webSocketMessage;

        public SecondScreenUpdateVideoEvent(Course course, Module module, Item<VideoItemDetail> item, WebSocketMessage webSocketMessage) {
            super();
            this.course = course;
            this.module = module;
            this.item = item;
            this.webSocketMessage = webSocketMessage;
        }

        public Course getCourse() {
            return course;
        }

        public Module getModule() {
            return module;
        }

        public Item<VideoItemDetail> getItem() {
            return item;
        }

        public WebSocketMessage getWebSocketMessage() {
            return webSocketMessage;
        }

    }

    public static class SecondScreenNewVideoEvent extends SecondScreenUpdateVideoEvent {

        public SecondScreenNewVideoEvent(Course course, Module module, Item<VideoItemDetail> item, WebSocketMessage webSocketMessage) {
            super(course, module, item, webSocketMessage);
        }

    }

}
