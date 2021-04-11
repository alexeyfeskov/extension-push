package com.defold.push;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import androidx.core.app.NotificationCompat;

import java.lang.Exception;
import java.lang.String;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class RichNotificationUtils {

    public static final String TAG = "push";

    public static JSONObject getLayoutJson(Bundle bundle) {
        String jsonString = bundle.getString("layout");
        if (jsonString == null || jsonString.length() == 0) {
            return null;
        }

        try {
            JSONObject layoutJson = new JSONObject(jsonString);
            return layoutJson;
        } catch (JSONException e) {
            Log.e(TAG, String.format("Failed to create json object from '%s'", jsonString), e);
            return null;
        }
    }

    public static void setCustomLayout(Context context, NotificationCompat.Builder builder, JSONObject layoutJson, String title, String text) {
        String layoutName = layoutJson.optString("name");
        if (layoutName == null) {
            String errorMsg = String.format("Custom layout name is missing: `%s`", layoutJson.toString());
            Log.w(TAG, errorMsg);
            return;
        }

        int layoutId = findIdentifierInContextByName(context, layoutName, "layout");
        if (layoutId == 0) {
            String errorMsg = String.format("Layout `<bundle_resources>/android/res/layout/%s.xml` not found.", layoutName);
            Log.w(TAG, errorMsg);
            return;
        }

        RemoteViews customView = new RemoteViews(context.getPackageName(), layoutId);
        builder.setCustomContentView(customView);

        // try to set default text (silent if fails)
        trySetCustomViewText(context, customView, "text", text);
        trySetCustomViewText(context, customView, "title", title);

        // set custom texts
        JSONObject textsJson = layoutJson.optJSONObject("texts");
        if (textsJson != null) {
            JSONArray textIds = textsJson.names();
            for (int i = 0; i < textIds.length(); ++i) {
                String textId = textIds.optString(i);
                if (textId == null) {
                    continue;
                }

                String textValue = textsJson.optString(textId);
                if (textValue == null) {
                    continue;
                }

                if (!trySetCustomViewText(context, customView, textId, textValue)) {
                    String errorMsg = String.format("Fail to set text `@+id/%s` in layout `res/layout/%s.xml` to value `%s`",
                                                    textId, layoutName, textValue);
                    Log.w(TAG, errorMsg);
                    continue;
                }
            }
        }

        // set custom images
        JSONObject imagesJson = layoutJson.optJSONObject("images");
        if (imagesJson != null) {
            JSONArray imageIds = imagesJson.names();
            for (int i = 0; i < imageIds.length(); ++i) {
                String imageId = imageIds.optString(i);
                if (imageId == null) {
                    continue;
                }

                String imageResourceName = imagesJson.optString(imageId);
                if (imageResourceName == null) {
                    continue;
                }

                if (!trySetCustomViewImage(context, customView, imageId, imageResourceName)) {
                    String errorMsg = String.format("Fail to set image `@+id/%s` in layout `res/layout/%s.xml` to value `@drawable/%s`",
                                                    imageId, layoutName, imageResourceName);
                    Log.w(TAG, errorMsg);
                    continue;
                }
            }
        }
    }

    private static boolean trySetCustomViewText(Context context, RemoteViews customView, String textId, String value) {
        int id = findIdentifierInContextByName(context, textId, "id");
        if (id == 0) {
            return false;
        }
        
        try {
            customView.setTextViewText(id, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean trySetCustomViewImage(Context context, RemoteViews customView, String imageId, int imageResourceId) {
        int id = findIdentifierInContextByName(context, imageId, "id");
        if (id == 0) {
            return false;
        }
        
        try {
            customView.setImageViewResource(id, imageResourceId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean trySetCustomViewImage(Context context, RemoteViews customView, String imageId, String imageResourceName) {
        int imageResourceId = findIdentifierInContextByName(context, imageResourceName, "drawable");
        if (imageResourceId == 0) {
            String errorMsg = String.format("Image `<bundle_resources>/android/res/drawable/%s` not found.", imageResourceName);
            Log.w(TAG, errorMsg);
            return false;
        } else {
            return trySetCustomViewImage(context, customView, imageId, imageResourceId);
        }
    }

    private static int findIdentifierInContextByName(Context context, String name, String defType) {
        if (name == null) {
            return 0;
        }

        try {
            Resources res = context.getResources();
            if (res != null) {
                int id = res.getIdentifier(name, defType, context.getPackageName());
                return id;
            }
            return 0;
        } catch (Resources.NotFoundException e) {
            return 0;
        }
    }
}
